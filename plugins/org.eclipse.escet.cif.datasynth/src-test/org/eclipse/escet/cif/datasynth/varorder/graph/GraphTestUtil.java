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

import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.Arrays;
import java.util.List;

import org.eclipse.escet.common.java.Assert;

/** Graph test utility, with methods to support graph-related tests. */
public class GraphTestUtil {
    /** Constructor for the {@link GraphTestUtil} class. */
    private GraphTestUtil() {
        // Static class.
    }

    /**
     * Constructs a graph from the given text.
     *
     * @param text The text. Each line is considered a row. Each character is an element in the row. A '{@code .}' is
     *     interpreted as zero. Any other character must be a single digit positive weight value.
     * @return The graph.
     * @throws AssertionError If the text is not valid.
     * @throws IllegalArgumentException If the text is not valid.
     */
    public static Graph fromString(String text) {
        List<String> lines = Arrays.asList(text.replace("\r", "").split("\n"));
        Graph graph = new Graph(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Assert.areEqual(lines.size(), line.length());
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);

                // Get weight.
                int weight;
                if (c == '.') {
                    weight = 0;
                } else if (c >= '1' && c <= '9') {
                    weight = c - '0';
                } else {
                    throw new IllegalArgumentException(fmt("Invalid weight \"%s\".", c));
                }

                // Add edge if non-zero weight.
                if (i == j) { // Diagonal.
                    Assert.areEqual(0, weight); // Diagonal must be empty.
                } else if (weight > 0) {
                    graph.nodes.get(i).addEdge(graph.nodes.get(j), weight);
                }
            }
        }
        return graph;
    }
}
