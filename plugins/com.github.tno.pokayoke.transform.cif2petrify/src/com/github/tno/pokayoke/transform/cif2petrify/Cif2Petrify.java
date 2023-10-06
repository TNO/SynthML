
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;

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
        String body = Cif2Petrify.transformModel(specification);
        FileHelper.writeToFile(body, targetPath);
    }

    private static String transformModel(Specification specification) {
        StringBuilder stringBuilder = new StringBuilder();

        // Append heading string.
        stringBuilder.append(".model safestatespace");
        stringBuilder.append("\n");
        stringBuilder.append(".dummy start end ");

        EList<Component> components = specification.getComponents();
        List<String> postConditionEvents = new ArrayList<>();

        // Iterate over components to extract information from components for event declaration (in 'Spec'), post
        // condition event declaration (in 'Post') and the state space (in 'statespace').
        for (Component component: components) {
            if (component.getName().equals("Spec")) {
                Group compWithEvents = (Group)component;
                EList<Declaration> events = compWithEvents.getDeclarations();

                List<String> eventName = new ArrayList<>();
                Iterator<Declaration> eventIterator = events.iterator();

                // Iterate over the event list to extract the name of events.
                while (eventIterator.hasNext()) {
                    Declaration event = eventIterator.next();
                    eventName.add(event.getName());
                }

                // Append the declaration of events.
                stringBuilder.append(String.join(" ", eventName));
                stringBuilder.append("\n");

                stringBuilder.append(".state graph");
                stringBuilder.append("\n");
            } else if (component.getName().equals("Post")) {
                Group compWithPostConditionEvents = (Group)component;
                EList<Declaration> events = compWithPostConditionEvents.getDeclarations();

                // Collect post condition events.
                Iterator<Declaration> eventIterator = events.iterator();
                while (eventIterator.hasNext()) {
                    Declaration event = eventIterator.next();
                    postConditionEvents.add(event.getName());
                }
            } else if (component.getName().equals("statespace")) {
                Automaton automaton = (Automaton)component;

                stringBuilder.append("loc0 start loc1");
                stringBuilder.append("\n");

                // Iterate over locations to extract edge info.
                for (Location location: automaton.getLocations()) {
                    // Iterate over edges to extract edge events and append them to the string.
                    for (Edge edge: location.getEdges()) {
                        List<String> edgeEvents = new ArrayList<>();

                        // Collect the name of edge events.
                        for (EdgeEvent edgeEvent: edge.getEvents()) {
                            EventExpression expression = (EventExpression)edgeEvent.getEvent();
                            String eventName = expression.getEvent().getName();

                            // Add the name of event that is not a post-condition event to the list.
                            if (!postConditionEvents.contains(eventName)) {
                                edgeEvents.add(eventName);
                            }
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

                // Get the last location in which the edge event is not a post condition event.
                String lastLocation = automaton.getLocations()
                        .get(automaton.getLocations().size() - postConditionEvents.size() - 1).getName();

                // Append the looping edge.
                String str = String.format("%s end loc0", lastLocation);
                stringBuilder.append(str);
                stringBuilder.append("\n");

                // Append the marking location.
                stringBuilder.append(".marking {loc0}");
                stringBuilder.append("\n");
            }
        }
        // Append ending string.
        stringBuilder.append(".end");

        return stringBuilder.toString();
    }
}
