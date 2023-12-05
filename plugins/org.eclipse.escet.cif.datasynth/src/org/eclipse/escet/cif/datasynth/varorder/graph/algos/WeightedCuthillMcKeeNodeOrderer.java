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

import static org.eclipse.escet.common.java.BitSets.bitset;
import static org.eclipse.escet.common.java.Lists.listc;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Assert;

/**
 * Weighted Cuthill-McKee bandwidth-reducing node ordering heuristic.
 *
 * <p>
 * This algorithm is based on Algorithm 1 from: Sam Lousberg, Sander Thuijsman and Michel Reniers, "DSM-based variable
 * ordering heuristic for reduced computational effort of symbolic supervisor synthesis", IFAC-PapersOnLine, volume 53,
 * issue 4, pages 429-436, 2020,
 * doi:<a href="https://doi.org/10.1016/j.ifacol.2021.04.058">10.1016/j.ifacol.2021.04.058</a>.
 * </p>
 */
public class WeightedCuthillMcKeeNodeOrderer extends NodeOrderer {
    /** The pseudo-peripheral node finder to use. */
    private final PseudoPeripheralNodeFinder nodeFinder;

    /**
     * Constructor for the {@link WeightedCuthillMcKeeNodeOrderer} class.
     *
     * @param nodeFinder The pseudo-peripheral node finder to use.
     */
    public WeightedCuthillMcKeeNodeOrderer(PseudoPeripheralNodeFinder nodeFinder) {
        this.nodeFinder = nodeFinder;
    }

    @Override
    protected List<Node> orderNodesPartition(Graph graph, List<Node> partition) {
        // In the paper by Lousberg et al, unconnected nodes in the graph (nodes without any edges) are treated as a
        // special case. It is assumed that the remaining nodes together form a connected graph. However, this may not
        // always be the case. It could be that we have four nodes, and that the first two are connected via an edge,
        // and the last two are connected via an edge, but the first two and last two are not connected in any way with
        // each other. Hence, we have two unconnected sub-graphs that partition the graph. The algorithm in the paper
        // does not account for this. It would determine pseudo-peripheral nodes for one partition, and order the
        // variables in that partition only. It does not consider what to do with the other partition.
        //
        // Unlike the paper, this implementation first partitions the graph into connected sub-graphs (this is done in
        // the base class). It then applies the algorithm for each partition (this method). Unconnected nodes are then
        // simply partitions with a single node, and don't need to be considered as a special case. To ensure that the
        // unconnected nodes are still at the end of the produced ordering, we sort the partitions on descending number
        // of nodes (in the base class).

        // Line numbers refer to the lines in Algorithm 1 of the paper.

        // Line 1:
        // - Initialize the result 'R'.
        // - The graph is already given.
        // - The list of unconnected nodes 'E' is replaced by partitions.
        List<Node> vR = listc(partition.size());

        // Line 2: Compute pseudo-peripheral node 'p'.
        Node p = nodeFinder.findPseudoPeripheralNode(graph, partition, null);

        // Line 3: Mark 'p' and append 'p' to 'R'.
        BitSet marked = bitset(graph.size());
        marked.set(p.index);
        vR.add(p);

        // Initialize 'ri' to the index of the 'next node in R' ('p' in our case), to later be used by line 8.
        int ri = 0;

        // Lines 4-9: While unmarked nodes exist.
        while (marked.cardinality() < partition.size()) {
            // Lines 5+6: Find list of unmarked neighbors 'C' of 'p' and sort 'C' in descending weight. Sort nodes
            // in 'C' with equal weight in ascending degree.
            List<Entry<Node, Integer>> vC = p.edges().stream().filter(e -> !marked.get(e.getKey().index))
                    .collect(Collectors.toList());
            Collections.sort(vC, Comparator.comparingInt((Entry<Node, Integer> e) -> e.getValue()).reversed()
                    .thenComparing(Comparator.comparingInt(e -> e.getKey().degree())));

            // Line 7: Append 'C' to 'R' and mark all nodes in 'C'.
            for (Entry<Node, Integer> e: vC) {
                Node c = e.getKey();
                vR.add(c);
                Assert.check(!marked.get(c.index));
                marked.set(c.index);
            }

            // Line 8: Set the next node in 'R' as 'p'.
            ri++;
            p = vR.get(ri);
        }

        // Line 10: No need to handle unconnected edges here, as they are handled as single-node partitions.

        // Return the resulting node order.
        return vR;
    }
}
