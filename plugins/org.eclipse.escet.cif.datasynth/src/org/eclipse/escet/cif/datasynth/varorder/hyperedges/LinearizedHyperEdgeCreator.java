//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.datasynth.varorder.hyperedges;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Lists.set2list;
import static org.eclipse.escet.common.java.Maps.map;
import static org.eclipse.escet.common.java.Maps.mapc;
import static org.eclipse.escet.common.java.Sets.set;
import static org.eclipse.escet.common.java.Sets.union;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.cif2cif.ElimStateEvtExclInvs;
import org.eclipse.escet.cif.cif2cif.LinearizeProduct;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifEventUtils.Alphabets;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.conversion.CifDataSynthesisLocationPointerManager;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

/**
 * Linearized automatic variable ordering hyper-edge creator. Creates the following hyper-edges:
 * <ul>
 * <li>For each linearized edge, a hyper-edge is created for the variables that occur in the guards and updates of that
 * linearized edge.</li>
 * </ul>
 * Variables that occur via algebraic variables are taken into account.
 **/
public class LinearizedHyperEdgeCreator extends HyperEdgeCreator {
    /**
     * Constructor for the {@link LinearizedHyperEdgeCreator} class.
     *
     * @param spec The CIF specification.
     * @param variables The synthesis variables.
     */
    public LinearizedHyperEdgeCreator(Specification spec, List<SynthesisVariable> variables) {
        super(spec, variables);
    }

    @Override
    public List<BitSet> getHyperEdges() {
        // Create a copy of the specification, to prevent modifying the input specification.
        Specification spec = EMFHelper.deepclone(getSpecification());

        // Convert state/event exclusion invariants to automata. They will then be taken into account for the linearized
        // edges, similar to how they are also part of the linearized edges during synthesis itself.
        new ElimStateEvtExclInvs().transform(spec);

        // Collect all automata and alphabets.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, list());
        Assert.check(!automata.isEmpty());
        List<Alphabets> alphabets = CifEventUtils.getAllAlphabets(automata, null);
        Set<Event> events = alphabets.stream().map(a -> union(a.syncAlphabet, a.sendAlphabet, a.recvAlphabet))
                .reduce((eventSet1, eventSet2) -> union(eventSet1, eventSet2)).get();
        List<Automaton> lpAuts = automata.stream().filter(aut -> aut.getLocations().size() > 1)
                .collect(Collectors.toList());

        // Linearize the edges of the copy of the specification.
        // Must match a similar call to linearize edges in `CifToSynthesisConverter'.
        CifDataSynthesisLocationPointerManager locPtrManager = new CifDataSynthesisLocationPointerManager(lpAuts);
        List<Edge> linearizedEdges = list();
        LinearizeProduct.linearizeEdges(automata, alphabets, set2list(events), locPtrManager, false, true,
                linearizedEdges);

        // Create variable mapping, based on the absolute names of objects from the original and copied specification.
        // The absolute names are identical, as only edges are linearized, not the entire specification.
        VariableMapping varMap = new VariableMapping(getVariables());

        // Create a hyper-edge for each linearized edge.
        List<BitSet> hyperEdges = listc(linearizedEdges.size());
        for (Edge edge: linearizedEdges) {
            // Collect variables from guards and updates.
            VariableCollector varCollector = new VariableCollector();
            Set<PositionObject> edgeVars = set();
            for (Expression guard: edge.getGuards()) {
                varCollector.collectCifVarObjs(guard, edgeVars);
            }
            for (Update update: edge.getUpdates()) {
                varCollector.collectCifVarObjs(update, edgeVars);
            }

            // Map variables from linearized copy of the specification to the ones from the synthesis variables for the
            // original specification.
            edgeVars = varMap.mapVars(edgeVars);

            // Add hyper-edge.
            addHyperEdge(edgeVars, hyperEdges);
        }

        // Return the hyper-edges.
        return hyperEdges;
    }

    /**
     * Mapping for variables from the linearized copy of the specification to variables from the original specification
     * as represented by the synthesis variables.
     */
    private static class VariableMapping {
        /**
         * Absolute non-escaped names of synthesis variables to their variable objects as represented by synthesis
         * variables.
         */
        private Map<String, PositionObject> mapping = map();

        /**
         * Constructor for the {@link VariableMapping} class.
         *
         * @param synthVars The synthesis variables.
         */
        private VariableMapping(List<SynthesisVariable> synthVars) {
            this.mapping = mapc(synthVars.size());
            for (SynthesisVariable synthVar: synthVars) {
                PositionObject obj;
                if (synthVar instanceof SynthesisDiscVariable) {
                    obj = ((SynthesisDiscVariable)synthVar).var;
                } else if (synthVar instanceof SynthesisInputVariable) {
                    obj = ((SynthesisInputVariable)synthVar).var;
                } else if (synthVar instanceof SynthesisLocPtrVariable) {
                    obj = ((SynthesisLocPtrVariable)synthVar).aut;
                } else {
                    throw new RuntimeException("Unknown synthesis variable: " + synthVar);
                }
                PositionObject prev = mapping.put(synthVar.rawName, obj);
                Assert.check(prev == null); // Ensure no mapping entries get overwritten.
            }
        }

        /**
         * Map the given variables.
         *
         * @param vars The variables to map.
         * @return The mapped variables.
         */
        private Set<PositionObject> mapVars(Set<PositionObject> vars) {
            return vars.stream().map(v -> mapVar(v)).collect(Collectors.toCollection(() -> set()));
        }

        /**
         * Map the given variable.
         *
         * @param var The variable to map.
         * @return The mapped variable.
         */
        private PositionObject mapVar(PositionObject var) {
            String name = CifTextUtils.getAbsName(var, false);
            PositionObject mappedVar = mapping.get(name);
            Assert.notNull(mappedVar, name);
            return mappedVar;
        }
    }
}
