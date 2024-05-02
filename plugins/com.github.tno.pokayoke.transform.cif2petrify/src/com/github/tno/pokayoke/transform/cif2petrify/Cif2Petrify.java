
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
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.google.common.base.Preconditions;

/** Transforms CIF state spaces to Petrify input. */
public class Cif2Petrify {
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

        // Obtain the list of names from the events in the alphabet of the CIF state space automaton.
        List<String> eventNames = CifEventUtils.getAlphabet(automaton).stream().map(Event::getName).toList();

        Preconditions.checkArgument(eventNames.stream().distinct().count() == eventNames.size(),
                "Expected all event names in the state space alphabet to be uniquely named.");
        Preconditions.checkArgument(
                eventNames.stream().filter(e -> e.equals("start") || e.equals("end")).findAny().isEmpty(),
                "Expected that 'start' and 'end' are not used as event names.");

        // Declare a Petrify event for every event in the alphabet of the CIF state space automaton.
        petrifyInput.add(".dummy start end " + String.join(" ", eventNames));

        petrifyInput.add(".state graph");

        // Iterate over all locations in the state space and translate all edges.
        for (Location location: automaton.getLocations()) {
            String locationName = location.getName();

            Preconditions.checkNotNull(locationName, "Expected locations to have a name.");
            Preconditions.checkArgument(!locationName.equals("loc0"),
                    "Expected no locations in the state space automaton to be named 'loc0'.");

            boolean isTriviallyInitial = location.getInitials().isEmpty() ? false
                    : CifValueUtils.isTriviallyTrue(location.getInitials(), true, true);
            boolean isTriviallyNotInitial = location.getInitials().isEmpty() ? true
                    : CifValueUtils.isTriviallyFalse(location.getInitials(), true, true);
            Preconditions.checkArgument(isTriviallyInitial || isTriviallyNotInitial,
                    "Expected that locations are either trivially initial or trivially not initial.");

            boolean isTriviallyMarked = location.getMarkeds().isEmpty() ? false
                    : CifValueUtils.isTriviallyTrue(location.getMarkeds(), false, true);
            boolean isTriviallyNotMarked = location.getMarkeds().isEmpty() ? true
                    : CifValueUtils.isTriviallyFalse(location.getMarkeds(), false, true);
            Preconditions.checkArgument(isTriviallyMarked || isTriviallyNotMarked,
                    "Expected that locations are either trivially marked or trivially not marked.");
            Preconditions.checkArgument(!location.getEdges().isEmpty() || isTriviallyMarked,
                    "Expected non-marked locations to have outgoing edges.");

            // Translate initial locations.
            if (isTriviallyInitial) {
                petrifyInput.add(String.format("loc0 start %s", locationName));
            }

            // Translate marked locations.
            if (isTriviallyMarked) {
                Preconditions.checkArgument(location.getEdges().isEmpty(),
                        "Expected marked locations to not have outgoing edges.");

                petrifyInput.add(String.format("%s end loc0", locationName));
            }

            // Translate all edges that go out of the current location.
            for (Edge edge: location.getEdges()) {
                for (Event edgeEvent: CifEventUtils.getEvents(edge)) {
                    Location targetLocation = CifEdgeUtils.getTarget(edge);
                    String targetLocationName = targetLocation.getName();
                    String edgeString = String.format("%s %s %s", locationName, edgeEvent.getName(),
                            targetLocationName);
                    petrifyInput.add(edgeString);
                }
            }
        }

        // Indicate that the first location has a token initially.
        petrifyInput.add(".marking {loc0}");

        // Indicate the end of the Petrify input graph.
        petrifyInput.add(".end");

        return petrifyInput;
    }
}
