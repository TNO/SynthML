
package com.github.tno.pokayoke.transform.petrify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;

/** Normalize Petrify output by relabeling the places. */
public class NormalizePetrifyOutput {
    private NormalizePetrifyOutput() {
    }

    /**
     * Normalize the specified Petrify output file.
     *
     * @param petrifyOutputPath The path to the Petrify output file.
     * @throws IOException In case the Petrify output file could not be read or written to.
     */
    public static void normalize(Path petrifyOutputPath) throws IOException {
        List<String> petrifyOutput = PetrifyHelper.readFile(petrifyOutputPath.toString());
        Files.write(petrifyOutputPath, NormalizePetrifyOutput.normalize(petrifyOutput));
    }

    /**
     * Normalize the given Petrify output by relabeling the places.
     *
     * @param petrifyOutput The Petrify ouput.
     * @return The normalized Petrify output.
     */
    public static List<String> normalize(List<String> petrifyOutput) {
        // Collect transition names.
        String dummyIdentifier = ".dummy";
        List<String> transitionDeclarations = petrifyOutput.stream().filter(line -> line.startsWith(dummyIdentifier))
                .toList();
        Preconditions.checkArgument(transitionDeclarations.size() == 1,
                "Expected the Petrify output to contain exactly one transition declaration.");
        String transitionDeclaration = transitionDeclarations.get(0).substring(dummyIdentifier.length()).trim();
        Set<String> declaredTransitionNames = Sets.newHashSet(transitionDeclaration.split(" "));
        Set<String> allTransitionNames = new LinkedHashSet<>(declaredTransitionNames);

        // Collect specification lines.
        List<String> specificationLines = petrifyOutput.stream()
                .filter(line -> !line.startsWith(".") && !line.startsWith("#")).toList();

        // Collect duplicate transitions, and make a map from nodes to all nodes they can reach in one forward step.
        Map<String, List<String>> nextNodes = new HashMap<>();
        for (String currentLine: specificationLines) {
            // Split the specification line into nodes.
            List<String> nodes = Arrays.asList(currentLine.split(" "));
            String parentNode = nodes.get(0);
            List<String> childNodes = nodes.subList(1, nodes.size());

            // Add the duplicate transitions.
            nodes.stream()
                    .filter(element -> !declaredTransitionNames.contains(element)
                            && PetrifyHelper.isDuplicateTransition(element, declaredTransitionNames))
                    .forEach(element -> allTransitionNames.add(element));

            nextNodes.put(parentNode, childNodes);
        }

        // From 'nextNodes' also construct a mapping from nodes to all nodes they can reach in one backward step.
        Map<String, List<String>> prevNodes = new HashMap<>();

        for (Entry<String, List<String>> entry: nextNodes.entrySet()) {
            String source = entry.getKey();

            for (String target: entry.getValue()) {
                prevNodes.computeIfAbsent(target, t -> new ArrayList<>()).add(source);
            }
        }

        // Sort the transition names.
        List<String> sortedTransitionNames = allTransitionNames.stream().sorted().toList();
        Map<String, Integer> transitionIndices = IntStream.range(0, sortedTransitionNames.size()).boxed()
                .collect(Collectors.toMap(sortedTransitionNames::get, i -> i));

        // Get the marking place.
        String markingIdentifier = ".marking";
        List<String> markingLines = petrifyOutput.stream().filter(line -> line.startsWith(markingIdentifier)).toList();
        Preconditions.checkArgument(markingLines.size() == 1,
                "Expected the Petrify output to contain exactly one marking line.");
        String markingLine = markingLines.get(0);
        String markingPlaceName = markingLines.get(0).substring(markingIdentifier.length()).replace("{", "")
                .replace("}", "").trim();
        Preconditions.checkArgument(markingPlaceName.split(" ").length == 1,
                "Expected that there is only one marking place.");

        // Perform breadth-first search and rename places based on the parent-to-child relation.
        Map<String, String> oldToNewPlaceNames = new LinkedHashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> found = new HashSet<>();
        queue.add(markingPlaceName);
        found.add(markingPlaceName);
        int nextPlaceNr = 1;

        while (!queue.isEmpty()) {
            String currentPlace = queue.poll();
            List<String> childTransitions = nextNodes.get(currentPlace);
            Collections.sort(childTransitions);

            // Rename the place that has not been renamed.
            String placeID = "p" + String.valueOf(nextPlaceNr);
            String result = oldToNewPlaceNames.put(currentPlace, placeID);
            Preconditions.checkArgument(result == null,
                    String.format("Expected that place %s has not been renamed yet.", currentPlace));
            nextPlaceNr++;

            for (String childTransition: childTransitions) {
                List<String> places = nextNodes.get(childTransition);

                // Recall that earlier we assigned a unique index to every transition, resulting in 'transitionIndices'.
                // Now, for every place we find the indices of all its incoming transitions, as well as the indices of
                // all its outgoing transitions.
                Map<String, Set<Integer>> placeToNextIndices = new LinkedHashMap<>();
                Map<String, Set<Integer>> placeToPrevIndices = new LinkedHashMap<>();

                for (String place: places) {
                    Set<Integer> prevIndices = prevNodes.get(place).stream().map(transitionIndices::get)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    Set<Integer> nextIndices = nextNodes.get(place).stream().map(transitionIndices::get)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    placeToPrevIndices.put(place, prevIndices);
                    placeToNextIndices.put(place, nextIndices);
                }

                // Sort the places based on the lowest non-shared indices found above.
                Comparator<String> comparator = compareToLowestNonShared(placeToNextIndices)
                        .thenComparing(compareToLowestNonShared(placeToPrevIndices));
                List<String> sortedPlaces = places.stream().sorted(comparator).toList();

                // Make sure that there is no ambiguity in the order of places to consider next.
                for (int i = 0; i < sortedPlaces.size() - 1; i++) {
                    String current = sortedPlaces.get(i);
                    String next = sortedPlaces.get(i + 1);

                    Verify.verify(comparator.compare(current, next) != 0,
                            String.format("Could not determine the order of the places '%s' and '%s'.", current, next));
                }

                // Enqueue the places.
                for (String place: sortedPlaces) {
                    if (!found.contains(place)) {
                        queue.add(place);
                        found.add(place);
                    }
                }
            }
        }

        // Construct new specification line by replacing old place names with new place names.
        List<String> newSpecificationLines = new ArrayList<>();
        for (String currentLine: specificationLines) {
            List<String> nodes = Arrays.asList(currentLine.split(" "));
            List<String> newNodes = nodes.stream().map(node -> oldToNewPlaceNames.getOrDefault(node, node))
                    .collect(Collectors.toList());

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

        // Replace the marking place name in the marking line.
        String newMarkingPlaceName = oldToNewPlaceNames.get(markingPlaceName);
        Preconditions.checkNotNull(newMarkingPlaceName, "Expected that the marking place is renamed.");
        String newMarkingLine = markingLine.replace(markingPlaceName, newMarkingPlaceName);
        petrifyOutput.set(petrifyOutput.indexOf(markingLine), newMarkingLine);

        // Remove the specification unrelated lines. These lines are automatically generated by Petrify to show the
        // absolute path of the file and the time stamp at which it was generated. They contain non-deterministic
        // information and should be removed.
        List<String> normalizedPetrifyOutput = petrifyOutput.stream().filter(line -> !line.startsWith("#")).toList();

        return normalizedPetrifyOutput;
    }

    /**
     * Gives a comparator for comparing places based on their lowest non-shared index as specified by the given mapping.
     *
     * @param map The mapping from nodes to indices.
     * @return A comparator for comparing places based on their lowest non-shared index.
     */
    private static Comparator<String> compareToLowestNonShared(Map<String, Set<Integer>> map) {
        return (left, right) -> {
            Set<Integer> leftSet = map.get(left);
            Set<Integer> rightSet = map.get(right);

            // If both elements have the same indices, then they are considered equal with respect to this comparison.
            if (leftSet.equals(rightSet)) {
                return 0;
            }

            // Otherwise, we find the lowest non-shared index, i.e., the lowest number in the symmetric difference of
            // the two sets of indices.
            int min = Collections.min(Sets.symmetricDifference(leftSet, rightSet));

            // If the left set contains this number, then 'left' is considered smaller than 'right', or else the right
            // set must contain this lowest number, in which case 'left' is considered larger than 'right'.
            return leftSet.contains(min) ? -1 : 1;
        };
    }
}
