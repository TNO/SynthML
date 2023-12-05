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

import org.eclipse.escet.cif.datasynth.workset.pruners.EdgePruner;
import org.eclipse.escet.common.java.BitSets;

/** Edge selector that first prunes the workset and then chooses an edge from it. */
public class PruningEdgeSelector extends EdgeSelector {
    /** The edge pruner. */
    private final EdgePruner pruner;

    /** The edge selector. */
    private final EdgeSelector selector;

    /**
     * Constructor for the {@link PruningEdgeSelector} class.
     *
     * @param pruner The edge pruner.
     * @param selector The edge selector.
     */
    public PruningEdgeSelector(EdgePruner pruner, EdgeSelector selector) {
        this.pruner = pruner;
        this.selector = selector;
    }

    @Override
    public int selectInternal(BitSet workset) {
        workset = BitSets.copy(workset);
        workset = pruner.prune(workset);
        return selector.select(workset);
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        pruner.update(edgeIdx, hadAnEffect);
        selector.update(edgeIdx, hadAnEffect);
    }
}
