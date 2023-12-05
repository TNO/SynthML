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

package org.eclipse.escet.cif.datasynth.varorder.graph.algos;

import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Pair;

/** Pseudo-peripheral node pair finder algorithm. */
public interface PseudoPeripheralNodePairFinder extends PseudoPeripheralNodeFinder {
    /**
     * Find a pair of pseudo-peripheral nodes which lie at opposite ends of a pseudo-diameter, in the given sub-graph.
     *
     * @param graph The graph.
     * @param partition The sub-graph of the graph to consider. Must be a maximal connected sub-graph (partition) of
     *     {@code graph}.
     * @param startNode The start node at which to start the search. If provided, must be a node in the
     *     {@code partition}. May be {@code null} to let the algorithm itself decide the start node.
     * @return A pair of pseudo-peripheral nodes which lie at opposite ends of a pseudo-diameter.
     */
    public Pair<Node, Node> findPseudoPeripheralNodePair(Graph graph, List<Node> partition, Node startNode);
}
