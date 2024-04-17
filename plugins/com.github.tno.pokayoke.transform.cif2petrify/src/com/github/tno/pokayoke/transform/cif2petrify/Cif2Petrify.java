
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;

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
        Specification specification = CifFileHelper.loadCifSpec(Paths.get(sourcePath));
        String body = Cif2Petrify.transform(specification);
        Files.writeString(Paths.get(targetPath), body);
    }

    public static String transform(Specification specification) {
        StringBuilder stringBuilder = new StringBuilder();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(specification, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1, "Expected the CIF specification to include one automaton.");
        Automaton automaton = automata.get(0);

        // Declare the header of the Petrify model.
        stringBuilder.append(".model ");
        stringBuilder.append(CifTextUtils.getAbsName(automaton));
        stringBuilder.append("\n.dummy start end ");

        // Obtain the list of names from the events in the alphabet of the CIF state space automaton.
        List<String> eventNames = CifEventUtils.getAlphabet(automaton).stream().map(Event::getName).toList();

        Preconditions.checkArgument(eventNames.stream().distinct().count() == eventNames.size(),
                "Expected all event names in the state space alphabet to be uniquely named.");
        Preconditions.checkArgument(
                eventNames.stream().filter(e -> e.equals("start") || e.equals("end")).findAny().isEmpty(),
                "Expected that 'start' and 'end' are not used as event names.");

        // Declare a Petrify event for every event in the alphabet of the CIF state space automaton.
        stringBuilder.append(String.join(" ", eventNames));
        stringBuilder.append("\n");

        stringBuilder.append(".state graph\n");

        // Iterate over all locations in the state space and translate all edges.
        for (Location location: automaton.getLocations()) {
            final String locationName = CifTextUtils.getAbsName(location);

            Preconditions.checkNotNull(locationName, "Expected locations to have a name.");
            Preconditions.checkArgument(!locationName.equals("loc0"),
                    "Expected no locations in the state space automaton to be named 'loc0'.");

            final EList<Expression> initials = location.getInitials();
            final boolean initialsIsEmpty = initials.isEmpty();
            boolean isTriviallyInitial = !initialsIsEmpty && CifValueUtils.isTriviallyTrue(initials, true, true);
            boolean isTriviallyNotInitial = initialsIsEmpty || CifValueUtils.isTriviallyFalse(initials, true, true);
            Preconditions.checkArgument(isTriviallyInitial || isTriviallyNotInitial,
                    "Expected that locations are either trivially initial or trivially not initial.");

            final EList<Expression> markeds = location.getMarkeds();
            final boolean markedsIsEmpty = markeds.isEmpty();
            final boolean isTriviallyMarked = !markedsIsEmpty && CifValueUtils.isTriviallyTrue(markeds, false, true);
            final boolean isTriviallyNotMarked = markedsIsEmpty || CifValueUtils.isTriviallyFalse(markeds, false, true);
            Preconditions.checkArgument(isTriviallyMarked || isTriviallyNotMarked,
                    "Expected that locations are either trivially marked or trivially not marked.");
            final boolean edgesIsEmpty = location.getEdges().isEmpty();
            Preconditions.checkArgument(!edgesIsEmpty || isTriviallyMarked,
                    "Expected non-marked locations to have outgoing edges.");

            // Translate initial locations.
            if (isTriviallyInitial) {
                stringBuilder.append("loc0 start ");
                stringBuilder.append(locationName);
                stringBuilder.append("\n");
            }

            // Translate marked locations.
            if (isTriviallyMarked) {
                Preconditions.checkArgument(edgesIsEmpty, "Expected marked locations to not have outgoing edges.");

                stringBuilder.append(locationName);
                stringBuilder.append(" end loc0\n");
            }

            // Translate all edges that go out of the current location.
            for (Edge edge: location.getEdges()) {
                for (Event edgeEvent: CifEventUtils.getEvents(edge)) {
                    final Location targetLocation = CifEdgeUtils.getTarget(edge);
                    final String targetLocationName = CifTextUtils.getAbsName(targetLocation);
                    final String edgeEventName = CifTextUtils.getAbsName(edgeEvent);
                    String edgeString = String.format("%s %s %s", locationName, edgeEventName, targetLocationName);
                    stringBuilder.append(edgeString);
                    stringBuilder.append("\n");
                }
            }
        }

        // Indicate that the first location has a token initially.
        stringBuilder.append(".marking {loc0}\n");

        // Indicate the end of the Petrify input graph.
        stringBuilder.append(".end\n");

        return stringBuilder.toString();
    }
}
