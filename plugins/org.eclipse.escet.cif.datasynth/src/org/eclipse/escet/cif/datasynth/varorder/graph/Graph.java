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

package org.eclipse.escet.cif.datasynth.varorder.graph;

import static org.eclipse.escet.common.java.BitSets.bitset;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Pair.pair;
import static org.eclipse.escet.common.java.Sets.list2set;

import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.escet.common.box.GridBox;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.BitSets;

/** A weighted graph with empty diagonal. */
public class Graph {
    /**
     * The list of nodes of this graph, in ascending order of their indices. The first node has index zero and the last
     * node has index {@code n - 1}, assuming there are {@code n} nodes.
     */
    public final List<Node> nodes;

    /**
     * Constructor for the {@link Graph} class.
     *
     * @param nodeCount The number of nodes.
     */
    public Graph(int nodeCount) {
        this.nodes = listc(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new Node(i));
        }
    }

    /**
     * Returns the node with the given index.
     *
     * @param index The node index.
     * @return The node.
     */
    public Node node(int index) {
        return nodes.get(index);
    }

    /**
     * Returns the size of the graph.
     *
     * @return The number of nodes in the graph.
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Returns the number of unique edges of the graph.
     *
     * @return The number of unique edges of the graph.
     */
    public long edgeCount() {
        return nodes.stream() // Get all nodes.
                .flatMap(n -> n.neighbours().stream().map(m -> pair(n.index, m.index))) // Get edge node index pairs.
                .map(p -> (p.left > p.right) ? pair(p.right, p.left) : p) // Normalize edge pairs: left < right.
                .distinct().count(); // Count unique edges.
    }

    /**
     * Partition this graph, which may be non-connected, into connected sub-graphs.
     *
     * @return Per partition (connected sub-graph), the nodes of that partition.
     */
    public List<List<Node>> partition() {
        List<List<Node>> partitions = listc(1); // Optimize for connected graphs.
        BitSet unpartitionedNodes = BitSets.ones(nodes.size());
        while (!unpartitionedNodes.isEmpty()) {
            // Initialize an empty new partition. Then iteratively consider new nodes to add. Before and after each
            // iteration the following invariant holds: the 'new nodes' and 'partition' are disjoint.
            BitSet partition = bitset();
            BitSet newNodes = bitset();
            newNodes.set(unpartitionedNodes.nextSetBit(0));
            while (!newNodes.isEmpty()) {
                // Find all neighbours of the new nodes.
                BitSet neighbours = bitset();
                for (int i: BitSets.iterateTrueBits(newNodes)) {
                    for (Node neighbour: node(i).neighbours()) {
                        neighbours.set(neighbour.index);
                    }
                }

                // Move new nodes to the partition.
                partition.or(newNodes);

                // Promote the neighbours not yet in the 'partition' to the 'new nodes'. Restores the invariant.
                newNodes = neighbours;
                newNodes.andNot(partition);
            }

            // Add partition.
            partitions.add(partition.stream().mapToObj(i -> node(i)).collect(Collectors.toList()));

            // Update unpartitioned nodes.
            unpartitionedNodes.andNot(partition);
        }
        return partitions;
    }

    /**
     * Reorder the graph to the given node order.
     *
     * @param newOrder The new node order.
     * @return The newly created reordered graph.
     */
    public Graph reorder(List<Node> newOrder) {
        // Check that given nodes are the nodes of this graph in some order.
        Assert.areEqual(list2set(newOrder), list2set(nodes));

        // Construct the new graph.
        Graph newGraph = new Graph(nodes.size());

        // Get mapping of old nodes to new nodes.
        Map<Node, Node> nodeMap = IntStream.range(0, newOrder.size()).boxed()
                .collect(Collectors.toMap(newOrder::get, i -> newGraph.nodes.get(i)));

        // Add edges to new graph.
        for (Node node: nodes) {
            Node newNode = nodeMap.get(node);
            for (Entry<Node, Integer> edge: node.edges()) {
                Node newTarget = nodeMap.get(edge.getKey());
                newNode.addEdge(newTarget, edge.getValue());
            }
        }
        return newGraph;
    }

    @Override
    public String toString() {
        // If only single-digit weights are used, then no separation is used in the matrix representation.
        // Otherwise a single space is used as separator between columns.
        int maxWeight = nodes.stream().flatMap(n -> n.edges().stream()).map(e -> e.getValue())
                .max(Comparator.naturalOrder()).orElse(0);
        int separator = (maxWeight <= 9) ? 0 : 1;
        return toString(separator);
    }

    /**
     * Converts the graph to a textual matrix representation, for debugging and testing purposes.
     *
     * @param separation The number of spaces separation between columns.
     * @return The textual matrix representation.
     */
    String toString(int separation) {
        GridBox grid = new GridBox(nodes.size(), nodes.size(), 0, separation);
        for (Node node: nodes) {
            for (Entry<Node, Integer> edge: node.edges()) {
                grid.set(node.index, edge.getKey().index, Integer.toString(edge.getValue()));
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (grid.get(i, j) == null) {
                    grid.set(i, j, ".");
                }
            }
        }
        return String.join("\n", grid.getLines());
    }
}
