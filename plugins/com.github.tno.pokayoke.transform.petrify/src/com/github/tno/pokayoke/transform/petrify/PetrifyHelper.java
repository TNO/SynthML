
package com.github.tno.pokayoke.transform.petrify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Verify;

/** Helper for petrification. */
public class PetrifyHelper {
    private PetrifyHelper() {
    }

    /**
     * Converts a given CIF state space to a Petri Net using Petrify. This conversion first tries to synthesize a free
     * choice Petri Net. If this fails, then an ordinary, non free choice Petri Net is synthesized instead. This
     * conversion assumes that Petrify is always able to synthesize an ordinary Petri Net.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param executablePath The path of the executable.
     * @param petrifyLogPath The path of the Petrify log file.
     * @param petrifyErrorPath The Petrify standard error (stderr) destination file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    public static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, String executablePath,
            Path petrifyLogPath, Path petrifyErrorPath, int timeoutInSeconds)
    {
        // Try to synthesize a free choice Petri Net.
        Consumer<Boolean> runPetrify = freeChoice -> convertToPetriNet(petrifyInputPath, petrifyOutputPath,
                executablePath, petrifyLogPath, petrifyErrorPath, freeChoice, timeoutInSeconds);

        runPetrify.accept(true);

        // Check whether Petrify reported any errors. If not, then we are done.
        File errorFile = petrifyErrorPath.toFile();
        Verify.verify(errorFile.exists(), "Expected a stderr destination file to have been created.");

        if (errorFile.length() != 0) {
            // Petrify reported errors. First rename all earlier Petrify output.
            try {
                Function<Path, Path> targetPath = path -> {
                    String fileName = path.getFileName().toString();
                    String filePrefix = FilenameUtils.removeExtension(fileName);
                    String fileExtension = FilenameUtils.getExtension(fileName);
                    return path.getParent().resolve(filePrefix + ".freechoice." + fileExtension);
                };

                Files.move(petrifyOutputPath, targetPath.apply(petrifyOutputPath));
                Files.move(petrifyLogPath, targetPath.apply(petrifyLogPath));
                Files.move(petrifyErrorPath, targetPath.apply(petrifyErrorPath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to rename Petrify output files.", e);
            }

            // Then run Petrify to synthesize an ordinary, non free choice Petri Net.
            runPetrify.accept(false);

            // Check again whether any errors were reported. If so, then Petri Net synthesis failed.
            errorFile = petrifyErrorPath.toFile();
            Verify.verify(errorFile.exists(), "Expected a stderr destination file to have been created.");

            if (errorFile.length() != 0) {
                try {
                    throw new RuntimeException(
                            "Petrify failed to synthesize a Petri Net: " + Files.readString(petrifyErrorPath));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read the Petrify error file.", e);
                }
            }
        }
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param executablePath The path of the executable.
     * @param petrifyLogPath The path of the Petrify log file.
     * @param petrifyErrorPath The Petrify standard error (stderr) destination file.
     * @param produceFreeChoiceResult Whether Petrify should synthesize a free choice Petri Net ({@code true}) or an
     *     ordinary Petri Net ({@code false}).
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    public static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, String executablePath,
            Path petrifyLogPath, Path petrifyErrorPath, boolean produceFreeChoiceResult, int timeoutInSeconds)
    {
        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();
        Path parentPath = petrifyInputPath.getParent();
        command.add(executablePath);
        command.add(parentPath.relativize(petrifyInputPath).toString());
        command.add("-o");
        command.add(parentPath.relativize(petrifyOutputPath).toString());

        // When this option is used, Petrify tries to produce the best possible result.
        command.add("-opt");

        if (produceFreeChoiceResult) {
            // Produce a free choice Petri Net, which may lead to more intuitive activity synthesis results.
            command.add("-fc");
        }

        // Produce Petri Net with intermediate places. If this option is not used, implied places are described as
        // transition-transition arcs.
        command.add("-ip");

        // Generate a log file.
        command.add("-log");
        command.add(parentPath.relativize(petrifyLogPath).toString());

        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);
        petrifyProcessBuilder.redirectError(petrifyErrorPath.toFile());

        petrifyProcessBuilder.directory(parentPath.toAbsolutePath().toFile());
        // Start the process for Petrify.
        Process petrifyProcess;

        try {
            petrifyProcess = petrifyProcessBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start the Petrify process.", e);
        }

        // Wait for the process to finish within the given timeout period.
        boolean petrifyProcessCompleted;

        try {
            petrifyProcessCompleted = petrifyProcess.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            petrifyProcess.destroyForcibly();
            throw new RuntimeException("Interrupted while waiting for Petrify process to finish.", e);
        }

        // Check whether the process timed out.
        if (!petrifyProcessCompleted) {
            petrifyProcess.destroyForcibly();
            throw new RuntimeException("Petrify process timed out.");
        }

        Verify.verify(petrifyProcess.exitValue() == 0,
                "Petrify process exited with non-zero exit code (" + petrifyProcess.exitValue() + ").");
    }

    public static boolean isDuplicateTransition(String elementName, Set<String> declaredNames) {
        // Since CIF does not accept '/' in identifiers, the generated state space cannot contain '/'. It is safe to use
        // '/' to identify duplicate transitions.
        for (String declaredName: declaredNames) {
            if (elementName.startsWith(declaredName) && elementName.contains("/")) {
                return true;
            }
        }
        return false;
    }
}
