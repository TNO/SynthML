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

import org.eclipse.escet.common.java.Assert;

/** Edge pruner for the edge workset algorithm. */
public abstract class EdgePruner {
    /**
     * Prune edges from a non-empty workset based on some heuristic.
     *
     * @param workset The non-empty workset. May be modified in-place.
     * @return The non-empty pruned workset.
     */
    public BitSet prune(BitSet workset) {
        BitSet pruned = pruneInternal(workset);
        Assert.check(!pruned.isEmpty());
        return pruned;
    }

    /**
     * Prune edges from a non-empty workset based on some heuristic.
     *
     * @param workset The non-empty workset. May be modified in-place.
     * @return The pruned workset. Must not be empty.
     */
    protected abstract BitSet pruneInternal(BitSet workset);

    /**
     * Update the state of the edge pruner, based on the result of applying the edge.
     *
     * @param edgeIdx The index of the edge that was applied.
     * @param hadAnEffect Whether applying the edge had an effect (at least one new state was reached).
     */
    public abstract void update(int edgeIdx, boolean hadAnEffect);
}
