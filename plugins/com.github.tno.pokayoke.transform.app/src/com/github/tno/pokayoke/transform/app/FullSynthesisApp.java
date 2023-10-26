
package com.github.tno.pokayoke.transform.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.explorer.CifAutomatonBuilder;
import org.eclipse.escet.cif.explorer.ExplorerStateFactory;
import org.eclipse.escet.cif.explorer.app.AutomatonNameOption;
import org.eclipse.escet.cif.explorer.runtime.BaseState;
import org.eclipse.escet.cif.explorer.runtime.Explorer;
import org.eclipse.escet.cif.explorer.runtime.ExplorerBuilder;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.Application;
import org.eclipse.escet.common.app.framework.DummyApplication;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.output.OutputMode;
import org.eclipse.escet.common.app.framework.output.OutputModeOption;

import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.cif2petrify.FileHelper;
import com.google.common.base.Preconditions;

/** Application that performs full synthesis. */
public class FullSynthesisApp {


    public static void performFullSynthesis(String inputPath, String outputdir) throws IOException {
        // Load CIF specification.
        Path inputFilePath = Paths.get(inputPath);
        String fileName = FilenameUtils.removeExtension(inputFilePath.getFileName().toString());
        Specification cifSpec = FileHelper.loadCifSpec(inputFilePath);

        // Generate CIF state space.
        Specification cifStateSpace = convertToStateSpace(cifSpec);

        // Output the generated CIF state space.
        Path cifStateSpacePath = Paths.get(outputdir, fileName + ".statespace.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifStateSpace, cifStateSpacePath.toString(), outputdir);
        } finally {
            AppEnv.unregisterApplication();
        }

        // Translate CIF state space to Petrify input.
        String body = Cif2Petrify.transform(cifStateSpace);
        Path petrifyInputPath = Paths.get(outputdir, fileName + ".g");
        FileHelper.writeToFile(body, petrifyInputPath);

        // Petrify the state space.
        Path petrifyOutputPath = Paths.get(outputdir, fileName + ".out");
        List<String> warinings = new ArrayList<>();
        executePetrify(petrifyInputPath, petrifyOutputPath, warinings, 20);

        // Translate Petrify output to PNML.

        // Translate Petri Net to UML Activity.
    }

    public static boolean executePetrify(Path petrifyInputPath, Path petrifyOutputPath, List<String> warnings,
            int timeoutInSeconds)
    {
        File stdOutFile = new File(petrifyOutputPath.toString());
        ArrayList<String> command = new ArrayList<>();
        String[] petrifyOptions = {"-opt", "-p", "-er", "-efc", "-ip"};
        command.add(ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyInputPath.toAbsolutePath().toString()));
        command.add("-o");
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(stdOutFile.getAbsolutePath().toString()));
        command.addAll(Arrays.asList(petrifyOptions));
        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);
        petrifyProcessBuilder.redirectErrorStream(true);
        petrifyProcessBuilder.redirectOutput(stdOutFile);

        // Start the process for rendering the SVG.
        Process petrifyProcess;
        try {
            petrifyProcess = petrifyProcessBuilder.start();
        } catch (IOException e) {
            warnings.add("I/O error during execution of petrify process: " + e.getMessage());
            return false;
        }

        // Wait for the process to finish within the given timeout period.
        boolean petrifyProcessCompleted;
        try {
            petrifyProcessCompleted = petrifyProcess.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            petrifyProcess.destroyForcibly();

            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException ex) {
                warnings.add("I/O error while deleting temporary file from " + stdOutFile + ": " + e.getMessage());
            }

            return false;
        }

        // Check whether the process timed out.
        if (!petrifyProcessCompleted) {
            petrifyProcess.destroyForcibly();

            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException e) {
                warnings.add("I/O error while deleting temporary file from " + stdOutFile + ": " + e.getMessage());
            }

            return false;
        }

        return true;
    }

    /**
     * Compute the statespace of the automata in a specification.
     *
     * @param cif {@link Specification} containing automata to convert.
     * @return {@link Specification} containing statespace automaton.
     */

    public static Specification convertToStateSpace(Specification cif) {
        Application<?> app = new DummyApplication(new AppStreams());
        Options.set(OutputModeOption.class, OutputMode.ERROR);
        Options.set(AutomatonNameOption.class, null);

        Specification statespace;
        try {
            ExplorerBuilder builder = new ExplorerBuilder(cif);
            builder.collectData();
            ExplorerStateFactory stateFactory = new ExplorerStateFactory();
            Explorer explorer = builder.buildExplorer(stateFactory);
            List<BaseState> initials = explorer.getInitialStates(app);
            Preconditions.checkArgument(initials != null && !initials.isEmpty());
            Queue<BaseState> queue = new ArrayDeque<>();
            queue.addAll(initials);
            while (!queue.isEmpty()) {
                BaseState state = queue.poll();
                queue.addAll(state.getNewSuccessorStates());
            }
            explorer.renumberStates();
            explorer.minimizeEdges();
            CifAutomatonBuilder statespaceBuilder = new CifAutomatonBuilder();
            statespace = statespaceBuilder.createAutomaton(explorer, cif);
        } finally {
            AppEnv.unregisterApplication();
        }
        return statespace;
    }
}
