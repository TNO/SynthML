
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.escet.cif.common.CifEventUtils;
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
            transformFile(args[0], args[1]);
        } else {
            throw new IOException("Exactly two arguments expected: a source path and a target path.");
        }
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Specification specification = FileHelper.loadCifSpec(sourcePath);
        String body = Cif2Petrify.transform(specification);
        FileHelper.writeToFile(body, targetPath);
    }

    private static String transform(Specification specification) {
        StringBuilder stringBuilder = new StringBuilder();

        // Declare the header of the Petrify model.
        stringBuilder.append(".model safestatespace");
        stringBuilder.append("\n");
        stringBuilder.append(".dummy start end ");

        // Obtain the state space automaton in the CIF specification.
        Optional<Automaton> possibleStatespace = specification.getComponents().stream()
                .filter(c -> c instanceof Automaton a && a.getName().equals("statespace")).map(c -> (Automaton)c)
                .findFirst();

        Preconditions.checkArgument(possibleStatespace.isPresent(),
                "Expected the CIF specification to include a state space automaton.");

        Automaton automaton = possibleStatespace.get();

        // Obtain the list of names from the events in the alphabet of the CIF state space automaton.
        List<String> eventNames = CifEventUtils.getAlphabet(automaton).stream().map(Event::getName).toList();

        // Declare a Petrify event for every event in the alphabet of the CIF state space automaton.
        stringBuilder.append(String.join(" ", eventNames));
        stringBuilder.append("\n");

        stringBuilder.append(".state graph");
        stringBuilder.append("\n");

        // Iterate over all locations in the state space and translate all edges.
        for (Location location: automaton.getLocations()) {
            // Translate initial locations.
            if (!location.getInitials().isEmpty()) {
                stringBuilder.append(String.format("loc0 start %s", location.getName()));
                stringBuilder.append("\n");
            }

            // Translate marked locations.
            if (!location.getMarkeds().isEmpty()) {
                Preconditions.checkArgument(location.getEdges().isEmpty(),
                        "Expected marked locations to not have outgoing edges.");

                stringBuilder.append(String.format("%s end loc0", location.getName()));
                stringBuilder.append("\n");
            }

            // Translate all edges that go out of the current location.
            for (Edge edge: location.getEdges()) {
                for (Event edgeEvent: CifEventUtils.getEvents(edge)) {
                    String edgeString = String.format("%s %s %s", location.getName(), edgeEvent.getName(),
                            edge.getTarget().getName());
                    stringBuilder.append(edgeString);
                    stringBuilder.append("\n");
                }
            }
        }

        // Indicate that the first location has a token initially.
        stringBuilder.append(".marking {loc0}");
        stringBuilder.append("\n");

        // Indicate the end of the Petrify input graph.
        stringBuilder.append(".end");
        stringBuilder.append("\n");

        return stringBuilder.toString();
    }
}
