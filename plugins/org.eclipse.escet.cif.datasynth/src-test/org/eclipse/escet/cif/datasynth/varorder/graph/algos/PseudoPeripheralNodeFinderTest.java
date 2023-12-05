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
import static org.eclipse.escet.common.java.Pair.pair;
import static org.eclipse.escet.common.java.Sets.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Pair;
import org.junit.jupiter.api.Test;

/** Base class for {@link PseudoPeripheralNodeFinder} tests. */
public abstract class PseudoPeripheralNodeFinderTest {
    /** The pseudo-peripheral node finder algorithm to test. */
    private final PseudoPeripheralNodeFinder nodeFinder;

    /** Constructor for the {@link PseudoPeripheralNodeFinderTest} class. */
    public PseudoPeripheralNodeFinderTest() {
        nodeFinder = createPseudoPeripheralNodeFinder();
    }

    /**
     * Create a pseudo-peripheral node finder algorithm.
     *
     * @return The pseudo-peripheral node finder algorithm.
     */
    protected abstract PseudoPeripheralNodeFinder createPseudoPeripheralNodeFinder();

    /** Test finding a pseudo-peripheral nodes, for a graph with a single node. */
    @Test
    public void testSingleNode() {
        // Create graph.
        Graph graph = new Graph(1);
        Node node = graph.node(0);

        // Test finding a pseudo-peripheral node.
        Node node2 = nodeFinder.findPseudoPeripheralNode(graph, graph.nodes, null);
        assertEquals(node, node2);

        // Test finding a pseudo-peripheral node pair.
        if (nodeFinder instanceof PseudoPeripheralNodePairFinder) {
            PseudoPeripheralNodePairFinder pairFinder = (PseudoPeripheralNodePairFinder)nodeFinder;
            Pair<Node, Node> pair = pairFinder.findPseudoPeripheralNodePair(graph, graph.nodes, null);
            assertEquals(pair(node, node), pair);
        }
    }

    /** Test finding a pseudo-peripheral nodes, for a graph with two partitions, each with a single node. */
    @Test
    public void testTwoPartitionsSingleNode() {
        // Create graph.
        Graph graph = new Graph(2);

        // Test per partition.
        for (int i = 0; i < graph.size(); i++) {
            // Test finding a pseudo-peripheral node.
            Node node = nodeFinder.findPseudoPeripheralNode(graph, list(graph.node(i)), null);
            assertSame(graph.node(i), node);

            // Test finding a pseudo-peripheral node pair.
            if (nodeFinder instanceof PseudoPeripheralNodePairFinder) {
                PseudoPeripheralNodePairFinder pairFinder = (PseudoPeripheralNodePairFinder)nodeFinder;
                Pair<Node, Node> pair = pairFinder.findPseudoPeripheralNodePair(graph, list(graph.node(i)), null);
                assertSame(graph.node(i), pair.left);
                assertSame(graph.node(i), pair.right);
            }
        }
    }

    /**
     * Test finding a pseudo-peripheral nodes, for a graph with two partitions, each with a two nodes connected in
     * sequence.
     */
    @Test
    public void testTwoPartitionsSequence() {
        // Create graph (n0-n1-n2, n3-n4-n5).
        Graph graph = new Graph(6);
        Node n0 = graph.node(0);
        Node n1 = graph.node(1);
        Node n2 = graph.node(2);
        Node n3 = graph.node(3);
        Node n4 = graph.node(4);
        Node n5 = graph.node(5);
        n0.addEdge(n1, 1, true);
        n1.addEdge(n2, 1, true);
        n3.addEdge(n4, 1, true);
        n4.addEdge(n5, 1, true);

        // Create partitions.
        List<List<Node>> partitions = graph.partition();

        // 1) Test first partition: nodes 0 and 2 are peripheral nodes. Test for each node in the partition (n0-n2).
        Set<Node> peripheralNodes = set(n0, n2);

        for (int i = 0; i < 3; i++) {
            // 1a) Test finding a pseudo-peripheral node.
            Node node = nodeFinder.findPseudoPeripheralNode(graph, partitions.get(0), graph.node(i));
            assertTrue(peripheralNodes.contains(node));

            // 1b) Test finding a pseudo-peripheral node pair.
            if (nodeFinder instanceof PseudoPeripheralNodePairFinder) {
                PseudoPeripheralNodePairFinder pairFinder = (PseudoPeripheralNodePairFinder)nodeFinder;
                Pair<Node, Node> pair = pairFinder.findPseudoPeripheralNodePair(graph, partitions.get(0),
                        graph.node(i));
                assertEquals(peripheralNodes, set(pair.left, pair.right));
            }
        }

        // 2) Test second partition: nodes 3 and 5 are peripheral nodes. Test for each node in the partition (n3-n5).
        peripheralNodes = set(n3, n5);

        for (int i = 3; i < 6; i++) {
            // 2a) Test finding a pseudo-peripheral node.
            Node node = nodeFinder.findPseudoPeripheralNode(graph, partitions.get(1), graph.node(i));
            assertTrue(peripheralNodes.contains(node));

            // 2b) Test finding a pseudo-peripheral node pair.
            if (nodeFinder instanceof PseudoPeripheralNodePairFinder) {
                PseudoPeripheralNodePairFinder pairFinder = (PseudoPeripheralNodePairFinder)nodeFinder;
                Pair<Node, Node> pair = pairFinder.findPseudoPeripheralNodePair(graph, partitions.get(1),
                        graph.node(i));
                assertEquals(peripheralNodes, set(pair.left, pair.right));
            }
        }
    }

    /**
     * Test finding a pseudo-peripheral nodes, based on example from Figure 1 of: Alan George and Joseph W. H. Liu, "An
     * Implementation of a Pseudoperipheral Node Finder", ACM Transactions on Mathematical Software, volume 5, issue 3,
     * pages 284-295, 1979, doi:<a href="https://doi.org/10.1145/355841.355845">10.1145/355841.355845</a>.
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

        // Nodes 2, 5 and 7 are peripheral nodes.
        Set<Node> expectedNodes = set(n2, n5, n7);
        Set<Pair<Node, Node>> expectedPairs = set();
        for (Node left: set(n2, n5, n7)) {
            for (Node right: set(n2, n5, n7)) {
                expectedPairs.add(pair(left, right));
            }
        }

        // Test with each node in the graph as start node.
        for (int i = 0; i < graph.size(); i++) {
            // Test finding a pseudo-peripheral node.
            Node node = nodeFinder.findPseudoPeripheralNode(graph, graph.nodes, graph.node(i));
            assertTrue(expectedNodes.contains(node), node.toString());

            // Test finding a pseudo-peripheral node pair.
            if (nodeFinder instanceof PseudoPeripheralNodePairFinder) {
                PseudoPeripheralNodePairFinder pairFinder = (PseudoPeripheralNodePairFinder)nodeFinder;
                Pair<Node, Node> pair = pairFinder.findPseudoPeripheralNodePair(graph, graph.nodes, graph.node(i));
                assertTrue(expectedPairs.contains(pair), pair.toString());
            }
        }
    }
}
