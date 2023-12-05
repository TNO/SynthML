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

import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for {@link Graph}. */
public class GraphTest {
    @SuppressWarnings("javadoc")
    @Test
    public void testEdgeCount() {
        String text = String.join("\n", list( //
                ".1.", //
                "1..", //
                "23."));
        Graph graph = GraphTestUtil.fromString(text);
        assertEquals(3, graph.edgeCount()); // 1 undirected edge, 2 directed edges.
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testPartitionBasic() {
        String text = String.join("\n", list( //
                ".7...8", //
                "7..5..", //
                ".....4", //
                ".5..3.", //
                "...3.2", //
                "8.4.2."));
        Graph graph = GraphTestUtil.fromString(text);
        List<List<Node>> partitions = graph.partition();
        assertEquals(1, partitions.size());
        assertEquals(graph.nodes, partitions.get(0));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testPartitionUnconnectedNodes() {
        String text = String.join("\n", list( //
                ".....", //
                "...1.", //
                ".....", //
                ".1...", //
                "....."));
        Graph graph = GraphTestUtil.fromString(text);
        List<List<Node>> partitions = graph.partition();
        assertEquals(4, partitions.size());
        assertEquals(list(graph.node(0)), partitions.get(0));
        assertEquals(list(graph.node(1), graph.node(3)), partitions.get(1));
        assertEquals(list(graph.node(2)), partitions.get(2));
        assertEquals(list(graph.node(4)), partitions.get(3));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testPartitionThreePartitions() {
        String text = String.join("\n", list( //
                ".1....", //
                "1.....", //
                "...2..", //
                "..2...", //
                ".....3", //
                "....3."));
        Graph graph = GraphTestUtil.fromString(text);
        List<List<Node>> partitions = graph.partition();
        assertEquals(3, partitions.size());
        assertEquals(list(graph.node(0), graph.node(1)), partitions.get(0));
        assertEquals(list(graph.node(2), graph.node(3)), partitions.get(1));
        assertEquals(list(graph.node(4), graph.node(5)), partitions.get(2));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testReorder() {
        String text = String.join("\n", list( //
                ".12345", //
                "1....6", //
                "2....7", //
                "3....8", //
                "4....9", //
                "56789."));
        Graph graph = GraphTestUtil.fromString(text);
        List<Node> newOrder = list(graph.node(1), graph.node(4), graph.node(0), graph.node(5), graph.node(3),
                graph.node(2));
        Graph newGraph = graph.reorder(newOrder);
        String newText = newGraph.toString();
        String expectedText = String.join("\n", list( //
                "..16..", //
                "..49..", //
                "14.532", //
                "695.87", //
                "..38..", //
                "..27.."));
        assertEquals(expectedText, newText);
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testToFromStringRoundTrip() {
        String text1 = String.join("\n", list( //
                ".3.567", //
                "3.1...", //
                ".1.3.5", //
                "5.3.84", //
                "6..8..", //
                "7.54.."));
        Graph graph1 = GraphTestUtil.fromString(text1);
        String text2 = graph1.toString();
        Graph graph2 = GraphTestUtil.fromString(text1);
        String text3 = graph2.toString();
        assertEquals(text1, text2);
        assertEquals(text2, text3);
    }
}
