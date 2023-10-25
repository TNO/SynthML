
package com.github.tno.pokayoke.transform.app;

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
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.Application;
import org.eclipse.escet.common.app.framework.DummyApplication;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.output.OutputMode;
import org.eclipse.escet.common.app.framework.output.OutputModeOption;

import com.github.tno.pokayoke.transform.cif2petrify.FileHelper;
import com.google.common.base.Preconditions;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    public void performFullSynthesis(String inputPath, String outputdir) {
        // Load CIF specification.
        Specification cifSpec = FileHelper.loadCifSpec(Paths.get(inputPath));

        // Generate CIF state space.
        Specification cifStateSpace = convertToStateSpace(cifSpec);

        // Translate CIF state space to Petrify input
        cifStateSpace

        // Petrify the state space

        // Translate Petrify output to PNML

        // Translate Petri Net to UML Activity
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
