
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

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
import org.apache.commons.io.FilenameUtils;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    public void performFullSynthesis(String inputPath, String outputdir) throws IOException {
        // Load CIF specification.
        Path inputFilePath = Paths.get(inputPath);
        String fileName = FilenameUtils.removeExtension(inputFilePath.getFileName().toString());
        Specification cifSpec = FileHelper.loadCifSpec(inputFilePath);

        // Generate CIF state space.
        Specification cifStateSpace = convertToStateSpace(cifSpec);

        // Output the generated CIF state space.
        Path cifStateSpacePath = Paths.get(outputdir, fileName + ".ctrlsys_statespace.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifStateSpace, cifStateSpacePath.toString(), outputdir);
        } finally {
            AppEnv.unregisterApplication();
        }

        // Translate CIF state space to Petrify input.
        String body = Cif2Petrify.transform(cifStateSpace);
        Path petrifyInputPath = Paths.get(outputdir,
                fileName + ".g");
        FileHelper.writeToFile(body, petrifyInputPath);

        // Petrify the state space.
        Runtime rt = Runtime.getRuntime();
        Process ps = rt.exec("");


        // Translate Petrify output to PNML.

        // Translate Petri Net to UML Activity.
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
