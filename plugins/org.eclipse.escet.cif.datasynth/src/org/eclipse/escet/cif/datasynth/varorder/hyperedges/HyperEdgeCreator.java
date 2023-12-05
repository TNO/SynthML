//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
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

import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

/** Automatic variable ordering hyper-edge creator. */
public abstract class HyperEdgeCreator {
    /** The CIF specification. Must not be modified. */
    private final Specification spec;

    /** The synthesis variables. */
    private final List<SynthesisVariable> variables;

    /** Per synthesis variable CIF object, the index into the bitset for a hyper-edge. */
    private final Map<PositionObject, Integer> synthVarBitIndices;

    /**
     * Constructor for the {@link HyperEdgeCreator} class.
     *
     * @param spec The CIF specification. Must not be modified.
     * @param variables The synthesis variables.
     */
    public HyperEdgeCreator(Specification spec, List<SynthesisVariable> variables) {
        this.spec = spec;
        this.variables = variables;

        synthVarBitIndices = mapc(variables.size());
        for (int i = 0; i < variables.size(); i++) {
            SynthesisVariable synthVar = variables.get(i);
            if (synthVar instanceof SynthesisDiscVariable) {
                synthVarBitIndices.put(((SynthesisDiscVariable)synthVar).var, i);
            } else if (synthVar instanceof SynthesisInputVariable) {
                synthVarBitIndices.put(((SynthesisInputVariable)synthVar).var, i);
            } else if (synthVar instanceof SynthesisLocPtrVariable) {
                synthVarBitIndices.put(((SynthesisLocPtrVariable)synthVar).aut, i);
            } else {
                throw new RuntimeException("Unknown synthesis variable: " + synthVar);
            }
        }
    }

    /**
     * Returns the CIF specification.
     *
     * @return The CIF specification.
     */
    protected Specification getSpecification() {
        return spec;
    }

    /**
     * Returns the synthesis variables.
     *
     * @return The synthesis variables.
     */
    protected List<SynthesisVariable> getVariables() {
        return variables;
    }

    /**
     * Create hyper-edges.
     *
     * @return The hyper-edges. Each bitset represents a hyper-edge. Within each hyper-edge, there are bits
     *     corresponding to the synthesis variables of the {@link #getSpecification CIF specification}, indicating
     *     whether each variable is included in the hyper-edge or not. The bit indices in the bitsets correspond to the
     *     indices of the {@link #getVariables synthesis variables}.
     */
    public abstract List<BitSet> getHyperEdges();

    /**
     * Add a hyper-edge for the given CIF variable objects. Creating and adding a hyper-edge is skipped if no CIF
     * variable objects are provided.
     *
     * @param edgeVars The CIF variable objects for which to create a new hyper-edge. This must be a subset of the
     *     variables represented by {@code variables}.
     * @param hyperEdges The collection of hyper-edges so far, gets expanded in-place.
     */
    protected void addHyperEdge(Collection<PositionObject> edgeVars, List<BitSet> hyperEdges) {
        // Skip creation of hyper-edges without any variables.
        if (edgeVars.isEmpty()) {
            return;
        }

        // Create and add hyper-edge.
        BitSet hyperEdge = new BitSet(synthVarBitIndices.size());
        for (PositionObject var: edgeVars) {
            int bitIdx = synthVarBitIndices.get(var);
            hyperEdge.set(bitIdx);
        }
        hyperEdges.add(hyperEdge);
    }
}
