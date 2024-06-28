
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.annotations.AnnotationArgument;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.StringExpression;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.java.Sets;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Helper for manipulating state annotations. */
public class StateAnnotationHelper {
    private StateAnnotationHelper() {
    }

    /**
     * Remove state annotations from intermediate states, states that have uncontrollable events on their outgoing
     * edges.
     *
     * @param spec CIF specification from which to remove state annotations.
     * @param outputFilePath The output path of the specification.
     * @param outputFolderPath The path of the output folder.
     */
    public static void reduceStateAnnotations(Specification spec, Path outputFilePath, Path outputFolderPath) {
        Set<Event> events = CifCollectUtils.collectEvents(spec, new LinkedHashSet<>());
        Set<Event> uncontrollableEvents = events.stream().filter(event -> !event.getControllable())
                .collect(Sets.toSet());
        Set<Event> controllableEvents = events.stream().filter(event -> event.getControllable()).collect(Sets.toSet());

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to include exactly one automaton.");
        Automaton automaton = automata.get(0);

        for (Location loc: automaton.getLocations()) {
            Set<Event> edgeEvents = loc.getEdges().stream().flatMap(edge -> CifEventUtils.getEvents(edge).stream())
                    .collect(Sets.toSet());

            if (!edgeEvents.isEmpty()) {
                if (uncontrollableEvents.containsAll(edgeEvents)) {
                    List<Annotation> annotationToRemove = loc.getAnnotations().stream()
                            .filter(annotation -> annotation.getName().equals("state")).toList();
                    loc.getAnnotations().removeAll(annotationToRemove);
                } else {
                    Verify.verify(controllableEvents.containsAll(edgeEvents),
                            "Expected that the events of an edge are either controllable events or uncontrollable events.");
                }
            }
        }
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(spec, outputFilePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    /**
     * Get state annotations from a CIF specification.
     *
     * @param spec The CIF specification.
     * @return A map from locations to annotations.
     */
    public static Map<Location, List<Annotation>> getStateAnnotations(Specification spec) {
        Map<Location, List<Annotation>> locationAnnotationMap = new LinkedHashMap<>();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to include exactly one automaton.");
        Automaton automaton = automata.get(0);

        for (Location location: automaton.getLocations()) {
            List<Annotation> annotations = location.getAnnotations().stream()
                    .filter(annotation -> annotation.getName().equals("state")).toList();
            locationAnnotationMap.put(location, annotations);
        }

        return locationAnnotationMap;
    }

    /**
     * Get composite state annotations from two given maps between locations and annotations.
     *
     * @param map1 A map from locations to annotations.
     * @param map2 A map from locations to annotations.
     * @return A map from locations of {@code map1} to annotations of {@code map2}.
     */
    public static Map<Location, List<Annotation>> getCompositeStateAnnotations(Map<Location, List<Annotation>> map1,
            Map<Location, List<Annotation>> map2)
    {
        Map<Location, List<Annotation>> compositeMap = new LinkedHashMap<>();

        for (var entry: map1.entrySet()) {
            Location location = entry.getKey();
            List<Annotation> mappedAnnotations = new ArrayList<>();

            for (Annotation annotation: entry.getValue()) {
                Preconditions.checkArgument(annotation.getArguments().size() == 1,
                        "Expected the annotation to have exactly one argument.");
                AnnotationArgument argument = annotation.getArguments().get(0);
                String mappedLocationName = ((StringExpression)argument.getValue()).getValue();
                List<Location> mappedLocations = map2.keySet().stream()
                        .filter(loc -> loc.getName().equals(mappedLocationName)).toList();
                Preconditions.checkArgument(mappedLocations.size() == 1,
                        String.format("Expected that there is exactly one location named %s.", mappedLocationName));
                Location mappedLocation = mappedLocations.get(0);
                mappedAnnotations.addAll(map2.get(mappedLocation));
            }

            mappedAnnotations = mappedAnnotations.stream().distinct().toList();
            compositeMap.put(location, mappedAnnotations);
        }

        return compositeMap;
    }
}
