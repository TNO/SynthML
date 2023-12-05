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

import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.GraphTestUtil;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.junit.jupiter.api.Test;

/** Base class for {@link NodeOrderer} tests. */
public abstract class NodeOrdererTest {
    /**
     * Create a node ordering algorithm.
     *
     * @return The node ordering algorithm.
     */
    protected abstract NodeOrderer createNodeOrderer();

    /** Test node ordering for a simple connected graph. */
    @Test
    public void testSimple() {
        String text = String.join("\n", list( //
                ".7...8", //
                "7..5..", //
                ".....4", //
                ".5..3.", //
                "...3.2", //
                "8.4.2."));
        Graph graph = GraphTestUtil.fromString(text);
        List<Node> newOrder = createNodeOrderer().orderNodes(graph);
        Graph newGraph = graph.reorder(newOrder);
        String newText = newGraph.toString();
        String expectedText = getSimpleExpectedGraph();
        assertEquals(expectedText, newText);
    }

    /**
     * Returns the textual representation of the expected reordered graph for {@link #testSimple}.
     *
     * @return The textual representation of the expected reordered graph.
     */
    protected abstract String getSimpleExpectedGraph();

    /** Test node ordering for unconnected nodes. */
    @Test
    public void testUnconnectedNodes() {
        String text = String.join("\n", list( //
                ".....", //
                "...1.", //
                ".....", //
                ".1...", //
                "....."));
        Graph graph = GraphTestUtil.fromString(text);
        List<Node> newOrder = createNodeOrderer().orderNodes(graph);
        List<Node> expectedOrder = list(graph.node(1), graph.node(3), graph.node(0), graph.node(2), graph.node(4));
        assertEquals(expectedOrder, newOrder);
        Graph newGraph = graph.reorder(newOrder);
        String newText = newGraph.toString();
        String expectedText = String.join("\n", list( //
                ".1...", //
                "1....", //
                ".....", //
                ".....", //
                "....."));
        assertEquals(expectedText, newText);
    }

    /** Test node ordering for three partitions (disjoint sub-graphs). */
    @Test
    public void testThreePartitions() {
        String text = String.join("\n", list( //
                ".1....", //
                "1.....", //
                "...2..", //
                "..2...", //
                ".....3", //
                "....3."));
        Graph graph = GraphTestUtil.fromString(text);
        List<Node> newOrder = createNodeOrderer().orderNodes(graph);
        Graph newGraph = graph.reorder(newOrder);
        String newText = newGraph.toString();
        assertEquals(text, newText);
    }

    /**
     * Test node ordering, based on example from Figure 3 of: S. W. Sloan, "A FORTRAN program for profile and wavefront
     * reduction", International Journal for Numerical Methods in Engineering, volume 28, issue 11, pages 2651-2679,
     * 1989, doi:<a href="https://doi.org/10.1002/nme.1620281111">10.1002/nme.1620281111</a>.
     */
    @Test
    public void testPaperSloan89() {
        // Create graph. The paper does not give weights, so these are invented here.
        // Note that the paper does not indicate the outcome of ordering this graph.
        Graph graph = new Graph(8);
        Node n0 = graph.node(0);
        Node n1 = graph.node(1);
        Node n2 = graph.node(2);
        Node n3 = graph.node(3);
        Node n4 = graph.node(4);
        Node n5 = graph.node(5);
        Node n6 = graph.node(6);
        Node n7 = graph.node(7);
        n0.addEdge(n2, 1, true);
        n2.addEdge(n1, 2, true);
        n2.addEdge(n3, 3, true);
        n1.addEdge(n3, 4, true);
        n1.addEdge(n4, 5, true);
        n3.addEdge(n4, 6, true);
        n3.addEdge(n5, 7, true);
        n3.addEdge(n6, 8, true);
        n4.addEdge(n6, 9, true);
        n5.addEdge(n7, 10, true);
        n6.addEdge(n7, 11, true);

        // Test node ordering.
        List<Node> newOrder = createNodeOrderer().orderNodes(graph);
        Graph newGraph = graph.reorder(newOrder);
        String newText = newGraph.toString();
        String expectedText = getPaperSloan89ExpectedGraph();
        assertEquals(expectedText, newText);
    }

    /**
     * Returns the textual representation of the expected reordered graph for {@link #testPaperSloan89}.
     *
     * @return The textual representation of the expected reordered graph.
     */
    protected abstract String getPaperSloan89ExpectedGraph();
}
