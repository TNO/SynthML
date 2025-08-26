
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.cif2cif.ElimIfUpdates;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.eventbased.apps.DfaMinimizationApplication;
import org.eclipse.escet.cif.eventbased.apps.ProjectionApplication;
import org.eclipse.escet.cif.explorer.app.ExplorerApplication;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.io.AppStream;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.io.MemAppStream;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.java.PathPair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Model;

import com.github.tno.pokayoke.transform.activitysynthesis.AbstractActivityDependencyOrderer;
import com.github.tno.pokayoke.transform.activitysynthesis.CIFDataSynthesisHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.CheckNonDeterministicChoices;
import com.github.tno.pokayoke.transform.activitysynthesis.CifSourceSinkLocationTransformer;
import com.github.tno.pokayoke.transform.activitysynthesis.GuardComputation;
import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter;
import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter.NonAtomicPattern;
import com.github.tno.pokayoke.transform.app.StateAwareWeakLanguageEquivalenceHelper.ModelPreparationResult;
import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.cif2petrify.CifFileHelper;
import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.flatten.CompositeDataTypeFlattener;
import com.github.tno.pokayoke.transform.petrify.PetrifyHelper;
import com.github.tno.pokayoke.transform.petrify2uml.PNML2UMLTranslator;
import com.github.tno.pokayoke.transform.petrify2uml.PNMLUMLFileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.PetrifyOutput2PNMLTranslator;
import com.github.tno.pokayoke.transform.petrify2uml.PostProcessActivity;
import com.github.tno.pokayoke.transform.petrify2uml.PostProcessPNML;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.pokayoke.transform.track.UmlToCifTranslationPurpose;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Transition;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    private FullSynthesisApp() {
    }

    public static void performFullSynthesis(Path inputPath, Path outputFolderPath, List<String> warnings)
            throws IOException, CoreException
    {
        Files.createDirectories(outputFolderPath);
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());

        // Load UML specification.
        Model umlSpec = FileHelper.loadModel(inputPath.toString());
        FileHelper.normalizeIds(umlSpec);

        // Flatten composite data types.
        CompositeDataTypeFlattener.flattenCompositeDataTypes(umlSpec);

        // Synthesize all abstract activities in the loaded UML specification in the proper order.
        AbstractActivityDependencyOrderer orderer = new AbstractActivityDependencyOrderer(
                new CifContext(umlSpec).getAllActivities());
        List<Activity> activities = orderer.computeOrder();

        if (activities == null) {
            throw new RuntimeException(String.format(
                    "Expected to find no cyclic dependencies in the activities to synthesize, but found '%s'.",
                    orderer.getCycleDescription()));
        }

        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            Preconditions.checkArgument(!Strings.isNullOrEmpty(activity.getName()), "Expected activities to be named.");
            Path localOutputPath = outputFolderPath.resolve(String.format("%d-%s", i + 1, activity.getName()));
            Files.createDirectories(localOutputPath);
            performFullSynthesis(activity, filePrefix, localOutputPath, warnings);
        }
    }

    public static void performFullSynthesis(Activity activity, String filePrefix, Path outputFolderPath,
            List<String> warnings) throws IOException, CoreException
    {
        // Instantiate the tracker that indicates how results from intermediate steps of the activity synthesis chain
        // relate to the input UML.
        SynthesisChainTracking tracker = new SynthesisChainTracking();

        // Translate the UML specification to a CIF specification.
        UmlToCifTranslator umlToCifTranslator = new UmlToCifTranslator(activity, UmlToCifTranslationPurpose.SYNTHESIS,
                tracker);
        Specification cifSpec = umlToCifTranslator.translate();
        Path cifSpecPath = outputFolderPath.resolve(filePrefix + ".01.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpec, makePathPair(cifSpecPath), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Post-process the CIF specification to eliminate all if-updates.
        ElimIfUpdates elimIfUpdates = new ElimIfUpdates();
        elimIfUpdates.transform(cifSpec);
        Path cifPostProcessedSpecPath = outputFolderPath.resolve(filePrefix + ".02.postprocessed.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpec, makePathPair(cifPostProcessedSpecPath), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Get CIF/BDD specification.
        CifDataSynthesisSettings settings = CIFDataSynthesisHelper.getSynthesisSettings();
        CifBddSpec cifBddSpec = CIFDataSynthesisHelper.getCifBddSpec(cifSpec,
                cifPostProcessedSpecPath.toAbsolutePath().toString(), settings);

        // Perform synthesis.
        CifDataSynthesisResult cifSynthesisResult = CIFDataSynthesisHelper.synthesize(cifBddSpec, settings);

        // Convert synthesis result back to CIF.
        Path cifSynthesisPath = outputFolderPath.resolve(filePrefix + ".03.ctrlsys.cif");
        CIFDataSynthesisHelper.convertSynthesisResultToCif(cifSpec, cifSynthesisResult, cifSynthesisPath,
                outputFolderPath.toString());

        // Perform state space generation.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".04.ctrlsys.statespace.cif");
        String[] stateSpaceGenerationArgs = new String[] {cifSynthesisPath.toString(), "--name=synthesis_state_space",
                "--output=" + cifStateSpacePath.toString()};
        AppStream explorerAppStream = new MemAppStream();
        AppStreams explorerAppStreams = new AppStreams(InputStream.nullInputStream(), explorerAppStream,
                explorerAppStream, explorerAppStream);
        ExplorerApplication explorerApp = new ExplorerApplication(explorerAppStreams);
        int exitCode = explorerApp.run(stateSpaceGenerationArgs, false);
        if (exitCode != 0) {
            throw new RuntimeException(
                    "Non-zero exit code for state space generation: " + exitCode + "\n" + explorerAppStream.toString());
        }

        // Transform the state space by creating a single (initial) source and a single (marked) sink location.
        Path cifStatespaceWithSingleSourceSink = outputFolderPath
                .resolve(filePrefix + ".05.statespace.singlesourcesink.cif");
        Specification cifStateSpace = CifFileHelper.loadCifSpec(cifStateSpacePath);
        CifSourceSinkLocationTransformer.transform(cifStateSpace, cifStatespaceWithSingleSourceSink, outputFolderPath,
                tracker);

        // Perform event-based automaton projection and update the synthesis tracker.
        Pair<String, Set<String>> preservedAndRemovedEventNames = getPreservedAndRemovedEventNames(cifStateSpace,
                tracker);
        String preservedEventNames = preservedAndRemovedEventNames.left;
        Set<String> removedEventNames = preservedAndRemovedEventNames.right;
        tracker.removeAndUpdateEvents(removedEventNames, UmlToCifTranslationPurpose.SYNTHESIS);
        Path cifProjectedStateSpacePath = outputFolderPath.resolve(filePrefix + ".06.statespace.projected.cif");
        String[] projectionArgs = new String[] {cifStatespaceWithSingleSourceSink.toString(),
                "--preserve=" + preservedEventNames, "--output=" + cifProjectedStateSpacePath.toString()};
        AppStream projectionAppStream = new MemAppStream();
        AppStreams projectionAppStreams = new AppStreams(InputStream.nullInputStream(), projectionAppStream,
                projectionAppStream, projectionAppStream);
        ProjectionApplication projectionApp = new ProjectionApplication(projectionAppStreams);
        exitCode = projectionApp.run(projectionArgs, false);
        if (exitCode != 0) {
            throw new RuntimeException("Non-zero exit code for event-based automaton projection: " + exitCode + "\n"
                    + projectionAppStream.toString());
        }

        // Perform DFA minimization.
        Path cifMinimizedStateSpacePath = outputFolderPath
                .resolve(filePrefix + ".07.statespace.projected.minimized.cif");
        String[] dfaMinimizationArgs = new String[] {cifProjectedStateSpacePath.toString(),
                "--output=" + cifMinimizedStateSpacePath.toString()};
        AppStream dfaMinimizationAppStream = new MemAppStream();
        AppStreams dfaMinimizationAppStreams = new AppStreams(InputStream.nullInputStream(), dfaMinimizationAppStream,
                dfaMinimizationAppStream, dfaMinimizationAppStream);
        DfaMinimizationApplication dfaMinimizationApp = new DfaMinimizationApplication(dfaMinimizationAppStreams);
        exitCode = dfaMinimizationApp.run(dfaMinimizationArgs, false);
        if (exitCode != 0) {
            throw new RuntimeException("Non-zero exit code for DFA minimization: " + exitCode + "\n"
                    + dfaMinimizationAppStream.toString());
        }

        // Translate the CIF state space to Petrify input.
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".08.g");
        Specification cifMinimizedStateSpace = CifFileHelper.loadCifSpec(cifMinimizedStateSpacePath);
        List<String> petrifyInput = Cif2Petrify.transform(cifMinimizedStateSpace);
        Files.write(petrifyInputPath, petrifyInput);

        // Petrify the state space.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".09.out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        Path petrifyErrorPath = outputFolderPath.resolve("petrify.err");
        PetrifyHelper.convertToPetriNet(petrifyInputPath, petrifyOutputPath,
                ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"),
                petrifyLogPath, petrifyErrorPath, 20);

        // Load Petrify output.
        List<String> petrifyOutput = PetrifyHelper.readFile(petrifyOutputPath.toString());

        // Translate Petrify output into PNML.
        Path pnmlWithLoopOutputPath = outputFolderPath.resolve(filePrefix + ".10.pnml");
        PetriNet petriNet = PetrifyOutput2PNMLTranslator.transform(new ArrayList<>(petrifyOutput));
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithLoopOutputPath.toString());

        // Remove the self-loop that was added for petrification.
        Path pnmlWithoutLoopOutputPath = outputFolderPath.resolve(filePrefix + ".11.loopremoved.pnml");
        PostProcessPNML.removeLoop(petriNet);
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithoutLoopOutputPath.toString());

        // Store the Petri net transitions in the synthesis tracker. It is more convenient to use the Petri net after it
        // has been synthesised, instead of storing each transition at the time of creation: in case a transition
        // appears multiple times in a Petri net, Petrify distinguishes each duplicate by adding a postfix to the name
        // of the transition (e.g., 'Transition_A/1' is a duplicate of 'Transition_A'), and these duplicates are not
        // specified in the transition declarations, but only appear in the specification, and are handled separately.
        tracker.addPetriNetTransitions(petriNet);

        // Rewrite all rewritable non-atomic patterns in the Petri Net. The rewriting merges the non-atomic patterns
        // that can be merged, replacing their start and end transitions by a single transition. These patterns'
        // intermediate control flows cannot have guards and we can safely merge them, since 1) the patterns have no
        // other actions between the start and end actions; 2) we only place outgoing guards in incoming control flows
        // to actions. The end actions are uncontrollable, so synthesis will push the computed guards to whatever comes
        // before them: in this case, the start of the action.
        Path pnmlNonAtomicsReducedOutputPath = outputFolderPath.resolve(filePrefix + ".12.nonatomicsreduced.pnml");
        NonAtomicPatternRewriter nonAtomicPatternRewriter = new NonAtomicPatternRewriter(
                tracker.getNonAtomicStartEndEventMap(UmlToCifTranslationPurpose.SYNTHESIS));
        List<NonAtomicPattern> nonAtomicPatterns = nonAtomicPatternRewriter.findAndRewritePatterns(petriNet);
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlNonAtomicsReducedOutputPath.toString());

        // Update the synthesis tracker transition map with the rewritten non-atomic pattern.
        Map<Transition, List<Transition>> rewrittenTransitions = nonAtomicPatterns.stream()
                .collect(Collectors.toMap(p -> p.startTransition(), p -> p.endTransitions()));
        tracker.mergeTransitionPatterns(rewrittenTransitions);

        // Translate PNML into UML activity. The translation translates every Petri Net transition to a UML opaque
        // action.
        Path umlOutputPath = outputFolderPath.resolve(filePrefix + ".13.uml");
        PNML2UMLTranslator petriNet2Activity = new PNML2UMLTranslator(activity);
        petriNet2Activity.translate(petriNet);
        FileHelper.storeModel(activity.getModel(), umlOutputPath.toString());

        // Add the newly generated UML opaque actions and their corresponding transitions to the tracker.
        tracker.addActions(petriNet2Activity.getTransitionMapping());

        // Finalize the opaque actions of the activity. Transform opaque actions into call behaviors when they
        // correspond to atomic opaque behaviors or non-atomic ones that have been re-written in the previous step. For
        // non-atomic ones that couldn't be rewritten, add guards (for start action) and effects (for end actions).
        Path opaqueActionsFinalizedOutputPath = outputFolderPath
                .resolve(filePrefix + ".14.opaque_actions_finalized.uml");
        PostProcessActivity.finalizeOpaqueActions(activity, tracker, warnings);
        FileHelper.storeModel(activity.getModel(), opaqueActionsFinalizedOutputPath.toString());

        // Remove the internal actions that were added in CIF specification and petrification.
        Path internalActionsRemovedUMLOutputPath = outputFolderPath
                .resolve(filePrefix + ".15.internalactionsremoved.uml");
        PostProcessActivity.removeInternalActions(activity);
        FileHelper.storeModel(activity.getModel(), internalActionsRemovedUMLOutputPath.toString());

        // Post-process the activity to simplify it.
        Path umlSimplifiedOutputPath = outputFolderPath.resolve(filePrefix + ".16.simplified.uml");
        PostProcessActivity.simplify(activity);
        FileHelper.storeModel(activity.getModel(), umlSimplifiedOutputPath.toString());

        // Post-process the activity to remove the names of edges and nodes.
        Path umlLabelsRemovedOutputPath = outputFolderPath.resolve(filePrefix + ".17.labelsremoved.uml");
        PostProcessActivity.removeNodesEdgesNames(activity);
        FileHelper.storeModel(activity.getModel(), umlLabelsRemovedOutputPath.toString());

        // Translating synthesized activity to CIF, for guard computation.
        Path umlActivityToCifPath = outputFolderPath.resolve(filePrefix + ".18.guardcomputation.cif");
        UmlToCifTranslator umlActivityToCifTranslator = new UmlToCifTranslator(activity,
                UmlToCifTranslationPurpose.GUARD_COMPUTATION, tracker);
        Specification cifTranslatedActivity = umlActivityToCifTranslator.translate();
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifTranslatedActivity, makePathPair(umlActivityToCifPath),
                    outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Computing guards.
        new GuardComputation(umlActivityToCifTranslator, tracker).computeGuards(cifTranslatedActivity,
                umlActivityToCifPath);
        Path umlGuardsOutputPath = outputFolderPath.resolve(filePrefix + ".19.guardsadded.uml");
        FileHelper.storeModel(umlActivityToCifTranslator.getActivity().getModel(), umlGuardsOutputPath.toString());

        // Check the activity for non-deterministic choices.
        CheckNonDeterministicChoices.check(activity, umlToCifTranslator, warnings, cifBddSpec);

        // Perform the language equivalence check between the CIF model generated by the state space exploration and the
        // translation to CIF of the final UML model. Throws a runtime error if models are non-equivalent.
        performLanguageEquivalenceCheck(filePrefix, outputFolderPath, umlToCifTranslator, tracker);
    }

    private static Pair<String, Set<String>> getPreservedAndRemovedEventNames(Specification spec,
            SynthesisChainTracking tracker)
    {
        List<Event> events = CifCollectUtils.collectEvents(spec, new ArrayList<>());

        // Preserve controllable events and all events that are *not* the end of an atomic non-deterministic action.
        // This merges (folds) the non-deterministic result events of an atomic action into the single start event. The
        // choice is based on the nodes name: in the future we might want to refer directly to the nodes instead of
        // using a string comparison.
        List<String> preservedEventNames = events.stream().filter(
                event -> event.getControllable() || !tracker.isAtomicNonDeterministicEndEventName(event.getName()))
                .map(event -> CifTextUtils.getAbsName(event, false)).toList();

        // Get the removed events names (end of atomic non-deterministic actions).
        Set<String> removedEventNames = events.stream().filter(event -> !preservedEventNames.contains(event.getName()))
                .map(e -> e.getName()).collect(Collectors.toSet());

        return new Pair<>(String.join(",", preservedEventNames), removedEventNames);
    }

    private static void performLanguageEquivalenceCheck(String filePrefix, Path localOutputPath,
            UmlToCifTranslator translator, SynthesisChainTracking tracker) throws CoreException
    {
        // Load state space UML file.
        Specification stateSpaceGenerated = CifFileHelper
                .loadCifSpec(localOutputPath.resolve(filePrefix + ".04.ctrlsys.statespace.cif"));

        // Translate final UML model to CIF and get its state space.
        UmlToCifTranslator umlToCifTranslatorPostSynth = new UmlToCifTranslator(translator.getActivity(),
                UmlToCifTranslationPurpose.LANGUAGE_EQUIVALENCE, tracker);
        Specification cifSpec = umlToCifTranslatorPostSynth.translate();
        Path cifSpecPath = localOutputPath.resolve(filePrefix + ".99.01.finalUmlToCif.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpec, makePathPair(cifSpecPath), localOutputPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Perform state space generation.
        Path cifStateSpacePath = localOutputPath.resolve(filePrefix + ".99.02.ctrlsys.statespace.cif");
        String[] stateSpaceGenerationArgs = new String[] {cifSpecPath.toString(),
                "--name=post_synthesis_chain_state_space", "--output=" + cifStateSpacePath.toString()};
        AppStream explorerAppStream = new MemAppStream();
        AppStreams explorerAppStreams = new AppStreams(InputStream.nullInputStream(), explorerAppStream,
                explorerAppStream, explorerAppStream);
        ExplorerApplication explorerApp = new ExplorerApplication(explorerAppStreams);
        int exitCode = explorerApp.run(stateSpaceGenerationArgs, false);
        if (exitCode != 0) {
            throw new RuntimeException(
                    "Non-zero exit code for state space generation: " + exitCode + "\n" + explorerAppStream.toString());
        }

        // Load state space post-synthesis chain file.
        Specification stateSpacePostSynthChain = CifFileHelper.loadCifSpec(cifStateSpacePath);

        // Filter the state annotations to keep only the external variables, and get the tau and non-tau events before
        // the language equivalence check.
        ModelPreparationResult result = StateAwareWeakLanguageEquivalenceHelper.prepareModels(stateSpaceGenerated,
                translator.getNormalizedNameToEventsMap(), translator.getInternalEvents(), stateSpacePostSynthChain,
                umlToCifTranslatorPostSynth.getNormalizedNameToEventsMap(),
                umlToCifTranslatorPostSynth.getInternalEvents(), translator.getVariableNames());

        // Get the two state space automata to compare.
        Automaton stateSpace1 = (Automaton)stateSpaceGenerated.getComponents().get(0);
        Automaton stateSpace2 = (Automaton)stateSpacePostSynthChain.getComponents().get(0);

        // Perform the language equivalence check.
        StateAwareWeakLanguageEquivalenceChecker checker = new StateAwareWeakLanguageEquivalenceChecker();
        checker.check(stateSpace1, result.stateAnnotations1(), translator.getInternalEvents(), stateSpace2,
                result.stateAnnotations2(), umlToCifTranslatorPostSynth.getInternalEvents(), result.pairedEvents());
    }

    private static PathPair makePathPair(Path path) {
        return new PathPair(path.toString(), path.toAbsolutePath().toString());
    }
}
