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

package org.eclipse.escet.cif.datasynth.varorder.metrics;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrderHelper;

/** Variable order metric. Lower metric values (heuristically) indicate better variable orders. */
public interface VarOrderMetric {
    /**
     * Compute the metric value. Lower metric values (heuristically) indicate better variable orders.
     *
     * @param newIndices For each variable, its new 0-based index.
     * @param hyperEdges The hyper-edges to use to compute the metric value.
     * @return The metric value.
     */
    public double computeForNewIndices(int[] newIndices, List<BitSet> hyperEdges);

    /**
     * Compute the metric value. Lower metric values (heuristically) indicate better variable orders.
     *
     * @param helper Helper for variable ordering.
     * @param order The variable order.
     * @param hyperEdges The hyper-edges to use to compute the metric value.
     * @return The metric value.
     */
    public default double computeForVarOrder(VarOrderHelper helper, List<SynthesisVariable> order,
            List<BitSet> hyperEdges)
    {
        int[] newIndices = helper.getNewIndicesForVarOrder(order);
        return computeForNewIndices(newIndices, hyperEdges);
    }

    /**
     * Compute the metric value. Lower metric values (heuristically) indicate better variable orders.
     *
     * @param helper Helper for variable ordering.
     * @param order The node order.
     * @param hyperEdges The hyper-edges to use to compute the metric value.
     * @return The metric value.
     */
    public default double computeForNodeOrder(VarOrderHelper helper, List<Node> order, List<BitSet> hyperEdges) {
        int[] newIndices = helper.getNewIndicesForNodeOrder(order);
        return computeForNewIndices(newIndices, hyperEdges);
    }
}
