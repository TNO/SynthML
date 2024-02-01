
package com.github.tno.pokayoke.transform.region2statemapping;

import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.escet.common.java.Triple;

import com.github.tno.pokayoke.transform.petrify2uml.PetriNet2ActivityHelper;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

public class ExtractRegionStateMapping {
    private ExtractRegionStateMapping() {
    }

    /**
     * Extracts a region-state map from the given Petri net and state machine.
     *
     * @param petriNet The Petri Net.
     * @param petrifyInput The Petrify input that contains a state machine.
     * @return A map from state in the state machine to region (i.e., a set of places) in the Petri net.
     */
    public static Map<String, Set<String>> extract(PetriNet petriNet, List<String> petrifyInput) {
        // Initialize an empty map from place to states.
        List<Place> places = petriNet.getPages().get(0).getObjects().stream().filter(Place.class::isInstance)
                .map(Place.class::cast).toList();
        Map<String, Set<String>> regionStateMap = new LinkedHashMap<>();
        places.stream().forEach(place -> regionStateMap.put(place.getName().getText(), new LinkedHashSet<>()));

        // Initialize a queue that stores pairs of state and corresponding places to be visited. The first pair is the
        // initial state (i.e., loc0 as added in the CIF2Petrify step) and the initial marked place.
        List<Place> markedPlace = places.stream().filter(place -> PetriNet2ActivityHelper.isMarkedPlace(place))
                .toList();
        String initialState = "loc0";
        Queue<Pair<String, Set<Place>>> queue = new LinkedList<>();
        queue.add(Pair.of(initialState, new LinkedHashSet<>(markedPlace)));

        // Initialize a list for the visited pairs of state and places.
        Set<Pair<String, Set<Place>>> visited = new LinkedHashSet<>();

        // Get transitions of the state machine.
        List<Triple<String, String, String>> transitions = getTransitions(petrifyInput);

        // Co-simulate the state machine and Petri net while the queue is not empty.
        while (!queue.isEmpty()) {
            Pair<String, Set<Place>> statePlacesPair = queue.poll();

            // Process the pair when it has not been visited yet.
            if (!visited.contains(statePlacesPair)) {
                visited.add(statePlacesPair);

                // Get the current state and places (with tokens).
                String currentState = statePlacesPair.getLeft();
                Set<Place> currentPlace = statePlacesPair.getRight();

                // Update the region-state map with current state for each current place.
                currentPlace.stream().forEach(place -> regionStateMap.get(place.getName().getText()).add(currentState));

                // Get the transitions to be fired.
                List<Triple<String, String, String>> transitionsToFire = transitions.stream()
                        .filter(transition -> transition.first.equals(currentState)).toList();

                // Get the firing result of each transition and push the next pair of state and places into the queue.
                for (Triple<String, String, String> transition: transitionsToFire) {
                    String transitionLabel = transition.second;
                    String nextState = transition.third;
                    Set<Place> nextPlaces = fire(transitionLabel, currentPlace);
                    queue.add(Pair.of(nextState, nextPlaces));
                }
            }
        }
        return regionStateMap;
    }

    private static List<Triple<String, String, String>> getTransitions(List<String> petrifyInput) {
        List<Triple<String, String, String>> transitions = new ArrayList<>();

        // Get transition lines from the Petrify output. All the lines that do not start with '.' are the transition
        // lines.
        List<String> transitionLines = petrifyInput.stream().filter(line -> !line.startsWith(".")).toList();
        for (String transition: transitionLines) {
            String[] components = transition.split(" ");
            String sourceState = components[0];
            String transitionLabel = components[1];
            String targetState = components[2];
            transitions.add(new Triple<>(sourceState, transitionLabel, targetState));
        }
        return transitions;
    }

    private static Set<Place> fire(String transitionLabel, Set<Place> currentPlaces) {
        // Initialize the next places with a copy of the current places.
        Set<Place> nextPlaces = currentPlaces.stream().collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

        // For each current place, the connected transitions are collected, and fired if allowed.
        for (Place currentPlace: currentPlaces) {
            Set<Transition> transitions = currentPlace.getOutArcs().stream().map(arc -> arc.getTarget())
                    .map(Transition.class::cast).filter(t -> t.getName().getText().equals(transitionLabel))
                    .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

            for (Transition transition: transitions) {
                List<Place> sourcePlacesofTransition = transition.getInArcs().stream().map(arc -> arc.getSource())
                        .map(Place.class::cast).toList();

                // If the source places of the transition are the subset of the current places, the transition can be
                // fired.
                if (currentPlaces.containsAll(sourcePlacesofTransition)) {
                    List<Place> targetPlacesofTransition = transition.getOutArcs().stream().map(arc -> arc.getTarget())
                            .map(Place.class::cast).toList();

                    // The current place is removed from the next places and the target places of the transition are
                    // added to the next places (i.e., the token is moved from the current place to the target places).
                    nextPlaces.remove(currentPlace);
                    nextPlaces.addAll(targetPlacesofTransition);
                }
            }
        }

        return nextPlaces;
    }
}
