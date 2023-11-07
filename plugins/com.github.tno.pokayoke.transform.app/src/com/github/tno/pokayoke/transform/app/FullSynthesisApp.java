
package com.github.tno.pokayoke.transform.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import com.github.tno.pokayoke.transform.petrify2uml.Petrify2PNMLTranslator;
import com.google.common.base.Preconditions;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    private FullSynthesisApp() {
    }

    public static void performFullSynthesis(Path inputPath, Path outputFolderPath) throws IOException {
        Files.createDirectories(outputFolderPath);

        // Perform Synthesis.
        // TODO when the synthesis specification is formalized.

        // Load CIF specification.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Specification cifSpec = FileHelper.loadCifSpec(inputPath);

        // Generate CIF state space.
        Specification cifStateSpace = convertToStateSpace(cifSpec);

        // Output the generated CIF state space.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifStateSpace, cifStateSpacePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Translate the CIF state space to Petrify input and output the Petrify input.
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".g");
        Cif2Petrify.transformFile(cifStateSpacePath.toString(), petrifyInputPath.toString());

        // Petrify the state space and output the generated Petri Net.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        convertToPetriNet(petrifyInputPath, petrifyOutputPath, petrifyLogPath, 20);

        // Translate the Petrify output to PNML and output the PNML.
        Path pnmlOutputPath = outputFolderPath.resolve(filePrefix + ".pnml");
        Petrify2PNMLTranslator.transformFile(petrifyOutputPath.toString(), pnmlOutputPath.toString());

        // Translate Petri Net to UML Activity and output the activity.
        // TODO when the code for this functionality is merged into the main branch.
    }

    /**
     * Compute the state space of the automata in a specification.
     *
     * @param spec {@link Specification} containing automata to convert.
     * @return {@link Specification} containing state space automaton.
     */
    public static Specification convertToStateSpace(Specification spec) {
        Application<?> app = new DummyApplication(new AppStreams());
        Options.set(OutputModeOption.class, OutputMode.ERROR);
        Options.set(AutomatonNameOption.class, null);

        Specification statespace;
        try {
            ExplorerBuilder builder = new ExplorerBuilder(spec);
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
            statespace = statespaceBuilder.createAutomaton(explorer, spec);
        } finally {
            AppEnv.unregisterApplication();
        }
        return statespace;
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param petrifyLogPath The path of the etrify log file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    public static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, Path petrifyLogPath,
            int timeoutInSeconds)
    {
        File stdOutputFile = new File(petrifyOutputPath.toString());

        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();

        command.add(ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyInputPath.toString()));
        command.add("-o");
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyOutputPath.toString()));

        // When this option is used, Petrify tries to produce the best possible result.
        command.add("-opt");

        // Produce a choice free Petri net. By being choice free, the Petri Net becomes easier to translate to an
        // activity.
        command.add("-fc");

        // Produce Petri Net with intermediate places. If this option is not used, implied places are described as
        // transition-transition arcs.
        command.add("-ip");

        // Generate a log file.
        command.add("-log");
        command.add(petrifyLogPath.toString());

        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);
        petrifyProcessBuilder.redirectOutput(stdOutputFile);

        // Start the process for Petrify.
        Process petrifyProcess;

        try {
            petrifyProcess = petrifyProcessBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start the Petrify process.", e);
        }

        // Wait for the process to finish within the given timeout period.
        boolean petrifyProcessCompleted = false;

        try {
            petrifyProcessCompleted = petrifyProcess.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            try {
                petrifyProcess.destroyForcibly();
                Files.delete(stdOutputFile.toPath());
            } catch (IOException e1) {
                throw new RuntimeException("Failed to kill the Petrify process.", e);
            }
        }

        // Check whether the process timed out.
        if (!petrifyProcessCompleted) {
            petrifyProcess.destroyForcibly();
            try {
                Files.delete(stdOutputFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete: " + stdOutputFile.toString(), e);
            }
        }
    }
}
