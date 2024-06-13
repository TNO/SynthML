
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.app.framework.AppEnv;

import com.google.common.base.Preconditions;

/**
 * Functionality for transforming CIF automata by creating a single source (initial) location and a single sink (marked)
 * location in the automaton, with corresponding new edges that go from the new source location to all original initial
 * locations, and from all original marked locations to the new sink location. The initials and markeds of the original
 * initial/marked locations will then be removed.
 */
public class CifSourceSinkLocationTransformer {
    private static final String START_LOCATION_NAME = "__init";

    private static final String END_LOCATION_NAME = "__done";

    private static final String START_EVENT_NAME = "__start";

    private static final String END_EVENT_NAME = "__end";

    private CifSourceSinkLocationTransformer() {
        // Static class.
    }

    /**
     * Transforms the given specification (as described for {@link #transform(Specification)}) and writes the
     * transformed specification to the indicated file path.
     *
     * @param specification The input specification to transform, which should contain exactly one automaton.
     * @param outputFilePath The path to the output file to write the transformation result to.
     * @param outputFolderPath The path to the folder in which the transformation result is to be written.
     */
    public static void transform(Specification specification, Path outputFilePath, Path outputFolderPath) {
        transform(specification);

        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(specification, outputFilePath.toString(), outputFolderPath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    /**
     * Transforms the given specification by replacing all initial/marked locations by a new source (initial) location
     * and a new sink (marked) location.
     *
     * @param specification The input specification to transform, which should contain exactly one automaton.
     */
    public static void transform(Specification specification) {
        // Make sure the input specification does not contain any complex component with separate initials or markeds.
        Preconditions.checkArgument(
                CifCollectUtils.getComplexComponentsStream(specification).allMatch(c -> c.getInitials().isEmpty()),
                "Expected the input specification to not contain any initials.");
        Preconditions.checkArgument(
                CifCollectUtils.getComplexComponentsStream(specification).allMatch(c -> c.getMarkeds().isEmpty()),
                "Expected the input specification to not contain any markeds.");

        // Make sure the input specification does not already contain a declaration with the new start/end event name.
        List<Declaration> declarations = CifCollectUtils.collectDeclarations(specification, new ArrayList<>());
        Set<String> newEventNames = Set.of(START_EVENT_NAME, END_EVENT_NAME);
        Preconditions.checkArgument(declarations.stream().noneMatch(decl -> newEventNames.contains(decl.getName())),
                "Expected the input specification to contain no declaration with the new start/end event name.");

        // Obtain the single automaton in the input specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(specification, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the input specification to contain exactly one automaton.");
        Automaton automaton = automata.get(0);

        // Make sure the automaton does not already contain a location with the new start/end location name.
        Set<String> newLocationNames = Set.of(START_LOCATION_NAME, END_LOCATION_NAME);
        Preconditions.checkArgument(
                automaton.getLocations().stream().noneMatch(loc -> newLocationNames.contains(loc.getName())),
                "Expected none of the location in the input automaton with the new start/end location name. ");

        // First, we make a new initial location, which will become the single initial (source) location.

        // Find the set of all initial locations.
        Set<Location> initialLocations = automaton.getLocations().stream()
                .filter(CifSourceSinkLocationTransformer::isInitial)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Create a new initial location that has all the annotations of all original initial locations.
        Location newInitialLocation = CifConstructors.newLocation();
        newInitialLocation.getAnnotations().addAll(
                initialLocations.stream().flatMap(loc -> loc.getAnnotations().stream()).map(EcoreUtil::copy).toList());
        newInitialLocation.getInitials()
                .add(CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), true));
        newInitialLocation.setName(START_LOCATION_NAME);
        automaton.getLocations().add(newInitialLocation);

        // Declare a new start event and add it to the alphabet of the automaton.
        Event startEvent = CifConstructors.newEvent();
        startEvent.setControllable(true);
        startEvent.setName(START_EVENT_NAME);
        specification.getDeclarations().add(startEvent);
        automaton.getAlphabet().getEvents()
                .add(CifConstructors.newEventExpression(startEvent, null, CifConstructors.newBoolType()));

        // Add edges from the new initial location to all original initial locations, which replace their initials.
        for (Location location: initialLocations) {
            Edge edge = CifConstructors.newEdge();
            edge.getEvents().add(CifConstructors.newEdgeEvent(
                    CifConstructors.newEventExpression(startEvent, null, CifConstructors.newBoolType()), null));
            edge.setTarget(location);
            newInitialLocation.getEdges().add(edge);
            location.getInitials().clear();
        }

        // Next, we make a new marked location, which will become the single marked (sink) location.

        // Find the set of all marked locations.
        Set<Location> markedLocations = automaton.getLocations().stream()
                .filter(CifSourceSinkLocationTransformer::isMarked)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Create a new marked location that has all the annotations of all original marked locations.
        Location newMarkedLocation = CifConstructors.newLocation();
        newMarkedLocation.getAnnotations().addAll(
                markedLocations.stream().flatMap(loc -> loc.getAnnotations().stream()).map(EcoreUtil::copy).toList());
        newMarkedLocation.getMarkeds()
                .add(CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), true));
        newMarkedLocation.setName(END_LOCATION_NAME);
        automaton.getLocations().add(newMarkedLocation);

        // Declare a new end event and add it to the alphabet of the automaton.
        Event endEvent = CifConstructors.newEvent();
        endEvent.setControllable(true);
        endEvent.setName(END_EVENT_NAME);
        specification.getDeclarations().add(endEvent);
        automaton.getAlphabet().getEvents()
                .add(CifConstructors.newEventExpression(endEvent, null, CifConstructors.newBoolType()));

        // Add edges from all original marked locations to the new marked location, which replace their markeds.
        for (Location location: markedLocations) {
            Edge edge = CifConstructors.newEdge();
            edge.getEvents().add(CifConstructors.newEdgeEvent(
                    CifConstructors.newEventExpression(endEvent, null, CifConstructors.newBoolType()), null));
            edge.setTarget(newMarkedLocation);
            location.getEdges().add(edge);
            location.getMarkeds().clear();
        }
    }

    private static boolean isInitial(Location location) {
        boolean isTriviallyInitial = location.getInitials().isEmpty() ? false
                : CifValueUtils.isTriviallyTrue(location.getInitials(), true, true);
        boolean isTriviallyNotInitial = location.getInitials().isEmpty() ? true
                : CifValueUtils.isTriviallyFalse(location.getInitials(), true, true);

        Preconditions.checkArgument(isTriviallyInitial || isTriviallyNotInitial,
                "Expected that locations are either trivially initial or trivially not initial.");

        return isTriviallyInitial;
    }

    private static boolean isMarked(Location location) {
        boolean isTriviallyMarked = location.getMarkeds().isEmpty() ? false
                : CifValueUtils.isTriviallyTrue(location.getMarkeds(), false, true);
        boolean isTriviallyNotMarked = location.getMarkeds().isEmpty() ? true
                : CifValueUtils.isTriviallyFalse(location.getMarkeds(), false, true);

        Preconditions.checkArgument(isTriviallyMarked || isTriviallyNotMarked,
                "Expected that locations are either trivially marked or trivially not marked.");

        return isTriviallyMarked;
    }
}
