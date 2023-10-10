
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

        // Append the heading string.
        stringBuilder.append(".model safestatespace");
        stringBuilder.append("\n");
        stringBuilder.append(".dummy start end ");

        // Get the state space.
        Optional<Automaton> possibleStatespace = specification.getComponents().stream()
                .filter(c -> c instanceof Automaton a && a.getName().equals("statespace")).map(c -> (Automaton)c)
                .findFirst();

        Preconditions.checkArgument(possibleStatespace.isPresent(),
                "Expected the CIF specification to include a state space automaton.");

        Automaton automaton = possibleStatespace.get();

        // Extract the name of events.
        List<String> eventNames = CifEventUtils.getAlphabet(automaton).stream().map(Event::getName).toList();

        // Append the declaration of events.
        stringBuilder.append(String.join(" ", eventNames));
        stringBuilder.append("\n");

        stringBuilder.append(".state graph");
        stringBuilder.append("\n");

        // Iterate over all locations in the state space and translate all edges.
        for (Location location: automaton.getLocations()) {
            // Translate initial locations.
            if (!location.getInitials().isEmpty()) {
                // Append the starting edge.
                stringBuilder.append(String.format("loc0 start %s", location.getName()));
                stringBuilder.append("\n");
            }

            // Translate marked locations.
            if (!location.getMarkeds().isEmpty()) {
                Preconditions.checkArgument(location.getEdges().isEmpty(),
                        "Expected a marked location does not have outgoing edges");

                // Append the ending edge.
                stringBuilder.append(String.format("%s end loc0", location.getName()));
                stringBuilder.append("\n");
            }

            // Translate all edges that go out of the current location.
            for (Edge edge: location.getEdges()) {
                for (Event edgeEvent: CifEventUtils.getEvents(edge)) {
                    // Append the edge.
                    String edgeString = String.format("%s %s %s", location.getName(), edgeEvent.getName(),
                            edge.getTarget().getName());
                    stringBuilder.append(edgeString);
                    stringBuilder.append("\n");
                }
            }
        }

        // Append the marking string.
        stringBuilder.append(".marking {loc0}");
        stringBuilder.append("\n");

        // Append the ending string.
        stringBuilder.append(".end");
        stringBuilder.append("\n");

        return stringBuilder.toString();
    }
}
