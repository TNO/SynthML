
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
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
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
     * Collect guards of events from a CIF specification.
     *
     * @param cifSpec The CIF specification.
     * @return The map from events to string representations of guards.
     */
    public static Map<Event, String> collectSpecificationEventGuards(Specification cifSpec) {
        Map<Event, String> eventGuards = new LinkedHashMap<>();
        List<Edge> edges = getEdges(cifSpec);

        ConvertExpressionUpdateToText converter = new ConvertExpressionUpdateToText();
        edges.stream().forEach(
                edge -> eventGuards.put(getEvent(edge), converter.convertExpressions(cifSpec, edge.getGuards())));
        return eventGuards;
    }

    private static List<Edge> getEdges(Specification cifSpec) {
        List<Automaton> automata = CifCollectUtils.collectAutomata(cifSpec, new ArrayList<>()).stream()
                .filter(aut -> aut.getKind() == SupKind.PLANT).toList();

        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to contain exactly one plant.");
        Automaton automaton = automata.get(0);

        List<Location> locations = automaton.getLocations();
        Preconditions.checkArgument(locations.size() == 1,
                "Expected the plant in CIF specification to contain exactly one location.");
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
     * Collect updates of events from a CIF specification.
     *
     * @param cifSpec The CIF specification.
     * @return The map from events to string representations of updates.
     */
    public static Map<Event, String> collectSpecificationEventUpdates(Specification cifSpec) {
        Map<Event, String> eventUpdates = new LinkedHashMap<>();
        List<Edge> edges = getEdges(cifSpec);
        ConvertExpressionUpdateToText converter = new ConvertExpressionUpdateToText();
        edges.stream().forEach(
                edge -> eventUpdates.put(getEvent(edge), converter.convertUpdates(cifSpec, edge.getUpdates())));
        return eventUpdates;
    }
}
