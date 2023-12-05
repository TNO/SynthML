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

import static org.eclipse.escet.common.java.Lists.listc;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Assert;

/** Node ordering algorithm. */
public abstract class NodeOrderer {
    /**
     * Order the nodes of the given graph using the node ordering algorithm.
     *
     * @param graph The graph.
     * @return The ordered nodes.
     */
    public List<Node> orderNodes(Graph graph) {
        // Partition the graph into connected sub-graphs.
        List<List<Node>> partitions = graph.partition();

        // Sort the partitions on descending size. This puts unconnected nodes at the end of the computed order.
        Collections.sort(partitions, Comparator.comparingInt((List<Node> p) -> p.size()).reversed());

        // Order the nodes per partition.
        List<Node> newOrder = listc(graph.size());
        for (List<Node> partition: partitions) {
            List<Node> newPartitionOrder = orderNodesPartition(graph, partition);
            newOrder.addAll(newPartitionOrder);
        }
        Assert.areEqual(graph.size(), newOrder.size());
        return newOrder;
    }

    /**
     * Order the nodes of the given partition sub-graph using the node ordering algorithm.
     *
     * @param graph The entire graph.
     * @param partition The connected partition sub-graph.
     * @return The ordered nodes of the partition.
     */
    protected abstract List<Node> orderNodesPartition(Graph graph, List<Node> partition);
}
