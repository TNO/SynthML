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

import static org.eclipse.escet.cif.datasynth.varorder.graph.algos.RootedLevelStructureConstructor.constructRootedLevelStructure;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Sets.list2set;
import static org.eclipse.escet.common.java.Sets.set;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.junit.jupiter.api.Test;

/** Tests for {@link RootedLevelStructureConstructor}. */
public class RootedLevelStructureConstructorTest {
    /** Test rooted level structure construction, for a graph with a single node. */
    @Test
    public void testSingleNode() {
        Graph graph = new Graph(1);
        List<List<Node>> rls = constructRootedLevelStructure(graph, graph.node(0));
        assertEquals(1, rls.size());
        assertEquals(1, rls.get(0).size());
        assertEquals(set(0), rls.get(0).stream().map(n -> n.index).collect(Collectors.toSet()));
    }

    /** Test rooted level structure construction, for a graph with two partitions. */
    @Test
    public void testTwoPartitions() {
        Graph graph = new Graph(2);
        for (int i = 0; i < graph.size(); i++) {
            List<List<Node>> rls = constructRootedLevelStructure(graph, graph.node(i));
            assertEquals(1, rls.size());
            assertEquals(1, rls.get(0).size());
            assertEquals(set(i), rls.get(0).stream().map(n -> n.index).collect(Collectors.toSet()));
        }
    }

    /**
     * Test rooted level structure construction, based on example from Figures 1 and 2 of: Alan George and Joseph W. H.
     * Liu, "An Implementation of a Pseudoperipheral Node Finder", ACM Transactions on Mathematical Software, volume 5,
     * issue 3, pages 284-295, 1979, doi:<a href="https://doi.org/10.1145/355841.355845">10.1145/355841.355845</a>.
     */
    @Test
    public void testPaper() {
        // Create graph.
        Graph graph = new Graph(8);
        Node n1 = graph.node(0);
        Node n2 = graph.node(1);
        Node n3 = graph.node(2);
        Node n4 = graph.node(3);
        Node n5 = graph.node(4);
        Node n6 = graph.node(5);
        Node n7 = graph.node(6);
        Node n8 = graph.node(7);
        n2.addEdge(n1, 1, true);
        n1.addEdge(n6, 1, true);
        n6.addEdge(n8, 1, true);
        n8.addEdge(n3, 1, true);
        n3.addEdge(n5, 1, true);
        n8.addEdge(n4, 1, true);
        n4.addEdge(n7, 1, true);
        n7.addEdge(n3, 1, true);

        // Create rooted level structure for node 6, with node index 5.
        List<List<Node>> rls = constructRootedLevelStructure(graph, graph.node(5));
        assertEquals(4, rls.size());
        assertEquals(1, rls.get(0).size());
        assertEquals(2, rls.get(1).size());
        assertEquals(3, rls.get(2).size());
        assertEquals(2, rls.get(3).size());
        assertEquals(set(n6), list2set(rls.get(0)));
        assertEquals(set(n1, n8), list2set(rls.get(1)));
        assertEquals(set(n2, n3, n4), list2set(rls.get(2)));
        assertEquals(set(n5, n7), list2set(rls.get(3)));
    }

    /** Test rooted level structure construction hitting a width limit, for a graph with a single node. */
    @Test
    public void testWidthLimitSingleNode() {
        Graph graph = new Graph(1);
        Node node = graph.node(0);
        for (int i = -3; i <= 1; i++) {
            List<List<Node>> rls = constructRootedLevelStructure(graph, node, i);
            assertEquals(null, rls);
        }
        for (int i = 2; i <= 9; i++) {
            List<List<Node>> rls = constructRootedLevelStructure(graph, node, i);
            assertEquals(list(list(node)), rls);
        }
    }

    /** Test rooted level structure construction hitting a width limit, for a graph with a single node. */
    @Test
    public void testWidthLimitMultipleNodes() {
        // Create graph.
        Graph graph = new Graph(5);
        Node n0 = graph.node(0);
        Node n1 = graph.node(1);
        Node n2 = graph.node(2);
        Node n3 = graph.node(3);
        Node n4 = graph.node(4);
        n0.addEdge(n1, 1, true);
        n0.addEdge(n2, 1, true);
        n0.addEdge(n3, 1, true);
        n0.addEdge(n4, 1, true);

        // Rooted level structure from node 0 is [[0], [1,2,3,4]]. Maximum weight is thus 4.
        for (int i = -3; i <= 4; i++) {
            List<List<Node>> rls = constructRootedLevelStructure(graph, n0, i);
            assertEquals(null, rls);
        }
        for (int i = 5; i <= 9; i++) {
            List<List<Node>> rls = constructRootedLevelStructure(graph, n0, i);
            assertEquals(2, rls.size());
            assertEquals(list(n0), rls.get(0));
            assertEquals(set(n1, n2, n3, n4), list2set(rls.get(1)));
        }
    }
}
