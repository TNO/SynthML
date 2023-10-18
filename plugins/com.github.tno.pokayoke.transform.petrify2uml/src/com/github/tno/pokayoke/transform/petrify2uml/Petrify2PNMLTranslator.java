
package com.github.tno.pokayoke.transform.petrify2uml;

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

    private static final PtnetFactory PETRINETFACTORY = PtnetFactory.eINSTANCE;

    public static Page transformPetriNetOutput(LinkedList<String> petrifyOutput) {
        // Skip all comments.
        while (petrifyOutput.element().startsWith("#")) {
            petrifyOutput.remove();
        }

        // Obtain model name.
        String modelMameHeader = ".model";
        Preconditions.checkArgument(petrifyOutput.element().startsWith(modelMameHeader),
                "Expected the Petri Net output to have a model name.");
        String modelName = petrifyOutput.element().replace(modelMameHeader, "").trim();
        petrifyOutput.remove();

        // Create a Petri Net page.
        Page petriNetPage = initializePetriNetPage(modelName);

        // Obtain list of transitions.
        String dummyIdentifier = ".dummy";
        Preconditions.checkArgument(petrifyOutput.element().startsWith(dummyIdentifier),
                "Expected the Petri Net output to contain transition declarations.");
        String transitionDeclaration = petrifyOutput.element().replace(dummyIdentifier, "").trim();
        petrifyOutput.remove();
        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclaration.split(" ")));
        Preconditions.checkArgument(transitionNames.size() == transitionNames.stream().distinct().count(),
                "Expected the transition name to be unique.");

        Map<String, Node> nameObjectMapping = new HashMap<>();

        // Create places and add them to the name map.
        transitionNames.forEach(t -> nameObjectMapping.put(t, createTransition(t, petriNetPage)));

        // Iterate over each specification line to create places, duplicate transitions and arcs.
        String specificationIdentifier = ".graph";
        Preconditions.checkArgument(petrifyOutput.element().startsWith(specificationIdentifier),
                "Expected the Petri Net output to contain specification.");
        petrifyOutput.remove();
        while (!petrifyOutput.element().startsWith(".marking")) {
            List<String> elements = Arrays.asList(petrifyOutput.element().split(" "));

            // Create new places and duplicate transitions if they have not been created.
            for (String element: elements) {
                if (!nameObjectMapping.containsKey(element)) {
                    if (isDuplicateTransition(element, nameObjectMapping)) {
                        nameObjectMapping.put(element, createDuplicateTransition(element, petriNetPage));
                    } else {
                        nameObjectMapping.put(element, createPlace(element, petriNetPage));
                    }
                }
            }

            String source = elements.get(0);

            // Create arcs from the source to its targets. In case the source is the 'end' transition, a final place is
            // created and connected to the 'end' transition.
            if (source.equals("end")) {
                String targetPlace = "FinalPlace";
                createArc(nameObjectMapping.get(source), createPlace(targetPlace, petriNetPage), petriNetPage);
            } else {
                elements.stream().skip(1).forEach((target) -> createArc(nameObjectMapping.get(source),
                        nameObjectMapping.get(target), petriNetPage));
            }
            petrifyOutput.remove();
        }

        // Obtain the marking place in curly brackets.
        String markingIdentifier = ".marking";
        Preconditions.checkArgument(petrifyOutput.element().startsWith(markingIdentifier),
                "Expected the Petri Net output to contain a marking place.");
        String markingPlaceName = petrifyOutput.element().replace(markingIdentifier, "").replace("{", "")
                .replace("}", "").trim();
        Place markingPlace = (Place)nameObjectMapping.get(markingPlaceName);

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

    private static Name createName(String name) {
        Name nameObject = PETRINETFACTORY.createName();
        nameObject.setText(name);
        return nameObject;
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

    private static boolean isDuplicateTransition(String elementName, Map<String, Node> nameObjectMapping) {
        for (String declaredName: nameObjectMapping.keySet()) {
            if (elementName.startsWith(declaredName) && elementName.contains("/")) {
                return true;
            }
        }
        return false;
    }
}
