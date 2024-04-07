
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Collect transitions.
        String dummyIdentifier = ".dummy";
        List<String> transitionDeclarations = petrifyOutput.stream().filter(line -> line.startsWith(dummyIdentifier))
                .toList();
        Preconditions.checkArgument(transitionDeclarations.size() == 1,
                "Expected the Petrify output to contain exactly one line of transition declaration.");
        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclarations.get(0).split(" ")));

        List<String> specificationLine = petrifyOutput.stream()
                .filter(line -> !line.startsWith(".") && !line.startsWith("#")).toList();

        List<String> newSpecificationLine = new ArrayList<>();

        Map<String, String> oldToNewPlaceNames = new HashMap<>();

        int placeCount = 0;
        for (String currentLine: specificationLine) {
            List<String> elements = Arrays.asList(currentLine.split(" "));
            List<String> newElements = new ArrayList<>();

            for (String element: elements) {
                String newElement = element;

                // The element is a place if it is neither a declared transition nor a duplicate transition of a
                // declared transition.
                if (!transitionNames.contains(element)
                        && !Petrify2PNMLTranslator.isDuplicateTransition(element, Sets.newHashSet(transitionNames)))
                {
                    if (oldToNewPlaceNames.containsKey(element)) {
                        newElement = oldToNewPlaceNames.get(element);
                    } else {
                        newElement = "p" + String.valueOf(placeCount);
                        placeCount = placeCount + 1;
                        oldToNewPlaceNames.put(element, newElement);
                    }
                }
                newElements.add(newElement);
            }
            newSpecificationLine.add(String.join(" ", newElements));
        }

        // Get the index range of the specification.
        int startIndex = petrifyOutput.indexOf(specificationLine.get(0));
        int endIndex = startIndex + specificationLine.size();

        // Replace the old specification with the new specification.
        petrifyOutput.subList(startIndex, endIndex).clear();
        petrifyOutput.addAll(startIndex, newSpecificationLine);

        // Get the marking line.
        String markingIdentifier = ".marking";
        List<String> markingLines = petrifyOutput.stream().filter(line -> line.startsWith(markingIdentifier)).toList();
        Preconditions.checkArgument(markingLines.size() == 1,
                "Expected the Petrify output to contain exactly one marking line.");
        String markingLine = markingLines.get(0);
        String markingPlaceName = markingLine.substring(markingIdentifier.length()).replace("{", "").replace("}", "")
                .trim();

        // Replace the marking place name in the marking line.
        String newMarkingPlaceName = oldToNewPlaceNames.get(markingPlaceName);
        String newMarkingLine = markingLine.replace(markingPlaceName, newMarkingPlaceName);
        int markingLineIndex = petrifyOutput.indexOf(markingLine);
        petrifyOutput.set(markingLineIndex, newMarkingLine);
    }
}
