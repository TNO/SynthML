
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Verify;

/** Helper for petrification. */
public class PetrifyHelper {
    private PetrifyHelper() {
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
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
        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();
        Path parentPath = petrifyInputPath.getParent();
        command.add(executablePath);
        command.add(parentPath.relativize(petrifyInputPath).toString());
        command.add("-o");
        command.add(parentPath.relativize(petrifyOutputPath).toString());
        command.add("-err");
        command.add(parentPath.relativize(petrifyErrorPath).toString());

        // When this option is used, Petrify tries to produce the best possible result.
        command.add("-opt");

        // Produce a free choice Petri net. By being free choice, the Petri Net becomes easier to translate to an
        // activity.
        command.add("-fc");

        // Produce Petri Net with intermediate places. If this option is not used, implied places are described as
        // transition-transition arcs.
        command.add("-ip");

        // Generate a log file.
        command.add("-log");
        command.add(parentPath.relativize(petrifyLogPath).toString());

        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);

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

        // Check whether Petrify reported any errors during Petri Net synthesis.
        File errorFile = petrifyErrorPath.toFile();
        Verify.verify(errorFile.exists(), "Expected a stderr destination file to have been created.");

        if (errorFile.length() != 0) {
            throw new RuntimeException("Petrify reported errors during Petri Net synthesis.");
        }
    }
}
