
package com.github.tno.pokayoke.transform.region2statemapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    public static Map<String, Set<String>> extract(PetriNet petriNet, List<String> petrifyOutput) {
        // Initialize an empty map from place to states.
        List<Place> places = petriNet.getPages().get(0).getObjects().stream().filter(Place.class::isInstance)
                .map(Place.class::cast).toList();
        Map<String, Set<String>> regionStateMap = new HashMap<>();
        places.stream().forEach(place -> regionStateMap.put(place.getName().getText(), new HashSet<>()));

        // Initialize a queue that stores pairs of state and corresponding places to be visited. The first pair is the
        // initial state (i.e., loc0 as added in the CIF2Petrify step) and the initial marked place.
        List<Place> markedPlace = places.stream().filter(place -> PetriNet2ActivityHelper.isMarkedPlace(place))
                .toList();
        String initialState = "loc0";
        Queue<Pair<String, Set<Place>>> queue = new LinkedList<>();
        queue.add(Pair.of(initialState, new HashSet<>(markedPlace)));

        // Initialize a list for the visited pairs of state and places.
        List<Pair<String, Set<Place>>> visited = new ArrayList<>();

        // Get transitions of the state machine.
        List<Triple<String, String, String>> transitions = getTransitions(petrifyOutput);

        // Co-simulate the State Machine and Petri Net while the queue is not empty.
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

    private static List<Triple<String, String, String>> getTransitions(List<String> petrifyOutput) {
        List<Triple<String, String, String>> transitions = new ArrayList<>();

        // Get transition lines from the Petrify output. All the lines that do not start with '.' are the transition
        // lines.
        List<String> transitionLines = petrifyOutput.stream().filter(line -> !line.startsWith(".")).toList();
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
        Set<Place> nextPlaces = currentPlaces.stream().collect(Collectors.toSet());

        // For each current place, the connected transitions are collected, and fired if allowed.
        for (Place currentPlace: currentPlaces) {
            Set<Transition> transitions = currentPlace.getOutArcs().stream().map(arc -> arc.getTarget())
                    .map(Transition.class::cast).filter(t -> t.getName().getText().equals(transitionLabel))
                    .collect(Collectors.toSet());

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
