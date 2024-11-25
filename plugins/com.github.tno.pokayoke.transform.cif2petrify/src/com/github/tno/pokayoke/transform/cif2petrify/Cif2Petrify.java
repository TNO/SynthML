
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.tno.pokayoke.transform.activitysynthesis.CifLocationHelper;
import com.google.common.base.Preconditions;

/** Transforms CIF state spaces to Petrify input. */
public class Cif2Petrify {
    private static final String LOOP_EVENT_NAME = "__loop";

    private Cif2Petrify() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            transformFile(Paths.get(args[0]), Paths.get(args[1]));
        } else {
            throw new IOException("Exactly two arguments expected: a source path and a target path.");
        }
    }

    public static void transformFile(Path inputFilePath, Path outputFolderPath) throws IOException {
        String filePrefix = FilenameUtils.removeExtension(inputFilePath.getFileName().toString());
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".g");
        Files.createDirectories(outputFolderPath);
        Specification specification = CifFileHelper.loadCifSpec(inputFilePath);
        List<String> body = Cif2Petrify.transform(specification);
        Files.write(petrifyInputPath, body);
    }

    public static List<String> transform(Specification specification) {
        List<String> petrifyInput = new ArrayList<>();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(specification, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1, "Expected the CIF specification to include one automaton.");
        Automaton automaton = automata.get(0);

        // Declare the header of the Petrify model.
        petrifyInput.add(".model " + automaton.getName());

        // Obtain the list of names from the events in the implicit alphabet of the CIF state space automaton. We first
        // remove the explict automaton alphabet, to ensure that 'CifEventUtils.getAlphabet' will give the implicit one.
        // This is needed, since Petrify will give warnings in case the explicit alphabet contains events that are not
        // actually used. And simply considering the derived alphabet here is easier than parsing Petrify's warnings.
        // Moreover, it is valid to reduce the alphabet here, since we use the result of the CIF explorer, which gives
        // us a single state space automaton. If some event is not used in this state space automaton, then it's never
        // enabled and can thus be removed from the alphabet.
        automaton.setAlphabet(null);
        List<String> eventNames = CifEventUtils.getAlphabet(automaton).stream().map(Event::getName).toList();

        Preconditions.checkArgument(eventNames.stream().distinct().count() == eventNames.size(),
                "Expected all event names in the state space alphabet to be uniquely named.");
        Preconditions.checkArgument(eventNames.stream().noneMatch(LOOP_EVENT_NAME::equals),
                "Expected that '" + LOOP_EVENT_NAME + "' is not used as an event name.");

        // Declare a Petrify event for every event in the CIF state space automaton alphabet, plus the 'loop' event.
        petrifyInput.add(".dummy " + String.join(" ", eventNames) + " " + LOOP_EVENT_NAME);

        petrifyInput.add(".state graph");

        // Iterate over all locations in the state space and translate all edges.
        for (Location location: automaton.getLocations()) {
            Preconditions.checkNotNull(location.getName(), "Expected locations to have a name.");

            // Translate all edges that go out of the current location.
            for (Edge edge: location.getEdges()) {
                for (Event edgeEvent: CifEventUtils.getEvents(edge)) {
                    Location targetLocation = CifEdgeUtils.getTarget(edge);
                    String targetLocationName = targetLocation.getName();
                    String edgeString = String.format("%s %s %s", location.getName(), edgeEvent.getName(),
                            targetLocationName);
                    petrifyInput.add(edgeString);
                }
            }
        }

        // Add the self-loop transition to the marked state, that indicates being done.
        Location markedLocation = CifLocationHelper.getMarkedLocation(automaton);
        String markedLocationName = markedLocation.getName();
        petrifyInput.add(String.format("%s %s %s", markedLocationName, LOOP_EVENT_NAME, markedLocationName));

        // Indicate that the initial location has a token initially.
        Location initialLocation = CifLocationHelper.getInitialLocation(automaton);
        petrifyInput.add(String.format(".marking {%s}", initialLocation.getName()));

        // Indicate the end of the Petrify input graph.
        petrifyInput.add(".end");

        return petrifyInput;
    }
}
