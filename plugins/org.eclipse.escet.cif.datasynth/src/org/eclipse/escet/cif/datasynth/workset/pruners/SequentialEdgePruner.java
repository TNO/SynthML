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

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.common.java.Assert;

/** Edge pruner that sequentially applies multiple pruners. */
public class SequentialEdgePruner extends EdgePruner {
    /** The sequence of pruners to apply, in the order to apply them. */
    private final List<EdgePruner> pruners;

    /**
     * Constructor for the {@link SequentialEdgePruner} class.
     *
     * @param pruners The sequence of pruners to apply, in the order to apply them.
     */
    public SequentialEdgePruner(EdgePruner... pruners) {
        this(Arrays.asList(pruners));
    }

    /**
     * Constructor for the {@link SequentialEdgePruner} class.
     *
     * @param pruners The sequence of pruners to apply, in the order to apply them.
     */
    public SequentialEdgePruner(List<EdgePruner> pruners) {
        this.pruners = pruners;
        Assert.check(pruners.size() >= 2);
    }

    @Override
    public BitSet pruneInternal(BitSet workset) {
        for (EdgePruner pruner: pruners) {
            workset = pruner.prune(workset);
        }
        return workset;
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        for (EdgePruner pruner: pruners) {
            pruner.update(edgeIdx, hadAnEffect);
        }
    }
}
