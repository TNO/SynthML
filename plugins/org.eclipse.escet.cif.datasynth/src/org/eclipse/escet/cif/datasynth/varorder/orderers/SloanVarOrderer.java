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

package org.eclipse.escet.cif.datasynth.varorder.orderers;

import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.cif.datasynth.varorder.graph.algos.SloanNodeOrderer;
import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RepresentationKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;

/**
 * Sloan profile/wavefront-reducing variable ordering heuristic.
 *
 * @see SloanNodeOrderer
 */
public class SloanVarOrderer extends VarOrderer {
    /** The relations to use to obtain the graph and to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link SloanVarOrderer} class.
     *
     * @param relationsKind The kind of relations to use to obtain the graph and to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public SloanVarOrderer(RelationsKind relationsKind, VarOrdererEffect effect) {
        this.relationsKind = relationsKind;
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Get graph.
        Graph graph = inputData.helper.getGraph(relationsKind);

        // Debug output before applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying Sloan algorithm:");
            inputData.helper.dbg(dbgLevel + 1, "Relations: %s", enumValueToParserArg(relationsKind));
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
            inputData.helper.dbgRepresentation(dbgLevel + 1, RepresentationKind.GRAPH, relationsKind);
            inputData.helper.dbg();
        }

        // Skip algorithm if no graph edges.
        if (graph.edgeCount() == 0) {
            if (dbgEnabled) {
                inputData.helper.dbg(dbgLevel + 1, "Skipping algorithm: no graph edges.");
            }
            return inputData;
        }

        // More debug output before applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForVarOrder(dbgLevel + 1, inputData.varOrder.getOrderedVars(), "before",
                    relationsKind);
        }

        // Apply algorithm.
        List<Node> order = new SloanNodeOrderer().orderNodes(graph);

        // Debug output after applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForNodeOrder(dbgLevel + 1, order, "after", relationsKind);
        }

        // Return the resulting order.
        return new VarOrdererData(inputData,
                VarOrder.createFromOrderedVars(inputData.helper.reorderForNodeOrder(order)), effect);
    }

    @Override
    public String toString() {
        return fmt("sloan(relations=%s, effect=%s)", enumValueToParserArg(relationsKind), enumValueToParserArg(effect));
    }
}
