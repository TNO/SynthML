
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    private static final PtnetFactory PETRINETFACTORY = PtnetFactory.eINSTANCE;

    public static Page transformPetriNetOutput(List<String> petrifyOutput) {
        Preconditions.checkArgument(!petrifyOutput.stream().anyMatch(line -> line.contains("FinalPlace")),
                "Expected that the Petri Net output does not contain string 'FinalPlace' as this string is used as the identifier of the final place.");

        Preconditions.checkArgument(!petrifyOutput.stream().anyMatch(line -> line.contains("__")),
                "Expected that the Petri Net output does not contain double underscores as they are used in the name of arcs.");

        // Skip all comments.
        while (petrifyOutput.get(0).startsWith("#")) {
            petrifyOutput.remove(0);
        }

        // Obtain model name.
        String modelNameHeader = ".model";
        Preconditions.checkArgument(petrifyOutput.get(0).startsWith(modelNameHeader),
                "Expected the Petri Net output to have a model name.");
        String modelName = petrifyOutput.get(0).substring(modelNameHeader.length()).trim();
        petrifyOutput.remove(0);

        // Create a Petri Net page.
        Page petriNetPage = initializePetriNetPage(modelName);

        // Obtain list of transitions.
        String dummyIdentifier = ".dummy";
        Preconditions.checkArgument(petrifyOutput.get(0).startsWith(dummyIdentifier),
                "Expected the Petri Net output to contain transition declarations.");
        String transitionDeclaration = petrifyOutput.get(0).substring(dummyIdentifier.length()).trim();
        petrifyOutput.remove(0);

        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclaration.split(" ")));
        Preconditions.checkArgument(transitionNames.size() == transitionNames.stream().distinct().count(),
                "Expected the transition name to be unique.");

        // Create transitions and add them to the map that stores name of places and places.
        Map<String, Node> transitionsPlacesMap = new HashMap<>();
        transitionNames.forEach(t -> transitionsPlacesMap.put(t, createTransition(t, petriNetPage)));

        // In case a transition appears multiple times in a Petri Net. Petrify distinguishes each duplication by
        // adding a postfix to the name of the transition (e.g., Transition_A/1 is a duplication of Transition_A), and
        // these duplications are not specified in the transition declarations, but only appear in the specification.
        // Therefore, transition duplications in the specification should be collected.

        // Iterate over each specification line to create places, duplicate transitions and arcs.
        String specificationIdentifier = ".graph";
        Preconditions.checkArgument(petrifyOutput.get(0).startsWith(specificationIdentifier),
                "Expected the Petri Net output to contain specification.");
        petrifyOutput.remove(0);
        while (!petrifyOutput.get(0).startsWith(".marking")) {
            List<String> elements = Arrays.asList(petrifyOutput.get(0).split(" "));

            // Create new places and duplicate transitions if they have not been created. Store the names and objects in
            // map.
            for (String element: elements) {
                if (!transitionsPlacesMap.containsKey(element)) {
                    if (isDuplicateTransition(element, transitionsPlacesMap)) {
                        transitionsPlacesMap.put(element, createDuplicateTransition(element, petriNetPage));
                    } else {
                        transitionsPlacesMap.put(element, createPlace(element, petriNetPage));
                    }
                }
            }

            // Create arcs from the source to its targets. In case the source is the 'end' transition, a final place is
            // created and connected to the 'end' transition.
            String source = elements.get(0);
            if (source.equals("end")) {
                String finalPlace = "FinalPlace";
                createArc(transitionsPlacesMap.get(source), createPlace(finalPlace, petriNetPage), petriNetPage);
            } else {
                elements.stream().skip(1).forEach((target) -> createArc(transitionsPlacesMap.get(source),
                        transitionsPlacesMap.get(target), petriNetPage));
            }
            petrifyOutput.remove(0);
        }

        // Obtain the marking place in curly brackets.
        String markingIdentifier = ".marking";
        Preconditions.checkArgument(petrifyOutput.get(0).startsWith(markingIdentifier),
                "Expected the Petri Net output to contain a marking place.");
        String markingPlaceName = petrifyOutput.get(0).replace(markingIdentifier, "").replace("{", "").replace("}", "")
                .trim();
        Place markingPlace = (Place)transitionsPlacesMap.get(markingPlaceName);

        // Create marking for the marking place.
        PTMarking initialMarking = PETRINETFACTORY.createPTMarking();
        initialMarking.setText((long)1);
        initialMarking.setContainerPlace(markingPlace);
        markingPlace.setInitialMarking(initialMarking);

        return petriNetPage;
    }

    private static Page initializePetriNetPage(String petriNetId) {
        // Create Petri Net doc.
        PetriNetDoc petriNetDoc = PETRINETFACTORY.createPetriNetDoc();

        // Create Petri Net.
        PetriNet petriNet = PETRINETFACTORY.createPetriNet();
        petriNet.setId(petriNetId);
        petriNet.setContainerPetriNetDoc(petriNetDoc);

        // Create page in Petri Net.
        Page page = PETRINETFACTORY.createPage();
        page.setContainerPetriNet(petriNet);
        page.setId(petriNetId);

        return page;
    }

    private static Transition createTransition(String name, Page page) {
        Transition transition = PETRINETFACTORY.createTransition();
        transition.setId(name);
        transition.setName(createName(name));
        transition.setContainerPage(page);
        return transition;
    }

    private static Name createName(String name) {
        Name nameObject = PETRINETFACTORY.createName();
        nameObject.setText(name);
        return nameObject;
    }

    private static boolean isDuplicateTransition(String elementName, Map<String, Node> nameObjectMapping) {
        // Since CIF does not accept '/', the generated state space cannot contain '/'. It is safe to use '/' to
        // identify duplicate transitions.
        for (String declaredName: nameObjectMapping.keySet()) {
            if (elementName.startsWith(declaredName) && elementName.contains("/")) {
                return true;
            }
        }
        return false;
    }

    private static Transition createDuplicateTransition(String name, Page page) {
        Transition transition = PETRINETFACTORY.createTransition();
        transition.setId(name);
        transition.setName(createName(name.split("/")[0]));
        transition.setContainerPage(page);
        return transition;
    }

    private static Place createPlace(String name, Page page) {
        Place place = PETRINETFACTORY.createPlace();
        place.setId(name);
        place.setName(createName(name));
        place.setContainerPage(page);
        return place;
    }

    private static Arc createArc(Node source, Node target, Page page) {
        Arc arc = PETRINETFACTORY.createArc();
        arc.setContainerPage(page);
        arc.setId(source.getId() + "__to__" + target.getId());
        arc.setSource(source);
        arc.setTarget(target);
        source.getOutArcs().add(arc);
        return arc;
    }
}
