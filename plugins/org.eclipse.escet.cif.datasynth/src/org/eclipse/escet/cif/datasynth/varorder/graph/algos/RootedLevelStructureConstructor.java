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
import static org.eclipse.escet.common.java.Lists.last;
import static org.eclipse.escet.common.java.Lists.list;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.datasynth.varorder.graph.Node;

/** Rooted level structure constructor. */
public class RootedLevelStructureConstructor {
    /** Constructor for the {@link RootedLevelStructureConstructor} class. */
    private RootedLevelStructureConstructor() {
        // Static class.
    }

    /**
     * Construct the rooted level structure rooted at a given node within a given graph.
     *
     * @param graph The graph.
     * @param root The root node.
     * @return The rooted level structure, as a list of nodes per level, from root level to deeper levels.
     */
    static List<List<Node>> constructRootedLevelStructure(Graph graph, Node root) {
        return constructRootedLevelStructure(graph, root, null);
    }

    /**
     * Construct the rooted level structure rooted at a given node within a given graph.
     *
     * @param graph The graph.
     * @param root The root node.
     * @param widthLimit The width limit in number of nodes of a level. If during construction of the rooted level
     *     structure the width of a level (its number of nodes) is exactly this limit or exceeds it, the rooted level
     *     structure construction is aborted. If {@code null}, no limit is set and construction is never aborted.
     * @return The rooted level structure, as a list of nodes per level, from root level to deeper levels. Is
     *     {@code null} if aborted.
     */
    static List<List<Node>> constructRootedLevelStructure(Graph graph, Node root, Integer widthLimit) {
        // Initialize the result with the first level.
        if (widthLimit != null && widthLimit <= 1) {
            return null;
        }
        List<List<Node>> result = list();
        result.add(list(root));

        // Initialize marked nodes, the nodes already in the result.
        BitSet marked = bitset(graph.size());
        marked.set(root.index);

        // Add more levels.
        while (true) {
            // Compute next level.
            List<Node> nextLevel = list();
            for (Node node: last(result)) { // Process nodes of last level.
                for (Node neighbour: node.neighbours()) { // Process neighbors.
                    if (!marked.get(neighbour.index)) { // Add neighbor to the next level, if not yet in the result.
                        nextLevel.add(neighbour);
                        marked.set(neighbour.index);
                    }
                }
            }

            // Abort if width is or exceeds the width limit.
            if (widthLimit != null && nextLevel.size() >= widthLimit) {
                return null;
            }

            // If level is empty, then we are done. Otherwise, we've found a next level and move on to the next one.
            if (nextLevel.isEmpty()) {
                break;
            }
            result.add(nextLevel);
        }

        // Return the rooted level structure.
        return result;
    }
}
