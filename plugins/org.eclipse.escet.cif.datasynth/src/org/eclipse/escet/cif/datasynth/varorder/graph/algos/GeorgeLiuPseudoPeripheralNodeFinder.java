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

import java.util.Comparator;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;

/**
 * Pseudo-peripheral node finder algorithm by George and Liu.
 *
 * <p>
 * This algorithm is from: Alan George and Joseph W. H. Liu, "An Implementation of a Pseudoperipheral Node Finder", ACM
 * Transactions on Mathematical Software, volume 5, issue 3, pages 284-295, 1979,
 * doi:<a href="https://doi.org/10.1145/355841.355845">10.1145/355841.355845</a>.
 * </p>
 */
public class GeorgeLiuPseudoPeripheralNodeFinder implements PseudoPeripheralNodeFinder {
    @Override
    public Node findPseudoPeripheralNode(Graph graph, List<Node> partition, Node startNode) {
        // Based on 'FNROOT' subroutine from the paper by George and Liu.
        // Steps refer to the algorithm steps as described on page 288 of the paper.

        // Step 1 (Initialization): Choose an arbitrary node 'r' in the partition.
        Node r = (startNode != null) ? startNode : partition.get(0);

        // Step 2 (Generation of level structure): Construct the rooted level structure at 'r'.
        List<List<Node>> rlsR = RootedLevelStructureConstructor.constructRootedLevelStructure(graph, r);

        // Repeated steps 3 and 4.
        while (true) {
            // Optimization: If there is only one level, or as many levels as there are nodes, we are done.
            // This optimization works best for connected graphs.
            if (rlsR.size() == 1 || rlsR.size() == graph.size()) {
                break;
            }

            // Step 3 (Shrinking): Shrink the last level according to shrinking strategy S2: Select a node 'x' of
            // minimum degree from the last level.
            List<Node> lastLevel = last(rlsR);
            Node x = lastLevel.stream().min(Comparator.comparing(Node::degree)).get();

            // Step 4: For each node 'x' in the (shrunk) last level (for S2 there is only one), generate its rooted
            // level structure. If l(x) > l(r), then 'x' becomes the new 'r' and we go to step 3. Otherwise, we proceed
            // to step 5.
            List<List<Node>> rlsX = RootedLevelStructureConstructor.constructRootedLevelStructure(graph, x);
            if (rlsX.size() <= rlsR.size()) {
                break;
            }
            r = x;
            rlsR = rlsX;
        }

        // Step 5 (Exit): Return the determined pseudo-peripheral node.
        return r;
    }
}
