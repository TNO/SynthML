/**
 *
 */

package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Alphabet;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;

/** Transform CIF state space to Petrify input. */
public class CIFToPetrify {
    private final Specification spec;

    public CIFToPetrify(Specification spec) {
        this.spec = spec;
    }

    public void transformModel(BufferedWriter output) throws IOException {
        output.append(".model example");
        output.newLine();
        output.append(".dummy start end ");

        EList<Component> comps = spec.getComponents();
        for (Component comp: comps) {

            if (comp instanceof Automaton aut) {
                Alphabet alphabet = aut.getAlphabet();

                List<String> eventName = new ArrayList();

                for (Expression expres: alphabet.getEvents()) {
                    Event event = (Event)expres;
                    eventName.add(event.getName());
                }

                String.join(" ", eventName);
                output.newLine();
                output.append(".state graph");
                output.newLine();
                output.append("loc0 start loc1");
                output.newLine();
                for (Location loc: aut.getLocations()) {
                    output.append(loc.getName());
                    output.append(" ");
                    for (Edge edge: loc.getEdges()) {
                        List<String> edgeEvents = new ArrayList();
                        for (EdgeEvent edgeEvent: edge.getEvents()) {
                            Event event = (Event)edgeEvent.getEvent();
                            edgeEvents.add(event.getName());
                        }
                        output.append(String.join(",", edgeEvents));
                        output.append(" ");
                        output.append(edge.getTarget().getName());
                        output.newLine();
                    }
                }

              //  output.append("loc0 start loc1");
            }
        }
    }
}
