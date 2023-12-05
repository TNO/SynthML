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

package org.eclipse.escet.cif.datasynth.varorder.metrics;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.common.java.BitSets;

/** Total span metric. */
public class TotalSpanMetric implements VarOrderMetric {
    @Override
    public double computeForNewIndices(int[] newIndices, List<BitSet> hyperEdges) {
        return compute(newIndices, hyperEdges);
    }

    /**
     * Compute the total span metric.
     *
     * @param newIndices For each variable, its new 0-based index.
     * @param hyperEdges The hyper-edges to use to compute the total span.
     * @return The total span.
     */
    public static long compute(int[] newIndices, List<BitSet> hyperEdges) {
        // Total span is the sum of the span of the edges.
        long totalSpan = 0;
        for (BitSet edge: hyperEdges) {
            // Get minimum and maximum index of the vertices of the edge.
            int minIdx = Integer.MAX_VALUE;
            int maxIdx = 0;
            for (int i: BitSets.iterateTrueBits(edge)) {
                int newIdx = newIndices[i];
                minIdx = Math.min(minIdx, newIdx);
                maxIdx = Math.max(maxIdx, newIdx);
            }

            // Get span of the edge and update total span.
            int span = maxIdx - minIdx;
            totalSpan += span;
        }
        return totalSpan;
    }
}
