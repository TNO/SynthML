
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.cif2cif.RemoveAnnotations;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.eventbased.apps.DfaMinimizationApplication;
import org.eclipse.escet.cif.eventbased.apps.ProjectionApplication;
import org.eclipse.escet.cif.explorer.app.ExplorerApplication;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.annotations.AnnotationArgument;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.StringExpression;
import org.eclipse.escet.common.app.framework.AppEnv;

import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.cif2petrify.FileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.PetriNet2Activity;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    private FullSynthesisApp() {
    }

    public static void performFullSynthesis(Path inputPath, Path outputFolderPath) throws IOException {
        Files.createDirectories(outputFolderPath);
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());

        // Perform Synthesis.
        // TODO when the synthesis specification is formalized.

        // Perform state space generation.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.cif");
        String[] stateSpaceGenerationArgs = new String[] {inputPath.toString(),
                "--output=" + cifStateSpacePath.toString()};
        ExplorerApplication explorerApp = new ExplorerApplication();
        explorerApp.run(stateSpaceGenerationArgs, false);

        // Remove state annotation for states that have uncontrollable events on its outgoing edges.
        Path cifAnnotReducedStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.annotreduced.cif");
        Specification cifStateSpace = FileHelper.loadCifSpec(cifStateSpacePath);
        Specification cifReducedStateSpace = reduceStateAnnotations(cifStateSpace, cifAnnotReducedStateSpacePath,
                outputFolderPath);

        // Remove state annotation for all states.
        Path cifAnnotRemovedStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.annotremoved.cif");
        Specification cifRemovedStateSpace = removeStateAnnotations(cifStateSpace, cifAnnotRemovedStateSpacePath,
                outputFolderPath);

        // Perform event-based automaton projection.
        String preservedEvents = getPreservedEvents(cifRemovedStateSpace);
        Path cifProjectedStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.projected.cif");
        String[] projectionArgs = new String[] {cifAnnotRemovedStateSpacePath.toString(),
                "--preserve=" + preservedEvents, "--output=" + cifProjectedStateSpacePath.toString()};
        ProjectionApplication projectionApp = new ProjectionApplication();
        projectionApp.run(projectionArgs, false);

        // Perform DFA minimization.
        Path cifMinimizedStateSpacePath = outputFolderPath.resolve(filePrefix + ".statespace.projected.minimized.cif");
        String[] dfaMinimizationArgs = new String[] {cifProjectedStateSpacePath.toString(),
                "--output=" + cifMinimizedStateSpacePath.toString()};
        DfaMinimizationApplication dfaMinimizationApp = new DfaMinimizationApplication();
        dfaMinimizationApp.run(dfaMinimizationArgs, false);

        // Obtain the composite state mapping.
        Map<Location, List<Annotation>> annotationFromReducedSP = getStateAnnotations(cifReducedStateSpace);
        Specification cifProjectedStateSpace = FileHelper.loadCifSpec(cifProjectedStateSpacePath);
        Map<Location, List<Annotation>> annotationFromProjectedSP = getStateAnnotations(cifProjectedStateSpace);
        Specification cifMinimizedStateSpace = FileHelper.loadCifSpec(cifMinimizedStateSpacePath);
        Map<Location, List<Annotation>> annotationFromMinimizedSP = getStateAnnotations(cifMinimizedStateSpace);

        Map<Location, List<Annotation>> minimizedToProjected = getCompositeStateAnnotations(annotationFromMinimizedSP,
                annotationFromProjectedSP);
        Map<Location, List<Annotation>> minimizedToReduced = getCompositeStateAnnotations(minimizedToProjected,
                annotationFromReducedSP);

        // TODO Guard computation.

        // Translate the CIF state space to Petrify input and output the Petrify input.
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".g");
        Cif2Petrify.transformFile(cifMinimizedStateSpacePath.toString(), petrifyInputPath.toString());

        // Petrify the state space and output the generated Petri Net.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        convertToPetriNet(petrifyInputPath, petrifyOutputPath, petrifyLogPath, 20);

        // TODO Obtain region-state mapping.

        // Translate Petri Net to UML Activity and output the activity.
        Path umlOutputPath = outputFolderPath.resolve(filePrefix + ".uml");
        PetriNet2Activity.transformFile(petrifyOutputPath.toString(), umlOutputPath.toString());
    }

    private static Specification reduceStateAnnotations(Specification spec, Path cifStateSpacePath,
            Path outputFolderPath)
    {
        Specification reducedSpec = EcoreUtil.copy(spec);
        List<Event> events = new ArrayList<>();
        CifCollectUtils.collectEvents(reducedSpec, events);
        List<String> uncontrollableEventNames = events.stream().filter(event -> !event.getControllable())
                .map(event -> event.getName()).toList();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(reducedSpec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1, "Expected the CIF specification to include one automaton.");
        Automaton automaton = automata.get(0);

        for (Location loc: automaton.getLocations()) {
            List<String> edgeEventNames = loc.getEdges().stream().map(edge -> CifEventUtils.getEvents(edge))
                    .flatMap(set -> set.stream()).map(event -> event.getName()).toList();

            if (new HashSet<>(edgeEventNames).equals(new HashSet<>(uncontrollableEventNames))) {
                List<Annotation> annotationToRemove = loc.getAnnotations().stream()
                        .filter(annotation -> annotation.getName().equals("state")).toList();
                loc.getAnnotations().removeAll(annotationToRemove);
            }
        }
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(reducedSpec, cifStateSpacePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }
        return reducedSpec;
    }

    private static Specification removeStateAnnotations(Specification spec, Path cifStateSpacePath,
            Path outputFolderPath)
    {
        Specification removedSpec = EcoreUtil.copy(spec);
        RemoveAnnotations annotationRemover = new RemoveAnnotations();
        annotationRemover.transform(removedSpec);
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(removedSpec, cifStateSpacePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }
        return removedSpec;
    }

    private static String getPreservedEvents(Specification spec) {
        List<Event> events = new ArrayList<>();
        CifCollectUtils.collectEvents(spec, events);
        List<String> eventNames = events.stream().filter(event -> event.getControllable())
                .map(event -> CifTextUtils.getAbsName(event, false)).toList();

        return String.join(",", eventNames);
    }

    private static Map<Location, List<Annotation>> getStateAnnotations(Specification spec) {
        Map<Location, List<Annotation>> locationAnnotationMap = new HashMap<>();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1, "Expected the CIF specification to include one automaton.");
        Automaton automaton = automata.get(0);

        for (Location location: automaton.getLocations()) {
            List<Annotation> annotations = location.getAnnotations().stream()
                    .filter(annotation -> annotation.getName().equals("state")).toList();
            locationAnnotationMap.put(location, annotations);
        }

        return locationAnnotationMap;
    }

    private static Map<Location, List<Annotation>> getCompositeStateAnnotations(Map<Location, List<Annotation>> map1,
            Map<Location, List<Annotation>> map2)
    {
        Map<Location, List<Annotation>> compositeMap = new HashMap<>();

        for (var entry: map1.entrySet()) {
            Location location = entry.getKey();
            List<Annotation> mappedAnnotations = new ArrayList<>();

            for (Annotation annotation: entry.getValue()) {
                Preconditions.checkArgument(annotation.getArguments().size() == 1,
                        "Expected the annotation to have one argument.");
                AnnotationArgument arguement = annotation.getArguments().get(0);
                String mappedLocationName = ((StringExpression)arguement.getValue()).getValue();
                Location mappedLocation = map2.keySet().stream().filter(loc -> loc.getName().equals(mappedLocationName))
                        .toList().get(0);
                mappedAnnotations.addAll(map2.get(mappedLocation));
            }

            mappedAnnotations = mappedAnnotations.stream().distinct().collect(Collectors.toList());
            compositeMap.put(location, mappedAnnotations);
        }

        return compositeMap;
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param petrifyLogPath The path of the Petrify log file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    public static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, Path petrifyLogPath,
            int timeoutInSeconds)
    {
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
