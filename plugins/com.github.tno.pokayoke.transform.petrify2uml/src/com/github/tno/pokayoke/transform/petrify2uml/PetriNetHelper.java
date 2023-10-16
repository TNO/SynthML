
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

/** Helper for parsing Petrify output. */
public class PetriNetHelper {
    private PetriNetHelper() {
    }

    private static PtnetFactory petriNetFactory = PtnetFactory.eINSTANCE;

    public static Page parsePetriNet(String sourcePath) throws IOException {
        List<String> inputPetriNet = FileHelper.readFile(sourcePath);

        // Obtain model name.
        String nameHeader = ".model";
        Optional<String> modelName = inputPetriNet.stream().filter(line -> line.startsWith(nameHeader))
                .map(line -> line.replace(nameHeader, "").trim()).findFirst();

        Preconditions.checkArgument(modelName.isPresent(),
                "Expected the Petri Net input to have a model name. ");

        // Create a Petri Net page.
        Page petriNetPage = PetriNetHelper.initializePetriNetPage(modelName.get().toString());

        // Remove the header lines.
        List<String> petriNetBody = inputPetriNet.stream().filter(line -> !line.startsWith("#")).toList();

        // Obtain the name of the declared transitions.
        String dummyString = ".dummy";
        Optional<String> transitionDeclaration = petriNetBody.stream().filter(line -> line.startsWith(dummyString))
                .map(line -> line.replace(dummyString, "").trim()).findFirst();
        Preconditions.checkArgument(transitionDeclaration.isPresent(),
                "Expected the Petri Net input to contain transition declarations.");
        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclaration.get().split(" ")));

        // Obtain the specification lines.
        List<String> specificationLines = petriNetBody.stream().filter(line -> !line.startsWith(".")).toList();

        // Obtain the name of all the elements in the specification.
        List<String> transitionPlaceNames = specificationLines.stream().flatMap(line -> Stream.of(line.split(" ")))
                .distinct().toList();

        // In case a transition appears multiple times in a Petri Net. Petrify distinguishes each duplication by adding
        // a postfix to the name of the transition (e.g., Transition_A/1 is a duplication of Transition_A), and these
        // duplications are not specified in the transition declarations, but only appear in the specification.
        // Obtain the name of these duplications.
        List<String> declaredTransitionNames = new ArrayList<>(transitionNames);
        for (String transitionName: declaredTransitionNames) {
            List<String> duplicates = transitionPlaceNames.stream()
                    .filter(e -> !e.equals(transitionName) && e.startsWith(transitionName)).toList();
            transitionNames.addAll(duplicates);
        }

        Map<String, Node> nameObjectMapping = new HashMap<>();

        // Create transitions and add them to the name map.
        transitionNames.stream().forEach(t -> nameObjectMapping.put(t, createTransition(t, petriNetPage)));

        // Obtain the name of places.
        List<String> placeNames = specificationLines.stream().flatMap(line -> Stream.of(line.split(" "))).distinct()
                .filter(e -> !nameObjectMapping.containsKey(e)).toList();

        // Create places and add them to the name map.
        placeNames.stream().forEach(p -> nameObjectMapping.put(p, createPlace(p, petriNetPage)));

        // Obtain the marking line.
        String markingIdentifier = ".marking";
        String markingLine = petriNetBody.stream().filter(line -> line.contains(markingIdentifier)).findFirst().get();

        // Parse the marking place in curly brackets.
        Pattern pattern = Pattern.compile("\\{\\s*([^}]+)\\s*\\}");
        Matcher matcher = pattern.matcher(markingLine);
        Preconditions.checkArgument(matcher.find(), "Expected the Petri Net input to contain a marking place.");
        String markingPlaceName = matcher.group(1).trim();
        Place markingPlace = (Place)nameObjectMapping.get(markingPlaceName);

        // Create marking for the marking place.
        PTMarking initialMarking = petriNetFactory.createPTMarking();
        initialMarking.setText((long)1);
        initialMarking.setContainerPlace(markingPlace);
        markingPlace.setInitialMarking(initialMarking);

        // Iterate over specification lines to create arcs.
        for (String line: specificationLines) {
            List<String> elements = Arrays.asList(line.split(" "));
            String source = elements.get(0);
            elements.stream().skip(1).forEach(
                    (target) -> createArc(nameObjectMapping.get(source), nameObjectMapping.get(target), petriNetPage));
        }

        return petriNetPage;
    }

    private static Page initializePetriNetPage(String petriNetId) {
        PtnetFactory petriNetFactory = PtnetFactory.eINSTANCE;

        // Create Petri Net doc.
        PetriNetDoc petriNetDoc = petriNetFactory.createPetriNetDoc();

        // Create Petri Net.
        PetriNet petriNet = petriNetFactory.createPetriNet();
        petriNet.setId(petriNetId);
        petriNet.setContainerPetriNetDoc(petriNetDoc);

        // Create page in Petri Net.
        Page page = petriNetFactory.createPage();
        page.setContainerPetriNet(petriNet);
        page.setId(petriNetId);

        return page;
    }

    private static Transition createTransition(String name, Page page) {
        Transition transition = petriNetFactory.createTransition();
        transition.setId(name);
        transition.setName(createName(name));
        transition.setContainerPage(page);
        return transition;
    }

    private static Place createPlace(String name, Page page) {
        Place place = petriNetFactory.createPlace();
        place.setId(name);
        place.setName(createName(name));
        place.setContainerPage(page);
        return place;
    }

    private static Name createName(String name) {
        Name nameObject = petriNetFactory.createName();
        nameObject.setText(name);
        return nameObject;
    }

    private static Arc createArc(Node source, Node target, Page page) {
        Arc arc = petriNetFactory.createArc();
        arc.setContainerPage(page);
        arc.setId(source.getId() + "_to_" + target.getId());
        arc.setSource(source);
        arc.setTarget(target);
        source.getOutArcs().add(arc);
        return arc;
    }
}
