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

import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.common.java.Assert;

/** A node in a {@link Graph}. */
public class Node {
    /** The 0-based index of the node in the graph. */
    public final int index;

    /** The edges, mapping an edge target node to the weight of the edge. */
    private final Map<Node, Integer> edges;

    /**
     * Constructor for the {@link Node} class.
     *
     * @param index The 0-based index of the node in the graph.
     */
    public Node(int index) {
        this.index = index;
        this.edges = mapc(2); // Assumes a sparse graph.
    }

    /**
     * Add an edge to this node.
     *
     * @param node The target node of the edge.
     * @param weight The weight of the edge.
     */
    public void addEdge(Node node, int weight) {
        Assert.check(node != this); // Self loops not allowed.
        Assert.check(weight > 0); // Require positive weight (negative is unsupported; no edge means zero weight).
        Integer prevWeight = edges.put(node, weight);
        Assert.check(prevWeight == null, prevWeight); // Multiple edges to the same node not allowed.
    }

    /**
     * Add an edge to this node, optionally, adding the reverse edge as well.
     *
     * @param node The target node of the edge.
     * @param weight The weight of the edge.
     * @param addReverse Whether to add the reverse edge as well ({@code true}) or only this edge ({@code false}).
     */
    public void addEdge(Node node, int weight, boolean addReverse) {
        addEdge(node, weight);
        if (addReverse) {
            node.addEdge(this, weight);
        }
    }

    /**
     * Returns the edges. Each edge consists of the edge target node and the weight of the edge.
     *
     * @return The edges.
     */
    public Set<Entry<Node, Integer>> edges() {
        return edges.entrySet();
    }

    /**
     * Returns the neighboring nodes.
     *
     * @return The neighboring nodes.
     */
    public Set<Node> neighbours() {
        return edges.keySet();
    }

    /**
     * Returns the degree of this node, which is the number of other nodes connect to this node via an edge.
     *
     * @return The degree.
     */
    public int degree() {
        return edges.size();
    }

    @Override
    public String toString() {
        return String.valueOf(index);
    }
}
