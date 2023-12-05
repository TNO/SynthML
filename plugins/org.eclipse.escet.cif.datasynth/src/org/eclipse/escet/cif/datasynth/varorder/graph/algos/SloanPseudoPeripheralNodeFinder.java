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

import static org.eclipse.escet.common.java.Lists.last;
import static org.eclipse.escet.common.java.Pair.pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Pair;

/**
 * Pseudo-peripheral node pair finder algorithm by Sloan.
 *
 * <p>
 * This algorithm is from: S. W. Sloan, "A FORTRAN program for profile and wavefront reduction", International Journal
 * for Numerical Methods in Engineering, volume 28, issue 11, pages 2651-2679, 1989,
 * doi:<a href="https://doi.org/10.1002/nme.1620281111">10.1002/nme.1620281111</a>.
 * </p>
 */
public class SloanPseudoPeripheralNodeFinder implements PseudoPeripheralNodePairFinder {
    @Override
    public Node findPseudoPeripheralNode(Graph graph, List<Node> partition, Node startNode) {
        return findPseudoPeripheralNodePair(graph, partition, startNode).left;
    }

    @Override
    public Pair<Node, Node> findPseudoPeripheralNodePair(Graph graph, List<Node> partition, Node startNode) {
        // Based on 'DIAMTR' subroutine from the paper by Sloan.
        // Steps refer to the algorithm steps as described on page 2654 of the paper.

        // Step 1 (First guess for starting node): Scan all nodes in the partition and select a node 's' with the
        // smallest degree.
        //
        // Unlike the paper, we allow an explicitly given start node to be specified, overriding step 1.
        Node s = (startNode != null) ? startNode : partition.stream().min(Comparator.comparing(Node::degree)).get();

        // Step 2 (Generate rooted level structure): Form the level structure rooted as node 's'.
        List<List<Node>> rlsS = RootedLevelStructureConstructor.constructRootedLevelStructure(graph, s);

        // Repeated steps 3-6.
        Node e = null;
        LOOP:
        while (true) {
            // Step 3 (Sort the last level): Sort the nodes in the last level in ascending sequence of their degrees.
            // Step 4 (Shrink the last level): Scan the sorted last level and form a list of nodes 'q' containing only
            // one node of each degree.
            List<Node> lastLevel = last(rlsS);
            List<Node> q = lastLevel.stream().collect(Collectors.groupingBy(Node::degree)).entrySet().stream()
                    .sorted(Comparator.comparing(Entry::getKey)) // Sort on ascending degree.
                    .map(entry -> entry.getValue().get(0)) // Take first node for each degree.
                    .collect(Collectors.toList());

            // Step 5 (Initialize): Set 'w(e)' = infinite.
            int we = Integer.MAX_VALUE;
            e = null;

            // Step 6 (Test for termination): For each node 'i' in 'q', in order of ascending degree, generate its
            // rooted level structure. If h(i) > h(s) and w(i) < w(e), set 's' to 'i' and go to step 3. Else, if
            // w(i) < w(e), set 'e' to 'i' and w(e) to w(i).
            //
            // We use the same optimization as in the paper, aborting the construction of rooted level structures if
            // w(i) < w(e) does not hold.
            for (Node i: q) {
                List<List<Node>> rlsI = RootedLevelStructureConstructor.constructRootedLevelStructure(graph, i, we);
                if (rlsI != null) {
                    // w(i) < w(e), by rooted level structure construction width limit.
                    if (rlsI.size() > rlsS.size()) {
                        s = i;
                        rlsS = rlsI;
                        continue LOOP;
                    } else {
                        e = i;
                        we = rlsI.stream().max(Comparator.comparing(List::size)).get().size(); // Maximum level width.
                    }
                }
            }
            break;
        }

        // Step 7 (Exit): exit with starting node 's' and end node 'e'.
        Assert.notNull(e);
        return pair(s, e);
    }
}
