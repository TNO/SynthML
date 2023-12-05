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

import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.common.java.BitSets;
import org.junit.jupiter.api.Test;

/** Tests for {@link MaxCardinalityEdgePruner}. */
public class MaxCardinalityEdgePrunerTest {
    @Test
    @SuppressWarnings("javadoc")
    public void test() {
        List<BitSet> dependencies = list( //
                BitSets.makeBitset(0, 1, 2), //
                BitSets.makeBitset(1, 2, 3, 4), //
                BitSets.makeBitset(7, 2, 9), //
                BitSets.makeBitset(3, 123, 0, 7), //
                BitSets.makeBitset(4, 5) //
        );
        int count = dependencies.size();
        EdgePruner pruner = new MaxCardinalityEdgePruner(dependencies);

        assertEquals(".1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(0, 1, 2, 3, 4)), count));
        assertEquals("1.1..", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(0, 2)), count));
        assertEquals("1.1..", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(0, 2, 4)), count));
        assertEquals("...1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(3, 4)), count));
    }
}
