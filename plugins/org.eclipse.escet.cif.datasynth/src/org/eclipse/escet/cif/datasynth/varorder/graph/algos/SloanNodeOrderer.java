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
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Maps.mapc;
import static org.eclipse.escet.common.java.Pair.pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Pair;

/**
 * Sloan profile/wavefront-reducing node ordering heuristic.
 *
 * <p>
 * This algorithm is from: S. W. Sloan, "A FORTRAN program for profile and wavefront reduction", International Journal
 * for Numerical Methods in Engineering, volume 28, issue 11, pages 2651-2679, 1989,
 * doi:<a href="https://doi.org/10.1002/nme.1620281111">10.1002/nme.1620281111</a>.
 * </p>
 */
public class SloanNodeOrderer extends NodeOrderer {
    @Override
    protected List<Node> orderNodesPartition(Graph graph, List<Node> partition) {
        // Based on 'NUMBER' subroutine from the paper by Sloan.
        // Steps refer to the algorithm steps as described on page 2656 of the paper.

        // Step 1 (Entry): Enter with the endpoints of a pseudo-diameter, nodes 's' and 'e'.
        PseudoPeripheralNodePairFinder pairFinder = new SloanPseudoPeripheralNodeFinder();
        Pair<Node, Node> endpoints = pairFinder.findPseudoPeripheralNodePair(graph, partition, null);
        Node s = endpoints.left;
        Node e = endpoints.right;

        // Step 2 (Compute distances): Construct the level structure rooted at 'e', and compute the distance d[i] of
        // each node 'i' from 'e'. Note that if node 'i' belongs to level 'j', with 1 <= j <= h(e), then dd[i] = j - 1.
        //
        // Unlike the paper, we iterate over levels 'j' with 0 <= j < h(e) instead, and thus dd[i] = j.
        List<List<Node>> rlsE = RootedLevelStructureConstructor.constructRootedLevelStructure(graph, e);
        Map<Node, Integer> dd = mapc(partition.size());
        for (int j = 0; j < rlsE.size(); j++) {
            for (Node i: rlsE.get(j)) {
                dd.put(i, j);
            }
        }

        // Step 3 (Assign initial status and priority): Assign each node in the partition an inactive status and compute
        // its initial priority, p[i], according to p[i] = W1 * dd[i] - W2 * (d[i] + 1), where W1 and W2 are integer
        // weights and d[i] is the degree of node 'i'.
        //
        // Similar to the paper, we use W1 = 1 and W2 = 2, with W2 >= W1.
        final int W1 = 1;
        final int W2 = 2;
        Map<Node, Status> status = mapc(partition.size());
        Map<Node, Integer> p = mapc(partition.size());
        for (Node i: partition) {
            status.put(i, Status.INACTIVE);
            p.put(i, W1 * dd.get(i) - W2 * (i.degree() + 1));
        }

        // Step 4 (Initialize node count and priority queue): Initialize the resulting node order to an empty list.
        // Assign node 's' a preactive status. Let 'q' denote a priority queue of length 'n'. Insert 's' in the priority
        // queue, such that n = 1 and q[n] = s.
        List<Node> result = listc(partition.size());
        status.put(s, Status.PREACTIVE);
        List<Node> q = list();
        q.add(s);

        // Step 5 (Test for termination): While the priority queue is not empty, do steps 6-9.
        while (!q.isEmpty()) {
            // Step 6 (Select node to be labeled): Search the priority queue and locate the node 'i' which has the
            // maximum priority according to p[i] = max{p[j]} for each 'j' in 'q'. Let 'm' be the index of node 'i'
            // such that q[m] = i.
            int m = IntStream.range(0, q.size()).mapToObj(i -> pair(i, p.get(q.get(i))))
                    .max(Comparator.comparing(pair -> pair.right)).get().left;
            Node i = q.get(m);

            // Step 7 (Update queue and priorities): Delete node 'i' from the priority queue by setting q[m] = q[n]
            // and decrementing 'n' according to n = n - 1. If node 'i' is not preactive go to step 8. Else, examine
            // each node 'j' which is adjacent to node 'i' and increment its priority according to p[j] = p[j] + W2.
            // This corresponds to decreasing the current degree of node 'j' by unity. If node 'j' is inactive, then
            // insert it in the priority queue with a preactive status by setting n = n + 1 and q[n] = j.
            int n = q.size() - 1;
            q.set(m, q.get(n));
            q.remove(n);
            if (status.get(i) == Status.PREACTIVE) {
                for (Node j: i.neighbours()) {
                    p.put(j, p.get(j) + W2);
                    if (status.get(j) == Status.INACTIVE) {
                        status.put(j, Status.PREACTIVE);
                        q.add(j);
                    }
                }
            }

            // Step 8 (Label the next node): Add node 'i' to the result. Assign it a postactive status.
            result.add(i);
            status.put(i, Status.POSTACTIVE);

            // Step 9 (Update priorities and queue): Examine each node 'j' which is adjacent to node 'i'. If node 'j' is
            // not preactive, take no action. Else, assign node 'j' an active status, set p[j] = p[j] + W2, and examine
            // each node 'k' which is adjacent to node 'j'. If node 'k' is not postactive, increment its priority
            // according to p[k] = p[k] + W2. If node 'k' is inactive, insert it in the priority queue with a preactive
            // status by setting n = n + 1 and q[n] = k.
            for (Node j: i.neighbours()) {
                if (status.get(j) == Status.PREACTIVE) {
                    status.put(j, Status.ACTIVE);
                    p.put(j, p.get(j) + W2);
                    for (Node k: j.neighbours()) {
                        if (status.get(k) != Status.POSTACTIVE) {
                            p.put(k, p.get(k) + W2);
                            if (status.get(k) == Status.INACTIVE) {
                                status.put(k, Status.PREACTIVE);
                                q.add(k);
                            }
                        }
                    }
                }
            }
        }

        // Step 10 (Exit): Exit with the new node order.
        Assert.areEqual(partition.size(), result.size());
        return result;
    }

    /** Node status. */
    private static enum Status {
        /** Postactive node. Has been ordered. */
        POSTACTIVE,

        /** Active node. Is adjacent to a postactive node, but does not have a postactive status. */
        ACTIVE,

        /** Preactive node. Is adjacent to an active node, but is not postactive or active. */
        PREACTIVE,

        /** Inactive node. Is not postactive, active or preactive. */
        INACTIVE;
    }
}
