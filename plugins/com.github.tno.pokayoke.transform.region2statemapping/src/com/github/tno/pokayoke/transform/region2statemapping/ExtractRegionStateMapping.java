
package com.github.tno.pokayoke.transform.region2statemapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Triple;
import org.json.JSONObject;

import com.github.tno.pokayoke.transform.petrify2uml.FileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.Petrify2PNMLTranslator;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

public class ExtractRegionStateMapping {
    private ExtractRegionStateMapping() {
    }

    /**
     * Extracts region-state map from provided Petrify input and output files and writes the map into a JSON file.
     *
     * @param petrifyInputPath Petrify input file path.
     * @param petrifyOutputPath Petrify output file path.
     * @param outputPath Output JSON file to which to write the map.
     * @throws IOException In case generating the output JSON failed.
     */
    public static void extractMappingFromFiles(String petrifyOutputPath, String petrifyInputPath, String outputPath)
            throws IOException
    {
        List<String> petrifyInput = FileHelper.readFile(petrifyInputPath);
        List<String> petrifyOutput = FileHelper.readFile(petrifyOutputPath);
        PetriNet petriNet = Petrify2PNMLTranslator.transform(petrifyOutput, false);
        Map<String, Set<String>> map = extract(petrifyInput, petriNet);
        Files.writeString(Paths.get(outputPath), new JSONObject(map).toString());
    }

    /**
     * Extracts a region-state map from the given Petri net and state machine.
     *
     * @param petrifyInput The Petrify input that contains a state machine.
     * @param petriNet The Petri net.
     * @return A map from state in the state machine to region (i.e., a set of places) in the Petri net.
     */
    public static Map<String, Set<String>> extract(List<String> petrifyInput, PetriNet petriNet) {
        // Initialize an empty map from place to states.
        List<Place> places = petriNet.getPages().get(0).getObjects().stream().filter(Place.class::isInstance)
                .map(Place.class::cast).toList();
        Map<String, Set<String>> regionStateMap = new LinkedHashMap<>();
        places.stream().forEach(place -> regionStateMap.put(place.getName().getText(), new LinkedHashSet<>()));

        // Initialize a queue that stores pairs of state and corresponding places to be visited. The first pair is the
        // initial state and the initial marked place.
        List<Place> markedPlace = places.stream().filter(place -> place.getInitialMarking() != null).toList();
        String markingIdentifier = ".marking";
        List<String> initialStates = petrifyInput.stream().filter(line -> line.startsWith(markingIdentifier)).toList();
        Verify.verify(initialStates.size() == 1, "Expected that the state machine has only one initial state.");
        String initialState = initialStates.get(0).substring(markingIdentifier.length()).replace("{", "")
                .replace("}", "").trim();
        Queue<Pair<String, Set<Place>>> queue = new LinkedList<>();
        queue.add(Pair.of(initialState, new LinkedHashSet<>(markedPlace)));

        // Initialize the visited pairs of state and places.
        Set<Pair<String, Set<Place>>> visited = new LinkedHashSet<>();

        // Get transitions of the state machine.
        List<Triple<String, String, String>> transitions = getTransitions(petrifyInput);

        // Co-simulate the state machine and Petri net while the queue is not empty.
        while (!queue.isEmpty()) {
            Pair<String, Set<Place>> statePlacesPair = queue.poll();

            // Process the pair when it has not been visited yet.
            if (!visited.contains(statePlacesPair)) {
                visited.add(statePlacesPair);

                // Get the current state and marked places.
                String currentState = statePlacesPair.getLeft();
                Set<Place> markedPlaces = statePlacesPair.getRight();

                // Update the region-state map with current state for each marked place.
                markedPlaces.stream().forEach(place -> regionStateMap.get(place.getName().getText()).add(currentState));

                // Get the transitions to be fired.
                List<Triple<String, String, String>> transitionsToFire = transitions.stream()
                        .filter(transition -> transition.first.equals(currentState)).toList();

                // Get the firing result of each transition and push the new pair of state and places into the queue.
                for (Triple<String, String, String> transition: transitionsToFire) {
                    String transitionLabel = transition.second;
                    String newState = transition.third;
                    Set<Place> newMarkedPlaces = fire(transitionLabel, markedPlaces);
                    queue.add(Pair.of(newState, newMarkedPlaces));
                }
            }
        }

        return regionStateMap;
    }

    private static List<Triple<String, String, String>> getTransitions(List<String> petrifyInput) {
        List<Triple<String, String, String>> transitions = new ArrayList<>();

        // Get transition lines from the Petrify input. All the lines that do not start with '.' are the transition
        // lines.
        List<String> transitionLines = petrifyInput.stream().filter(line -> !line.startsWith(".")).toList();
        for (String transition: transitionLines) {
            String[] elements = transition.split(" ");
            Assert.check(elements.length == 3, "The transition line should contains exactly three elements.");
            String sourceState = elements[0];
            String transitionLabel = elements[1];
            String targetState = elements[2];
            transitions.add(new Triple<>(sourceState, transitionLabel, targetState));
        }
        return transitions;
    }

    private static Set<Place> fire(String transitionLabel, Set<Place> markedPlaces) {
        // Obtain all fireable Petri net transitions with the specified label.
        Set<Transition> transitions = markedPlaces.stream().map(place -> place.getOutArcs())
                .flatMap(arcs -> arcs.stream()).map(arc -> arc.getTarget()).map(Transition.class::cast)
                .filter(t -> t.getName().getText().equals(transitionLabel))
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

        // Ensure there is exactly one such transition.
        Preconditions.checkArgument(transitions.size() == 1,
                String.format("Expected that there is only one transition with label %s.", transitionLabel));

        // Fire this single transition.
        return fire(transitions.stream().collect(Collectors.toList()).get(0), markedPlaces);
    }

    private static Set<Place> fire(Transition transition, Set<Place> markedPlaces) {
        // Obtain all source and target places of the transition.
        Set<Place> sourcePlaces = transition.getInArcs().stream().map(arc -> arc.getSource()).map(Place.class::cast)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
        Set<Place> targetPlaces = transition.getOutArcs().stream().map(arc -> arc.getTarget()).map(Place.class::cast)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

        // Ensure the transition can actually fire.
        Preconditions.checkArgument(markedPlaces.containsAll(sourcePlaces));
        Preconditions
                .checkArgument(Sets.intersection(markedPlaces, Sets.difference(targetPlaces, sourcePlaces)).isEmpty());

        // Determine the new set of marked places.
        Set<Place> newMarkedPlaces = new LinkedHashSet<>(markedPlaces);
        newMarkedPlaces.removeAll(sourcePlaces);
        newMarkedPlaces.addAll(targetPlaces);

        return newMarkedPlaces;
    }
}
