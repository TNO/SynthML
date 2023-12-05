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

/** Weighted Event Span (WES) metric. */
public class WesMetric implements VarOrderMetric {
    @Override
    public double computeForNewIndices(int[] newIndices, List<BitSet> hyperEdges) {
        return compute(newIndices, hyperEdges);
    }

    /**
     * Compute the Weighted Event Span (WES) metric.
     *
     * @param newIndices For each variable, its new 0-based index.
     * @param hyperEdges The hyper-edges to use to compute the WES.
     * @return The Weighted Event Span (WES).
     */
    public static double compute(int[] newIndices, List<BitSet> hyperEdges) {
        // This method is based on formula 7 from: Sam Lousberg, Sander Thuijsman and Michel Reniers, "DSM-based
        // variable ordering heuristic for reduced computational effort of symbolic supervisor synthesis",
        // IFAC-PapersOnLine, volume 53, issue 4, pages 429-436, 2020, https://doi.org/10.1016/j.ifacol.2021.04.058.
        //
        // The formula is: WES = SUM_{e in E} (2 * x_b) / |x| * (x_b - x_t + 1) / (|x| * |E|)
        // Where:
        // 1) 'E' is the set of edges. We use the hyper-edges.
        // 2) 'x' the current-state variables. We use the synthesis variables.
        // 3) 'x_b'/'x_t' the indices of the bottom/top BDD-variable in 'T_e(X)', the transition relation of edge 'e'.
        // Note that we use hyper-edges as edges. Also, variables in the variable order with lower indices are higher
        // (less deep, closer to the root) in the BDDs, while variables with higher indices are lower (deeper, closer
        // to the leafs) in the BDDs. Therefore, we use for each hyper-edge: the highest index of a variable with an
        // enabled bit in that hyper-edge as 'x_b', and the lowest index of a variable with an enabled bit in that
        // hyper-edge as 'x_t'.

        double nx = newIndices.length;
        double nE = hyperEdges.size();
        if (nx == 0 || nE == 0) {
            return 0;
        }

        double wes = 0;
        for (BitSet edge: hyperEdges) {
            // Compute 'x_t' and 'x_b' for this edge.
            int xT = Integer.MAX_VALUE;
            int xB = 0;
            for (int i: BitSets.iterateTrueBits(edge)) {
                int newIdx = newIndices[i];
                xT = Math.min(xT, newIdx);
                xB = Math.max(xB, newIdx);
            }

            // Update WES for this edge.
            wes += (2 * xB) / nx * (xB - xT + 1) / (nx * nE);
        }
        return wes;
    }
}
