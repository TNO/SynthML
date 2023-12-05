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

package org.eclipse.escet.cif.datasynth.workset.selectors;

import java.util.BitSet;

import org.eclipse.escet.common.java.Assert;

/** Edge selector for the edge workset algorithm. */
public abstract class EdgeSelector {
    /**
     * Select an edge from a non-empty workset based on some heuristic.
     *
     * @param workset The workset. Must not be modified.
     * @return The index of the selected edge, which is in present in the workset.
     */
    public int select(BitSet workset) {
        int edgeIdx = selectInternal(workset);
        Assert.check(workset.get(edgeIdx)); // Selected edge must be in the workset.
        return edgeIdx;
    }

    /**
     * Select an edge from a non-empty workset based on some heuristic.
     *
     * @param workset The workset. Must not be modified.
     * @return The index of the selected edge, which must be present in in the workset.
     */
    protected abstract int selectInternal(BitSet workset);

    /**
     * Update the state of the edge selector, based on the result of applying the edge.
     *
     * @param edgeIdx The index of the edge that was applied.
     * @param hadAnEffect Whether applying the edge had an effect (at least one new state was reached).
     */
    public abstract void update(int edgeIdx, boolean hadAnEffect);
}
