
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.javabdd.BDD;
import com.google.common.base.Preconditions;

/** Helper for working with event guards and updates. */
public class EventGuardUpdateHelper {
    private EventGuardUpdateHelper() {
    }

    /**
     * Collect uncontrolled system guards from a CIF/BDD specification.
     *
     * @param cifBddSpec The CIF/BDD specification.
     * @return A map from CIF events to their guards as BDDs.
     */
    public static Map<Event, BDD> collectUncontrolledSystemGuards(CifBddSpec cifBddSpec) {
        Map<Event, BDD> guards = new LinkedHashMap<>();
        for (Entry<Event, List<CifBddEdge>> entry: cifBddSpec.eventEdges.entrySet()) {
            List<CifBddEdge> cifBDDEdges = entry.getValue();
            Preconditions.checkArgument(cifBDDEdges.size() == 1,
                    "Expected that each event has exactly one CIF/BDD edge.");
            BDD bdd = cifBDDEdges.get(0).guard.id();
            guards.put(entry.getKey(), bdd);
        }
        return guards;
    }

    /**
     * Collect guards of controllable events from a CIF specification.
     *
     * @param cifSpec The CIF specification.
     * @return The map from events to string representations of guards.
     */
    public static Map<Event, String> collectSpecificationControllableEventGuards(Specification cifSpec) {
        Map<Event, String> eventGuards = new LinkedHashMap<>();
        List<Edge> edges = getEdges(cifSpec);

        for (Edge edge: edges) {
            Event event = getEvent(edge);
            if (event.getControllable()) {
                ConvertExpressionToText converter = new ConvertExpressionToText();

                // Move the declarations to the root of the CIF specification.
                converter.moveDeclarations(cifSpec);

                eventGuards.put(event, CifTextUtils.exprsToStr(edge.getGuards()));

                // Move the declarations back to their original scopes. This may change the order of the declarations.
                converter.revertDeclarationsMove();
            }
        }
        return eventGuards;
    }

    private static List<Edge> getEdges(Specification cifSpec) {
        List<Automaton> automata = CifCollectUtils.collectAutomata(cifSpec, new ArrayList<>()).stream()
                .filter(aut -> aut.getName().equals("Spec")).toList();
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to contain exactly one automaton called 'Spec'.");
        Automaton automaton = automata.get(0);

        List<Location> locations = automaton.getLocations();
        Preconditions.checkArgument(locations.size() == 1,
                "Expected the automaton in CIF specification to contain exactly one location.");
        Location location = locations.get(0);
        List<Edge> edges = location.getEdges();

        return edges;
    }

    private static Event getEvent(Edge edge) {
        Set<Event> events = CifEventUtils.getEvents(edge);
        Preconditions.checkArgument(events.size() == 1, "Expected the edge to contain exactly one event.");
        return events.iterator().next();
    }

    /**
     * Collect updates of controllable events from a CIF specification.
     *
     * @param cifSpec The CIF specification.
     * @return The map from events to string representations of updates.
     */
    public static Map<Event, String> collectSpecificationControllableEventUpdates(Specification cifSpec) {
        Map<Event, String> eventUpdates = new LinkedHashMap<>();
        List<Edge> edges = getEdges(cifSpec);
        for (Edge edge: edges) {
            Event event = getEvent(edge);
            if (event.getControllable()) {
                ConvertExpressionToText converter = new ConvertExpressionToText();

                // Move the declarations to the root of the CIF specification.
                converter.moveDeclarations(cifSpec);

                eventUpdates.put(event, CifTextUtils.updatesToStr(edge.getUpdates()));

                // Move the declarations back to their original scopes. This may change the order of the declarations.
                converter.revertDeclarationsMove();
            }
        }
        return eventUpdates;
    }
}
