
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Name;
import fr.lip6.move.pnml.ptnet.Node;
import fr.lip6.move.pnml.ptnet.PTMarking;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.PetriNetDoc;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PtnetFactory;
import fr.lip6.move.pnml.ptnet.Transition;

/** Translates Petrify output to PNML. */
public class Petrify2PNMLTranslator {
    private Petrify2PNMLTranslator() {
    }

    private static final PtnetFactory PETRI_NET_FACTORY = PtnetFactory.eINSTANCE;

    public static void transformFile(String inputPath, String outputPath) throws IOException {
        List<String> input = FileHelper.readFile(inputPath);
        PetriNet petriNet = transform(input, true);
        FileHelper.writePetriNet(petriNet, outputPath);
    }

    /**
     * Transforms Petrify output to PNML.
     *
     * @param petrifyOutput Petrify output in a list of strings. A {@link LinkedList} should be provided, as otherwise
     *     removing elements from the head of the list is too expensive.
     * @param removeLoop The removal of loop is enabled when it is {@code true}, otherwise, disabled.
     * @return The Petri Net.
     */
    public static PetriNet transform(List<String> petrifyOutput, boolean removeLoop) {
        Preconditions.checkArgument(!petrifyOutput.stream().anyMatch(line -> line.contains("FinalPlace")),
                "Expected that the Petrify output does not contain string 'FinalPlace' as this string is going to be used as the identifier of the final place");

        Preconditions.checkArgument(!petrifyOutput.stream().anyMatch(line -> line.contains("__to__")),
                "Expected that the Petrify output does not contain '__to__' as they are going to be used in the name of arcs.");

        // Skip all comments.
        String currentLine = petrifyOutput.get(0);
        while (currentLine.startsWith("#")) {
            petrifyOutput.remove(0);
            currentLine = petrifyOutput.get(0);
        }

        // Obtain model name.
        String modelNameHeader = ".model";
        Preconditions.checkArgument(currentLine.startsWith(modelNameHeader),
                "Expected the Petrify output to have a model name.");
        String modelName = currentLine.substring(modelNameHeader.length()).trim();
        petrifyOutput.remove(0);

        // Create a Petri Net page.
        Page petriNetPage = initializePetriNetPage(modelName);

        // Obtain list of transitions.
        String dummyIdentifier = ".dummy";
        currentLine = petrifyOutput.get(0);
        Preconditions.checkArgument(currentLine.startsWith(dummyIdentifier),
                "Expected the Petrify output to contain transition declarations.");
        String transitionDeclaration = currentLine.substring(dummyIdentifier.length()).trim();
        petrifyOutput.remove(0);

        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclaration.split(" ")));
        Preconditions.checkArgument(transitionNames.size() == transitionNames.stream().distinct().count(),
                "Expected transition names to be unique.");

        // Create transitions and add them to the map that stores objects and their names.
        Map<String, Node> transitionsPlacesMap = new HashMap<>();
        transitionNames.forEach(t -> transitionsPlacesMap.put(t, createTransition(t, petriNetPage)));

        // In case a transition appears multiple times in a Petri Net, Petrify distinguishes each duplicate by
        // adding a postfix to the name of the transition (e.g., 'Transition_A/1' is a duplicate of 'Transition_A'),
        // and these duplicates are not specified in the transition declarations, but only appear in the
        // specification. Therefore, transition duplicates in the specification are collected and separate transition
        // objects are created with same name but different IDs. For example, duplicate 'Transition_A/1' is named
        // as 'Transition_A' but with 'Transition_A/1' as its ID.

        // Iterate over each specification line to create places, duplicate transitions and arcs.
        String specificationIdentifier = ".graph";
        currentLine = petrifyOutput.get(0);
        Preconditions.checkArgument(currentLine.startsWith(specificationIdentifier),
                "Expected the Petrify output to contain a specification.");
        petrifyOutput.remove(0);

        currentLine = petrifyOutput.get(0);
        while (!currentLine.startsWith(".marking")) {
            List<String> elements = Arrays.asList(currentLine.split(" "));

            // Create new places and duplicate transitions if they have not been created. Store the names and the
            // corresponding objects in the map.
            for (String element: elements) {
                if (!transitionsPlacesMap.containsKey(element)) {
                    if (isDuplicateTransition(element, transitionsPlacesMap)) {
                        transitionsPlacesMap.put(element, createDuplicateTransition(element, petriNetPage));
                    } else {
                        transitionsPlacesMap.put(element, createPlace(element, petriNetPage));
                    }
                }
            }

            // Create arcs from the source to its targets. In case the source is the 'end' transition and the
            // 'removeLoop' is enabled, a final place is
            // created and connected to the 'end' transition.
            String source = elements.get(0);
            if (source.equals("end") && removeLoop) {
                String finalPlace = "FinalPlace";
                createArc(transitionsPlacesMap.get(source), createPlace(finalPlace, petriNetPage), petriNetPage);
            } else {
                elements.stream().skip(1).forEach(target -> createArc(transitionsPlacesMap.get(source),
                        transitionsPlacesMap.get(target), petriNetPage));
            }
            petrifyOutput.remove(0);
            currentLine = petrifyOutput.get(0);
        }

        // Obtain the marking place in curly brackets.
        String markingIdentifier = ".marking";
        Preconditions.checkArgument(currentLine.startsWith(markingIdentifier),
                "Expected the Petrify output to contain a marking place.");
        String markingPlaceName = currentLine.substring(markingIdentifier.length()).replace("{", "").replace("}", "")
                .trim();

        Place markingPlace = (Place)transitionsPlacesMap.get(markingPlaceName);

        // Create a marking for the marking place.
        PTMarking initialMarking = PETRI_NET_FACTORY.createPTMarking();
        initialMarking.setText(1L);
        initialMarking.setContainerPlace(markingPlace);
        markingPlace.setInitialMarking(initialMarking);

        return petriNetPage.getContainerPetriNet();
    }

    private static Page initializePetriNetPage(String petriNetId) {
        // Create Petri Net doc.
        PetriNetDoc petriNetDoc = PETRI_NET_FACTORY.createPetriNetDoc();

        // Create Petri Net.
        PetriNet petriNet = PETRI_NET_FACTORY.createPetriNet();
        petriNet.setId(petriNetId);
        petriNet.setContainerPetriNetDoc(petriNetDoc);

        // Create page in Petri Net.
        Page page = PETRI_NET_FACTORY.createPage();
        page.setContainerPetriNet(petriNet);
        page.setId(petriNetId);

        return page;
    }

    private static Transition createTransition(String name, Page page) {
        Transition transition = PETRI_NET_FACTORY.createTransition();
        transition.setId(name);
        transition.setName(createName(name));
        transition.setContainerPage(page);
        return transition;
    }

    private static Name createName(String name) {
        Name nameObject = PETRI_NET_FACTORY.createName();
        nameObject.setText(name);
        return nameObject;
    }

    private static boolean isDuplicateTransition(String elementName, Map<String, Node> nameObjectMapping) {
        // Since CIF does not accept '/' in identifiers, the generated state space cannot contain '/'. It is safe to use
        // '/' to identify duplicate transitions.
        for (String declaredName: nameObjectMapping.keySet()) {
            if (elementName.startsWith(declaredName) && elementName.contains("/")) {
                return true;
            }
        }
        return false;
    }

    private static Transition createDuplicateTransition(String name, Page page) {
        Transition transition = PETRI_NET_FACTORY.createTransition();
        transition.setId(name);
        transition.setName(createName(name.split("/")[0]));
        transition.setContainerPage(page);
        return transition;
    }

    private static Place createPlace(String name, Page page) {
        Place place = PETRI_NET_FACTORY.createPlace();
        place.setId(name);
        place.setName(createName(name));
        place.setContainerPage(page);
        return place;
    }

    private static Arc createArc(Node source, Node target, Page page) {
        Arc arc = PETRI_NET_FACTORY.createArc();
        arc.setContainerPage(page);
        arc.setId(source.getId() + "__to__" + target.getId());
        arc.setSource(source);
        arc.setTarget(target);
        source.getOutArcs().add(arc);
        return arc;
    }
}
