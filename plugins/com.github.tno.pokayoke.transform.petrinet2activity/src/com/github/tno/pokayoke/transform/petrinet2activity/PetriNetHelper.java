
package com.github.tno.pokayoke.transform.petrinet2activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
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

/** Helper for parsing Petrify input. */
public class PetriNetHelper {
    private PetriNetHelper() {
    }

    private static PtnetFactory pnFactory = PtnetFactory.eINSTANCE;

    public static Page parsePetriNet(String sourcePath) throws IOException {
        List<String> inputPetriNet = FileHelper.readFile(sourcePath);

        // Obtain model name.
        String nameHeader = ".model ";
        String modelName = inputPetriNet.stream().filter(line -> line.startsWith(nameHeader))
                .map(line -> line.replace(nameHeader, "")).findFirst().get();

        // Create a Petri Net page.
        Page petrinetPage = PetriNetHelper.initializePetriNetPage(modelName.toString());

        // Exclude the header lines.
        List<String> petrinetBody = FileHelper.readFile(sourcePath).stream().filter(line -> !line.startsWith("#"))
                .toList();

        // Collect the name of the declared transitions.
        String dummyString = ".dummy  ";
        Optional<String> transitionDeclaration = petrinetBody.stream().filter(line -> line.startsWith(dummyString))
                .map(line -> line.replaceAll(dummyString, "").trim()).findFirst();
        Preconditions.checkArgument(transitionDeclaration.isPresent(),
                "Expected the Petri Net input to contain transition decalrations.");
        List<String> transitionNames = new ArrayList<>(Arrays.asList(transitionDeclaration.get().split(" ")));

        // Obtain the specification lines.
        List<String> specificationLines = petrinetBody.stream().filter(line -> !line.startsWith(".")).toList();

        // Obtain the name of all the elements in the specification.
        List<String> transitionPlaceNames = specificationLines.stream().flatMap(line -> Stream.of(line.split(" ")))
                .distinct().toList();

        // Identify and add the name of the duplicate transitions.
        List<String> withoutTransitionDuplicates = new ArrayList<>(transitionNames);
        for (String transitionName: withoutTransitionDuplicates) {
            List<String> duplicates = transitionPlaceNames.stream()
                    .filter(e -> !e.equals(transitionName) && e.startsWith(transitionName)).toList();
            transitionNames.addAll(duplicates);
        }

        Map<String, Node> nameObjectMapping = new HashMap<>();

        // Create transitions.
        transitionNames.stream().forEach(t -> nameObjectMapping.put(t, createTransition(t, petrinetPage)));

        // Collect the name of places.
        List<String> placeNames = specificationLines.stream().flatMap(line -> Stream.of(line.split(" "))).distinct()
                .filter(e -> !nameObjectMapping.containsKey(e)).toList();

        // Create places.
        placeNames.stream().forEach(p -> nameObjectMapping.put(p, createPlace(p, petrinetPage)));

        // Obtain the marking line.
        String markingIdentifier = ".marking";
        String markingLine = petrinetBody.stream().filter(line -> line.contains(markingIdentifier)).findFirst().get();

        // Identify the marking place and set it as initial marking.
        Pattern pattern = Pattern.compile("\\{\\s*([^}]+)\\s*\\}");
        Matcher matcher = pattern.matcher(markingLine);

        Preconditions.checkArgument(matcher.find(), "Expected the Petri Net input to contain the marking place.");

        String initialPlaceName = matcher.group(1).trim();
        PTMarking initialMarking = pnFactory.createPTMarking();
        initialMarking.setText((long)1);
        Place initNodePlace = (Place)nameObjectMapping.get(initialPlaceName);
        initialMarking.setContainerPlace(initNodePlace);
        initNodePlace.setInitialMarking(initialMarking);

        // Iterate over specification line to create arcs.
        for (String line: specificationLines) {
            List<String> elements = Arrays.asList(line.split(" "));
            String source = elements.get(0);
            elements.stream().skip(1).forEach(
                    (target) -> createArc(nameObjectMapping.get(source), nameObjectMapping.get(target), petrinetPage));
        }

        return petrinetPage;
    }

    public static Page initializePetriNetPage(String petriNetId) {
        PtnetFactory pnFactory = PtnetFactory.eINSTANCE;

        // Create Petri Net doc.
        PetriNetDoc pnd = pnFactory.createPetriNetDoc();

        // Create Petri Net.
        PetriNet pn = pnFactory.createPetriNet();
        pn.setId(petriNetId);
        pn.setContainerPetriNetDoc(pnd);

        // Create page in Petri Net
        Page page = pnFactory.createPage();
        page.setContainerPetriNet(pn);
        page.setId(petriNetId);

        return page;
    }

    private static Transition createTransition(String nameString, Page page) {
        Transition transition = pnFactory.createTransition();
        transition.setId(nameString);
        transition.setName(createName(nameString));
        transition.setContainerPage(page);
        return transition;
    }

    private static Place createPlace(String nameString, Page page) {
        Place place = pnFactory.createPlace();
        place.setId(nameString);
        place.setName(createName(nameString));
        place.setContainerPage(page);
        return place;
    }

    private static Name createName(String nameString) {
        Name name = pnFactory.createName();
        name.setText(nameString);
        return name;
    }

    private static Arc createArc(Node source, Node target, Page page) {
        Arc arc = pnFactory.createArc();
        arc.setContainerPage(page);
        arc.setId(source.getId() + "_to_" + target.getId());
        arc.setSource(source);
        arc.setTarget(target);
        source.getOutArcs().add(arc);
        return arc;
    }

    public static void exportToPNMLFile(Page page, String outputPath) throws IOException {
        File file = new File(outputPath);
        try (FileOutputStream output = new FileOutputStream(file)) {
            FileChannel channel = output.getChannel();
            page.getContainerPetriNet().getContainerPetriNetDoc().toPNML(channel);
            channel.close();
        }
    }
}
