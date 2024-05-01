
package com.github.tno.pokayoke.transform.region2statemapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.escet.common.java.Triple;
import org.json.JSONObject;

import com.github.tno.pokayoke.transform.petrify2uml.PetriNetUMLFileHelper;
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
     * Extracts region-state map from provided Petrify input and output files and writes the map as a JSON file.
     *
     * @param petrifyInputPath Petrify input file path.
     * @param petrifyOutputPath Petrify output file path.
     * @param outputFolderPath Output folder path JSON file under which to write the map.
     * @throws IOException In case generating the output JSON failed.
     */
    public static void extractMappingFromFiles(Path petrifyInputPath, Path petrifyOutputPath, Path outputFolderPath)
            throws IOException
    {
        List<String> petrifyInput = PetriNetUMLFileHelper.readFile(petrifyInputPath.toString());
        List<String> petrifyOutput = PetriNetUMLFileHelper.readFile(petrifyOutputPath.toString());
        PetriNet petriNet = Petrify2PNMLTranslator.transform(petrifyOutput);
        Map<Place, Set<String>> regionMapping1 = extract(petrifyInput, petriNet);
        Map<String, Set<String>> regionMapping2 = new LinkedHashMap<>();
        regionMapping1.entrySet()
                .forEach(entry -> regionMapping2.put(entry.getKey().getName().getText(), entry.getValue()));

        String filePrefix = FilenameUtils.removeExtension(petrifyInputPath.getFileName().toString());
        Path jsonOutputPath = outputFolderPath.resolve(filePrefix + ".json");
        Files.createDirectories(outputFolderPath);
        Files.writeString(jsonOutputPath, new JSONObject(regionMapping2).toString());
    }

    /**
     * Extracts a region-state map from a given state machine and Petri net that is synthesized for it.
     *
     * @param petrifyInput The Petrify input that contains a state machine.
     * @param petriNet The Petri net that is synthesized by Petrify from the input state machine.
     * @return A map from places in the Petri net to the corresponding states that are in the region of the place.
     */
    public static Map<Place, Set<String>> extract(List<String> petrifyInput, PetriNet petriNet) {
        // Initialize an empty map from places to states.
        List<Place> places = petriNet.getPages().get(0).getObjects().stream().filter(Place.class::isInstance)
                .map(Place.class::cast).toList();
        Map<Place, Set<String>> regionStateMap = new LinkedHashMap<>();
        places.stream().forEach(place -> regionStateMap.put(place, new LinkedHashSet<>()));

        // Initialize a queue that stores pairs of state and corresponding places to be visited. The first pair is the
        // initial state and the initial marked place.
        Set<Place> initialMarkedPlace = places.stream().filter(place -> place.getInitialMarking() != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String markingIdentifier = ".marking";
        List<String> initialStateLines = petrifyInput.stream().filter(line -> line.startsWith(markingIdentifier))
                .toList();
        Verify.verify(initialStateLines.size() == 1,
                "Expected the input state machine to have exactly one marking line.");
        String[] initialStates = initialStateLines.get(0).substring(markingIdentifier.length()).replace("{", "")
                .replace("}", "").trim().split(",");
        Verify.verify(initialStates.length == 1, "Expected the input state machine to have exactly one initial state.");
        String initialState = initialStates[0].trim();
        Queue<Pair<String, Set<Place>>> queue = new LinkedList<>();
        queue.add(Pair.of(initialState, initialMarkedPlace));

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
                markedPlaces.stream().forEach(place -> regionStateMap.get(place).add(currentState));

                // Get the transitions to be taken.
                List<Triple<String, String, String>> transitionsToFire = transitions.stream()
                        .filter(transition -> transition.first.equals(currentState)).toList();

                // Get the result of each transition and push the new pair of state and places into the queue.
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
            Verify.verify(elements.length == 3, "The transition line should contains exactly three elements.");
            String sourceState = elements[0];
            String transitionLabel = elements[1];
            String targetState = elements[2];
            transitions.add(new Triple<>(sourceState, transitionLabel, targetState));
        }
        return transitions;
    }

    private static Set<Place> fire(String transitionLabel, Set<Place> markedPlaces) {
        // Obtain all potentially fireable Petri net transitions with the specified label.
        Set<Transition> transitions = markedPlaces.stream().flatMap(place -> place.getOutArcs().stream())
                .map(arc -> arc.getTarget()).map(Transition.class::cast)
                .filter(t -> t.getName().getText().equals(transitionLabel))
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

        // Ensure there is exactly one such transition.
        Preconditions.checkArgument(transitions.size() == 1, String.format(
                "Expected that there is exactly one potentially fireable transition with label %s.", transitionLabel));

        // Fire this single transition.
        return fire(transitions.iterator().next(), markedPlaces);
    }

    private static Set<Place> fire(Transition transition, Set<Place> markedPlaces) {
        // Obtain all source and target places of the transition.
        Set<Place> sourcePlaces = transition.getInArcs().stream().map(arc -> arc.getSource()).map(Place.class::cast)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
        Set<Place> targetPlaces = transition.getOutArcs().stream().map(arc -> arc.getTarget()).map(Place.class::cast)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));

        // Ensure the transition can actually fire.
        Preconditions.checkArgument(markedPlaces.containsAll(sourcePlaces),
                "Expected that all the source places have a token to be fired.");
        Preconditions.checkArgument(
                Sets.intersection(markedPlaces, Sets.difference(targetPlaces, sourcePlaces)).isEmpty(),
                "Expected that target places that are not source places should not have a token.");

        // Determine the new set of marked places.
        Set<Place> newMarkedPlaces = new LinkedHashSet<>(markedPlaces);
        newMarkedPlaces.removeAll(sourcePlaces);
        newMarkedPlaces.addAll(targetPlaces);

        return newMarkedPlaces;
    }
}
