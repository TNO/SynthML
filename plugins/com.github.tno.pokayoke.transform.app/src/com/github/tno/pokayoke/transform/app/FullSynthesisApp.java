
package com.github.tno.pokayoke.transform.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Path cifStateSpacePath = Paths.get(outputFolderPath.toString(), filePrefix + ".statespace.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifStateSpace, cifStateSpacePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Translate the CIF state space to Petrify input and output the Petrify input.
        Path petrifyInputPath = Paths.get(outputFolderPath.toString(), filePrefix + ".g");
        Cif2Petrify.transformFile(cifStateSpacePath.toString(), petrifyInputPath.toString());

        // Petrify the state space and output the generated Petri Net.
        Path petrifyOutputPath = Paths.get(outputFolderPath.toString(), filePrefix + ".out");
        convertToPetriNet(petrifyInputPath, petrifyOutputPath, 20);

        // Translate the Petrify output to PNML and output the PNML.
        Path pnmlOutputPath = Paths.get(outputFolderPath.toString(), filePrefix + ".pnml");
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
     * @param petrifyInputPath The path of the petrify input file.
     * @param petrifyOutputPath The path of the petrify output file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    public static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, int timeoutInSeconds) {
        File stdOutFile = new File(petrifyOutputPath.toString());

        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();

        command.add(ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyInputPath.toString()));
        command.add("-o");
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyOutputPath.toString()));

        // Add Petrify options.
        command.add("-opt");

        // Produce a pure Petri net (no self-loop places).
        command.add("-p");

        // Duplicate transitions for a better structured Petri Net.
        command.add("-er");

        // Produce a Free-Choice Petri net. If this option is not used, places with multiple incoming and outgoing
        // edges are produced.
        command.add("-fc");

        // Produce Petri Net with places. If this option is not used, implied places are described as
        // transition-transition arcs.
        command.add("-ip");

        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);
        petrifyProcessBuilder.redirectErrorStream(true);
        petrifyProcessBuilder.redirectOutput(stdOutFile);

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
                Files.delete(stdOutFile.toPath());
            } catch (IOException e1) {
                throw new RuntimeException("Failed to kill the Petrify process.", e);
            }
        }

        // Check whether the process timed out.
        if (!petrifyProcessCompleted) {
            petrifyProcess.destroyForcibly();
            try {
                Files.delete(stdOutFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete: " + stdOutFile.toString(), e);
            }
        }
    }
}
