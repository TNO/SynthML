
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.io.AppStream;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.io.MemAppStream;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Model;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.activitysynthesis.AbstractActivityDependencyOrderer;
import com.github.tno.pokayoke.transform.activitysynthesis.CIFDataSynthesisHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.CheckNonDeterministicChoices;
import com.github.tno.pokayoke.transform.activitysynthesis.ChoiceActionGuardComputationHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.CifSourceSinkLocationTransformer;
import com.github.tno.pokayoke.transform.activitysynthesis.EventGuardUpdateHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter;
import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter.NonAtomicPattern;
import com.github.tno.pokayoke.transform.activitysynthesis.StateAnnotationHelper;
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
import com.github.tno.pokayoke.transform.region2statemapping.ExtractRegionStateMapping;
import com.github.tno.pokayoke.transform.uml2cif.ConcreteUmlActivityToCifTranslator;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;

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
        // Translate the UML specification to a CIF specification.
        UmlToCifTranslator umlToCifTranslator = new UmlToCifTranslator(activity);
        Specification cifSpec = umlToCifTranslator.translate();
        Path cifSpecPath = outputFolderPath.resolve(filePrefix + ".01.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpec, cifSpecPath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Post-process the CIF specification to eliminate all if-updates.
        ElimIfUpdates elimIfUpdates = new ElimIfUpdates();
        elimIfUpdates.transform(cifSpec);
        Path cifPostProcessedSpecPath = outputFolderPath.resolve(filePrefix + ".02.postprocessed.cif");
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpec, cifPostProcessedSpecPath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Get CIF/BDD specification.
        CifDataSynthesisSettings settings = CIFDataSynthesisHelper.getSynthesisSettings();
        CifBddSpec cifBddSpec = CIFDataSynthesisHelper.getCifBddSpec(cifSpec, settings);

        // Get the BDDs of uncontrolled system guards before performing synthesis.
        Map<String, BDD> uncontrolledSystemGuards = EventGuardUpdateHelper.collectUncontrolledSystemGuards(cifBddSpec,
                umlToCifTranslator);

        // Perform synthesis.
        CifDataSynthesisResult cifSynthesisResult = CIFDataSynthesisHelper.synthesize(cifBddSpec, settings);
        Map<String, BDD> controlledSystemGuards = EventGuardUpdateHelper
                .collectControlledSystemGuards(cifSynthesisResult);

        // Convert synthesis result back to CIF.
        Path cifSynthesisPath = outputFolderPath.resolve(filePrefix + ".03.ctrlsys.cif");
        CIFDataSynthesisHelper.convertSynthesisResultToCif(cifSpec, cifSynthesisResult, cifSynthesisPath.toString(),
                outputFolderPath.toString());

        // Perform state space generation.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".04.ctrlsys.statespace.cif");
        String[] stateSpaceGenerationArgs = new String[] {cifSynthesisPath.toString(),
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
        CifSourceSinkLocationTransformer.transform(cifStateSpace, cifStatespaceWithSingleSourceSink, outputFolderPath);

        // Extend the uncontrollable system guards mapping with all auxiliary events that were introduced so far.
        CifSourceSinkLocationTransformer.addAuxiliarySystemGuards(uncontrolledSystemGuards, cifBddSpec,
                umlToCifTranslator);

        // Remove state annotations from intermediate states. Note that this removal might make the CIF specification
        // technically invalid, since it may then have locations with state annotations as well as locations without
        // state annotations. However, this is still fine for our internal analysis.
        Path cifAnnotReducedStateSpacePath = outputFolderPath.resolve(filePrefix + ".06.statespace.annotreduced.cif");
        Specification cifReducedStateSpace = EcoreUtil.copy(cifStateSpace);
        StateAnnotationHelper.reduceStateAnnotations(cifReducedStateSpace, cifAnnotReducedStateSpacePath,
                outputFolderPath);

        // Perform event-based automaton projection. Note that we can't use the state space with reduced state
        // annotations from the previous step as input here, since that CIF specification might be invalid. Therefore we
        // input the earlier version of the CIF specification that still has all state annotations.
        String preservedEvents = getPreservedEvents(cifStateSpace);
        Path cifProjectedStateSpacePath = outputFolderPath.resolve(filePrefix + ".07.statespace.projected.cif");
        String[] projectionArgs = new String[] {cifStatespaceWithSingleSourceSink.toString(),
                "--preserve=" + preservedEvents, "--output=" + cifProjectedStateSpacePath.toString()};
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
                .resolve(filePrefix + ".08.statespace.projected.minimized.cif");
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
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".09.g");
        Specification cifMinimizedStateSpace = CifFileHelper.loadCifSpec(cifMinimizedStateSpacePath);
        List<String> petrifyInput = Cif2Petrify.transform(cifMinimizedStateSpace);
        Files.write(petrifyInputPath, petrifyInput);

        // Petrify the state space.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".10.out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        Path petrifyErrorPath = outputFolderPath.resolve("petrify.err");
        PetrifyHelper.convertToPetriNet(petrifyInputPath, petrifyOutputPath,
                ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"),
                petrifyLogPath, petrifyErrorPath, 20);

        // Load Petrify output.
        List<String> petrifyOutput = PetrifyHelper.readFile(petrifyOutputPath.toString());

        // Translate Petrify output into PNML.
        Path pnmlWithLoopOutputPath = outputFolderPath.resolve(filePrefix + ".11.pnml");
        PetriNet petriNet = PetrifyOutput2PNMLTranslator.transform(new ArrayList<>(petrifyOutput));
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithLoopOutputPath.toString());

        // Extract region-state mapping.
        Map<Place, Set<String>> regionMap = ExtractRegionStateMapping.extract(petrifyInput, petriNet);

        // Remove the self-loop that was added for petrification.
        Path pnmlWithoutLoopOutputPath = outputFolderPath.resolve(filePrefix + ".12.loopremoved.pnml");
        PostProcessPNML.removeLoop(petriNet);
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithoutLoopOutputPath.toString());

        // Obtain the composite state mapping.
        Map<Location, List<Annotation>> annotationFromReducedSP = StateAnnotationHelper
                .getStateAnnotations(cifReducedStateSpace);
        Specification cifProjectedStateSpace = CifFileHelper.loadCifSpec(cifProjectedStateSpacePath);
        Map<Location, List<Annotation>> annotationFromProjectedSP = StateAnnotationHelper
                .getStateAnnotations(cifProjectedStateSpace);
        Map<Location, List<Annotation>> annotationFromMinimizedSP = StateAnnotationHelper
                .getStateAnnotations(cifMinimizedStateSpace);
        Map<Location, List<Annotation>> minimizedToProjected = StateAnnotationHelper
                .getCompositeStateAnnotations(annotationFromMinimizedSP, annotationFromProjectedSP);
        Map<Location, List<Annotation>> minimizedToReduced = StateAnnotationHelper
                .getCompositeStateAnnotations(minimizedToProjected, annotationFromReducedSP);

        // Rewrite all rewritable non-atomic patterns in the Petri Net.
        Map<Place, BDD> stateInfo = ChoiceActionGuardComputationHelper.computeStateInformation(regionMap,
                minimizedToReduced, cifMinimizedStateSpace, cifBddSpec);
        Path pnmlNonAtomicsReducedOutputPath = outputFolderPath.resolve(filePrefix + ".13.nonatomicsreduced.pnml");
        NonAtomicPatternRewriter nonAtomicPatternRewriter = new NonAtomicPatternRewriter(
                umlToCifTranslator.getNonAtomicEvents());
        List<NonAtomicPattern> nonAtomicPatterns = nonAtomicPatternRewriter.findAndRewritePatterns(petriNet);
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlNonAtomicsReducedOutputPath.toString());
        nonAtomicPatternRewriter.updateMappings(nonAtomicPatterns, stateInfo, uncontrolledSystemGuards);

        // Clean up all BDD references.
        uncontrolledSystemGuards.values().forEach(BDD::free);
        controlledSystemGuards.values().forEach(BDD::free);
        stateInfo.values().forEach(BDD::free);

        // Translate PNML into UML activity.
        Path umlOutputPath = outputFolderPath.resolve(filePrefix + ".14.uml");
        PNML2UMLTranslator petriNet2Activity = new PNML2UMLTranslator(activity);
        petriNet2Activity.translate(petriNet);
        FileHelper.storeModel(activity.getModel(), umlOutputPath.toString());

        // Rewrite any leftover non-atomic actions that weren't reduced earlier on the Petri Net level.
        Path nonAtomicsRewrittenOutputPath = outputFolderPath.resolve(filePrefix + ".16.nonatomicsrewritten.uml");
        PostProcessActivity.rewriteLeftoverNonAtomicActions(activity,
                NonAtomicPatternRewriter.getRewrittenActions(nonAtomicPatterns,
                        petriNet2Activity.getTransitionMapping()),
                umlToCifTranslator.getEndEventNameMap(), UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX, warnings);
        FileHelper.storeModel(activity.getModel(), nonAtomicsRewrittenOutputPath.toString());

        // Remove the internal actions that were added in CIF specification and petrification.
        Path internalActionsRemovedUMLOutputPath = outputFolderPath
                .resolve(filePrefix + ".17.internalactionsremoved.uml");
        PostProcessActivity.removeInternalActions(activity);
        FileHelper.storeModel(activity.getModel(), internalActionsRemovedUMLOutputPath.toString());

        // Post-process the activity to simplify it.
        Path umlSimplifiedOutputPath = outputFolderPath.resolve(filePrefix + ".18.simplified.uml");
        PostProcessActivity.simplify(activity);
        FileHelper.storeModel(activity.getModel(), umlSimplifiedOutputPath.toString());

        // Post-process the activity to remove the names of edges and nodes.
        Path umlLabelsRemovedOutputPath = outputFolderPath.resolve(filePrefix + ".19.labelsremoved.uml");
        PostProcessActivity.removeNodesEdgesNames(activity);
        FileHelper.storeModel(activity.getModel(), umlLabelsRemovedOutputPath.toString());

        // Translating synthesized activity to CIF, for guard computation.
        Path umlActivityToCifPath = outputFolderPath.resolve(filePrefix + ".20.labelsremoved.cif");
        UmlToCifTranslator umlActivityToCifTranslator = new ConcreteUmlActivityToCifTranslator(activity);
        Specification cifTranslatedActivity = umlActivityToCifTranslator.translate();
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifTranslatedActivity, umlActivityToCifPath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }

        // Check the activity for non-deterministic choices.
        CheckNonDeterministicChoices.check(activity, umlToCifTranslator, warnings, cifBddSpec);
    }

    private static String getPreservedEvents(Specification spec) {
        List<Event> events = CifCollectUtils.collectEvents(spec, new ArrayList<>());

        // Preserve only controllable events and non-controllable events that end a non-atomic action.
        List<String> eventNames = events.stream()
                .filter(event -> event.getControllable()
                        || event.getName().contains(UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX))
                .map(event -> CifTextUtils.getAbsName(event, false)).toList();

        return String.join(",", eventNames);
    }
}
