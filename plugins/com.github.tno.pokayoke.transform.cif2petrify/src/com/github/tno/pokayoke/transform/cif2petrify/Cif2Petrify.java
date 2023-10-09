
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Alphabet;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;

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

        EList<Component> components = specification.getComponents();

        // Iterate over components to extract information from the component named 'statespace'.
        for (Component component: components) {
            if (component.getName().equals("statespace")) {
                Automaton automaton = (Automaton)component;

                // Extract the name of events.
                Alphabet alphabet = automaton.getAlphabet();
                List<String> eventNames = new ArrayList<>();
                for (Expression expression: alphabet.getEvents()) {
                    EventExpression eventExpression = (EventExpression)expression;
                    eventNames.add(eventExpression.getEvent().getName());
                }

                // Append the declaration of events.
                stringBuilder.append(String.join(" ", eventNames));
                stringBuilder.append("\n");

                stringBuilder.append(".state graph");
                stringBuilder.append("\n");

                List<Location> markedLocactions = new ArrayList<>();

                // Iterate over locations to extract edge info.
                for (Location location: automaton.getLocations()) {
                    // Identify the initial location and append the first edge.
                    List<Expression> initialExpressions = location.getInitials();
                    if (initialExpressions.size() > 0) {
                        stringBuilder.append(String.format("loc0 start %s", location.getName()));
                        stringBuilder.append("\n");
                    }

                    // Identify the marked locations.
                    if (location.getMarkeds().size() > 0 & location.getEdges().size() == 0) {
                        markedLocactions.add(location);
                    }

                    // Iterate over edges to extract edge events and append them to the string.
                    for (Edge edge: location.getEdges()) {
                        List<String> edgeEvents = new ArrayList<>();

                        // Collect the name of edge events.
                        for (EdgeEvent edgeEvent: edge.getEvents()) {
                            EventExpression expression = (EventExpression)edgeEvent.getEvent();
                            String eventName = expression.getEvent().getName();
                            edgeEvents.add(eventName);
                        }

                        // Append the edge.
                        if (edgeEvents.size() > 0) {
                            String edgeString = String.format("%s %s %s", location.getName(),
                                    String.join(",", edgeEvents), edge.getTarget().getName());
                            stringBuilder.append(edgeString);
                            stringBuilder.append("\n");
                        }
                    }
                }
                // Append the looping edge.
                for (Location markedLocation: markedLocactions) {
                    stringBuilder.append(String.format("%s end loc0", markedLocation.getName()));
                    stringBuilder.append("\n");
                }

                // Append the marking string.
                stringBuilder.append(".marking {loc0}");
                stringBuilder.append("\n");
            }
        }

        // Append the ending string.
        stringBuilder.append(".end");

        return stringBuilder.toString();
    }
}
