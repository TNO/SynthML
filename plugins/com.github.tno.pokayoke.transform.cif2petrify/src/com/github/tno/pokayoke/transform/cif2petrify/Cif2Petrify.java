
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

/** Transform CIF state space to Petrify input. */
public class Cif2Petrify {
    private final Specification spec;

    private final StringBuilder stringBuilder = new StringBuilder();

    public Cif2Petrify(Specification spec) {
        this.spec = spec;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            transformFile(args[0], args[1]);
        } else {
            throw new IOException("Exactly two arguments expected: a source path and a target path.");
        }
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Specification spec = FileHelper.loadCIFSpec(sourcePath);
        Cif2Petrify transformer = new Cif2Petrify(spec);
        String output = transformer.transformModel();
        FileHelper.storePetrifySpec(output, targetPath);
    }

    private String transformModel() {
        // Append heading string.
        stringBuilder.append(".model example");
        stringBuilder.append("\n");
        stringBuilder.append(".dummy start end ");

        EList<Component> comps = spec.getComponents();
        List<String> postConditionEvents = new ArrayList<>();

        // Iterate over components to extract information from components for event declaration (in 'Spec'), post
        // condition event declaration (in 'Post') and the state space (in 'statespace').
        for (Component comp: comps) {
            if (comp.getName().equals("Spec")) {
                Group compWithEvents = (Group)comp;
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
            } else if (comp.getName().equals("Post")) {
                Group compWithPostConditionEvents = (Group)comp;
                EList<Declaration> events = compWithPostConditionEvents.getDeclarations();

                // Collect post condition events.
                Iterator<Declaration> eventIterator = events.iterator();
                while (eventIterator.hasNext()) {
                    Declaration event = eventIterator.next();
                    postConditionEvents.add(event.getName());
                }
            } else if (comp.getName().equals("statespace")) {
                Automaton aut = (Automaton)comp;

                stringBuilder.append("loc0 start loc1");
                stringBuilder.append("\n");

                // Iterate over locations to extract edge info.
                for (Location loc: aut.getLocations()) {
                    // Iterate over edges to extract edge events and append them to the string.
                    for (Edge edge: loc.getEdges()) {
                        List<String> edgeEvents = new ArrayList<>();

                        // Collect the name of edge events.
                        for (EdgeEvent edgeEvent: edge.getEvents()) {
                            EventExpression express = (EventExpression)edgeEvent.getEvent();
                            String eventName = express.getEvent().getName();

                            // Add the name of event that is not a post-condition event to the list.
                            if (!postConditionEvents.contains(eventName)) {
                                edgeEvents.add(eventName);
                            }
                        }

                        // Append the edge.
                        if (edgeEvents.size() > 0) {
                            String str = String.format("%s %s %s", loc.getName(), String.join(",", edgeEvents),
                                    edge.getTarget().getName());
                            stringBuilder.append(str);
                            stringBuilder.append("\n");
                        }
                    }
                }

                // Get the last location in which the edge event is not a post condition event.
                String lastLocation = aut.getLocations().get(aut.getLocations().size() - postConditionEvents.size() - 1)
                        .getName();

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
