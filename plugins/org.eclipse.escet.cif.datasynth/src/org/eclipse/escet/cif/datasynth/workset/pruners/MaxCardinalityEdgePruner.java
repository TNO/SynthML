//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.workset.pruners;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.common.java.BitSets;

/** Edge pruner that keeps only those edges that have the highest cardinality dependency sets. */
public class MaxCardinalityEdgePruner extends EdgePruner {
    /** Per edge, the cardinality of its dependency set. */
    private final int[] cardinalities;

    /**
     * Constructor for the {@link MaxCardinalityEdgePruner} class.
     *
     * @param dependencies Per edge, its dependency set.
     */
    public MaxCardinalityEdgePruner(List<BitSet> dependencies) {
        cardinalities = dependencies.stream().mapToInt(BitSet::cardinality).toArray();
    }

    @Override
    public BitSet pruneInternal(BitSet workset) {
        int max = -1;
        for (int i: BitSets.iterateTrueBits(workset)) {
            int cardinality = cardinalities[i];
            if (cardinality < max) {
                workset.clear(i); // Not the maximum.
            } else if (cardinality > max) {
                max = cardinality;
                workset.clear(0, i); // New maximum, so all previous bits are not the maximum (anymore).
            }
        }
        return workset;
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        // No state to update.
    }
}
