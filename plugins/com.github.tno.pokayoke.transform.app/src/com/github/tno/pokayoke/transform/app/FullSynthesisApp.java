
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.eventbased.apps.DfaMinimizationApplication;
import org.eclipse.escet.cif.eventbased.apps.ProjectionApplication;
import org.eclipse.escet.cif.explorer.app.ExplorerApplication;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.common.app.framework.io.AppStream;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.io.MemAppStream;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.OpaqueAction;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.activitysynthesis.CIFDataSynthesisHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.ChoiceActionGuardComputation;
import com.github.tno.pokayoke.transform.activitysynthesis.ConvertExpressionUpdateToText;
import com.github.tno.pokayoke.transform.activitysynthesis.EventGuardUpdateHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.OpaqueActionHelper;
import com.github.tno.pokayoke.transform.activitysynthesis.StateAnnotationHelper;
import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.cif2petrify.CifFileHelper;
import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.NormalizePetrifyOutput;
import com.github.tno.pokayoke.transform.petrify2uml.PNML2UMLActivity;
import com.github.tno.pokayoke.transform.petrify2uml.PNMLUMLFileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.PetrifyOutput2PNMLTranslator;
import com.github.tno.pokayoke.transform.petrify2uml.PostProcessActivity;
import com.github.tno.pokayoke.transform.petrify2uml.PostProcessPNML;
import com.github.tno.pokayoke.transform.region2statemapping.ExtractRegionStateMapping;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    private FullSynthesisApp() {
    }

    public static void performFullSynthesis(Path inputPath, Path outputFolderPath) throws IOException {
        Files.createDirectories(outputFolderPath);
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());

        // Load CIF specification.
        Specification cifSpec = CifFileHelper.loadCifSpec(inputPath);

        // Get CIF/BDD specification.
        Path cifSynthesisPath = outputFolderPath.resolve(filePrefix + ".01.ctrlsys.cif");
        CifDataSynthesisSettings settings = CIFDataSynthesisHelper.getSynthesisSettings();
        CifBddSpec cifBddSpec = CIFDataSynthesisHelper.getCifBddSpec(cifSpec, settings);

        // Get the BDDs of uncontrolled system guards before performing synthesis.
        Map<Event, BDD> uncontrolledSystemGuards = EventGuardUpdateHelper.collectUncontrolledSystemGuards(cifBddSpec);

        // Perform synthesis.
        CifDataSynthesisResult cifSynthesisResult = CIFDataSynthesisHelper.synthesize(cifBddSpec, settings);

        // Convert synthesis result back to CIF.
        CIFDataSynthesisHelper.convertSynthesisResultToCif(cifSpec, cifSynthesisResult, cifSynthesisPath.toString(),
                outputFolderPath.toString());

        // Perform state space generation.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".02.ctrlsys.statespace.cif");
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

        // Remove state annotations from intermediate states.
        Path cifAnnotReducedStateSpacePath = outputFolderPath.resolve(filePrefix + ".03.statespace.annotreduced.cif");
        Specification cifStateSpace = CifFileHelper.loadCifSpec(cifStateSpacePath);
        Specification cifReducedStateSpace = EcoreUtil.copy(cifStateSpace);
        StateAnnotationHelper.reduceStateAnnotations(cifReducedStateSpace, cifAnnotReducedStateSpacePath,
                outputFolderPath);

        // Remove state annotations from all states.
        Path cifAnnotRemovedStateSpacePath = outputFolderPath.resolve(filePrefix + ".04.statespace.annotremoved.cif");
        StateAnnotationHelper.removeStateAnnotations(cifStateSpace, cifAnnotRemovedStateSpacePath, outputFolderPath);

        // Perform event-based automaton projection.
        String preservedEvents = getPreservedEvents(cifStateSpace);
        Path cifProjectedStateSpacePath = outputFolderPath
                .resolve(filePrefix + ".05.statespace.annotremoved.projected.cif");
        String[] projectionArgs = new String[] {cifAnnotRemovedStateSpacePath.toString(),
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
                .resolve(filePrefix + ".06.statespace.annotremoved.projected.minimized.cif");
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
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".07.g");
        Specification cifMinimizedStateSpace = CifFileHelper.loadCifSpec(cifMinimizedStateSpacePath);
        List<String> petrifyInput = Cif2Petrify.transform(cifMinimizedStateSpace);
        Files.write(petrifyInputPath, petrifyInput);

        // Petrify the state space.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".08.out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        convertToPetriNet(petrifyInputPath, petrifyOutputPath, petrifyLogPath, 20);

        // Load Petrify output.
        List<String> petrifyOutput = PNMLUMLFileHelper.readFile(petrifyOutputPath.toString());

        // Normalize Petrify output by relabeling the places.
        petrifyOutput = NormalizePetrifyOutput.normalize(petrifyOutput);
        Files.write(petrifyOutputPath, petrifyOutput);

        // Translate Petrify output into PNML.
        Path pnmlWithLoopOutputPath = outputFolderPath.resolve(filePrefix + ".09.pnml");
        PetriNet petriNet = PetrifyOutput2PNMLTranslator.transform(new ArrayList<>(petrifyOutput));
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithLoopOutputPath.toString());

        // Extract region-state mapping.
        Map<Place, Set<String>> regionMap = ExtractRegionStateMapping.extract(petrifyInput, petriNet);

        // Remove the loop that was added for petrification.
        Path pnmlWithoutLoopOutputPath = outputFolderPath.resolve(filePrefix + ".10.loopremoved.pnml");
        PostProcessPNML.removeLoop(petriNet);
        PNMLUMLFileHelper.writePetriNet(petriNet, pnmlWithoutLoopOutputPath.toString());

        // Translate PNML into UML activity.
        Path umlOutputPath = outputFolderPath.resolve(filePrefix + ".11.uml");
        PNML2UMLActivity petriNet2Activity = new PNML2UMLActivity();
        Activity activity = petriNet2Activity.transform(petriNet);
        Map<Transition, OpaqueAction> transitionToAction = petriNet2Activity.getTransitionActionMap();
        FileHelper.storeModel(activity.getModel(), umlOutputPath.toString());

        // Remove the internal actions that were added in CIF specification and
        // petrification.
        Path internalActionsRemovedUMLOutputPath = outputFolderPath
                .resolve(filePrefix + ".12.internalactionsremoved.uml");
        PostProcessActivity.removeInternalActions(activity);
        FileHelper.storeModel(activity.getModel(), internalActionsRemovedUMLOutputPath.toString());

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

        // Compute choice guards.
        ChoiceActionGuardComputation guardComputation = new ChoiceActionGuardComputation(cifMinimizedStateSpace,
                uncontrolledSystemGuards, cifSynthesisResult, petriNet, minimizedToReduced, regionMap);
        Map<Transition, Expression> choiceTransitionToGuard = guardComputation.computeChoiceGuards();
        uncontrolledSystemGuards.values().stream().forEach(guard -> guard.free());

        // Get a map from actions to the guards of the incoming edges of the actions.
        Map<OpaqueAction, Expression> choiceActionToGuardExpression = new LinkedHashMap<>();
        choiceTransitionToGuard.forEach((transition, expression) -> choiceActionToGuardExpression
                .put(transitionToAction.get(transition), expression));

        // Convert CIF expression of choice guards into CIF expression text.
        ConvertExpressionUpdateToText converter = new ConvertExpressionUpdateToText();
        Map<OpaqueAction, String> choiceActionToGuardText = new LinkedHashMap<>();
        choiceActionToGuardExpression.forEach((action, expression) -> choiceActionToGuardText.put(action,
                converter.convertExpressions(cifSpec, Arrays.asList(expression))));

        // Add the guards for the edges that go from decision nodes to the opaque actions.
        OpaqueActionHelper.addGuardToIncomingEdges(choiceActionToGuardText);

        // Get the string representations of specification guards and updates.
        Map<Event, String> specificationGuards = EventGuardUpdateHelper.collectSpecificationEventGuards(cifSpec);
        Map<Event, String> specificationUpdates = EventGuardUpdateHelper.collectSpecificationEventUpdates(cifSpec);

        // Add the guards and updates to the opaque actions.
        OpaqueActionHelper.addGuardStringsToOpaqueActionBodies(activity, specificationGuards);
        OpaqueActionHelper.addUpdateStringsToOpaqueActionBodies(activity, specificationUpdates);

        Path umlWithGuardsUpdatesOutputPath = outputFolderPath
                .resolve(filePrefix + ".13.internalactionsremoved.guardsandupdatesadded.uml");
        FileHelper.storeModel(activity.getModel(), umlWithGuardsUpdatesOutputPath.toString());

        // Post-process to remove the names of edges and nodes.
        Path umlLabelsRemovedOutputPath = outputFolderPath
                .resolve(filePrefix + ".14.internalactionsremoved.guardsandupdatesadded.labelsremoved.uml");
        PostProcessActivity.removeNodesEdgesNames(activity);
        FileHelper.storeModel(activity.getModel(), umlLabelsRemovedOutputPath.toString());
    }

    private static String getPreservedEvents(Specification spec) {
        List<Event> events = CifCollectUtils.collectEvents(spec, new ArrayList<>());
        List<String> eventNames = events.stream().filter(event -> event.getControllable())
                .map(event -> CifTextUtils.getAbsName(event, false)).toList();

        return String.join(",", eventNames);
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param petrifyLogPath The path of the Petrify log file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    private static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, Path petrifyLogPath,
            int timeoutInSeconds)
    {
        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();
        Path parentPath = petrifyInputPath.getParent();
        command.add(ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"));
        command.add(parentPath.relativize(petrifyInputPath).toString());
        command.add("-o");
        command.add(parentPath.relativize(petrifyOutputPath).toString());

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
    }
}
