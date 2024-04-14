
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/** Normalize Petrify output by relabeling the places. */
public class NormalizePetrifyOutput {
    private NormalizePetrifyOutput() {
    }

    /**
     * Normalize the generated Petrify output by relabeling the places.
     *
     * @param petrifyOutput The Petrify ouput.
     */
    public static void normalize(List<String> petrifyOutput) {
        // Collect transition names.
        String dummyIdentifier = ".dummy";
        List<String> transitionDeclarations = petrifyOutput.stream().filter(line -> line.startsWith(dummyIdentifier))
                .toList();
        Preconditions.checkArgument(transitionDeclarations.size() == 1,
                "Expected the Petrify output to contain exactly one line of transition declaration.");
        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclarations.get(0).split(" ")));

        // Collect specification lines.
        List<String> specificationLines = petrifyOutput.stream()
                .filter(line -> !line.startsWith(".") && !line.startsWith("#")).toList();

        // Add duplicate transitions to the list, and get a map from parent node to child node.
        Map<String, List<String>> parentToChild = new HashMap<>();
        for (String currentLine: specificationLines) {
            // Split the specification line into nodes.
            List<String> nodes = Arrays.asList(currentLine.split(" "));
            String parentNode = nodes.get(0);
            List<String> childNodes = nodes.subList(1, nodes.size());

            // Add the duplicate transitions.
            nodes.stream()
                    .filter(element -> !transitionNames.contains(element)
                            && Petrify2PNMLTranslator.isDuplicateTransition(element, Sets.newHashSet(transitionNames)))
                    .forEach(element -> transitionNames.add(element));

            parentToChild.put(parentNode, childNodes);
        }

        // Sort the transition names.
        Collections.sort(transitionNames);

        // Perform breadth-first search and rename places based on the parent-to-child relation.
        Map<String, String> oldToNewPlaceNames = new LinkedHashMap<>();
        String startNode = "start";
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(startNode);
        int placeCount = 0;

        while (!queue.isEmpty()) {
            String currentNode = queue.poll();
            visited.add(currentNode);
            List<String> childNodes = parentToChild.get(currentNode);

            // If the current node is a transition, rename the target places of its out arcs.
            if (transitionNames.contains(currentNode)) {
                Map<String, Integer> placeToIndex = new LinkedHashMap<>();

                // Get a map from place to the smallest index of the outgoing transitions.
                for (String place: childNodes) {
                    // Only the places that have not been replaced are considered.
                    if (!oldToNewPlaceNames.containsKey(place)) {
                        List<String> transitions = parentToChild.get(place);
                        List<Integer> transitionsIndex = transitions.stream()
                                .map(transition -> transitionNames.indexOf(transition)).toList();
                        placeToIndex.put(place, Collections.min(transitionsIndex));
                    }
                }

                // Sort the places based on the index.
                List<Map.Entry<String, Integer>> sortedPlaceToIndex = new ArrayList<>(placeToIndex.entrySet());
                sortedPlaceToIndex.sort(Map.Entry.comparingByValue());
                List<String> sortedPlaces = sortedPlaceToIndex.stream().map(entry -> entry.getKey()).toList();

                // Rename the places based on the order.
                for (String place: sortedPlaces) {
                    String placeID = "p" + String.valueOf(placeCount);
                    oldToNewPlaceNames.put(place, placeID);
                    placeCount = placeCount + 1;
                }

                childNodes = sortedPlaces;
            }

            // Enqueue the child nodes.
            for (String childNode: childNodes) {
                if (!visited.contains(childNode)) {
                    queue.add(childNode);
                }
            }
        }

        // Construct new specification line by replacing old place names with new place names.
        List<String> newSpecificationLines = new ArrayList<>();
        for (String currentLine: specificationLines) {
            List<String> nodes = Arrays.asList(currentLine.split(" "));
            List<String> newNodes = new ArrayList<>();
            for (String node: nodes) {
                if (oldToNewPlaceNames.containsKey(node)) {
                    newNodes.add(oldToNewPlaceNames.get(node));
                } else {
                    newNodes.add(node);
                }
            }

            // Sort the child nodes to make sure the order is deterministic.
            Collections.sort(newNodes.subList(1, newNodes.size()));

            newSpecificationLines.add(String.join(" ", newNodes));
        }

        // Sort the new specification lines to make sure the order is deterministic.
        Collections.sort(newSpecificationLines);

        // Get the index of the start index and end index of the specification.
        int startIndex = petrifyOutput.indexOf(specificationLines.get(0));
        int endIndex = startIndex + specificationLines.size();

        // Replace the old specification with the new specification.
        petrifyOutput.subList(startIndex, endIndex).clear();
        petrifyOutput.addAll(startIndex, newSpecificationLines);

        // Get the marking place.
        String markingIdentifier = ".marking";
        List<String> markingLines = petrifyOutput.stream().filter(line -> line.startsWith(markingIdentifier)).toList();
        Preconditions.checkArgument(markingLines.size() == 1,
                "Expected the Petrify output to contain exactly one marking line.");
        String markingLine = markingLines.get(0);
        String markingPlaceName = markingLines.get(0).substring(markingIdentifier.length()).replace("{", "")
                .replace("}", "").trim();

        // Replace the marking place name in the marking line.
        String newMarkingPlaceName = oldToNewPlaceNames.get(markingPlaceName);
        String newMarkingLine = markingLine.replace(markingPlaceName, newMarkingPlaceName);
        petrifyOutput.set(petrifyOutput.indexOf(markingLine), newMarkingLine);
    }
}
