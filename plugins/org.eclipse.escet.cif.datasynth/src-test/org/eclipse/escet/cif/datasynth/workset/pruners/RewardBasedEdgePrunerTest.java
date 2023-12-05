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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.escet.common.java.BitSets;
import org.junit.jupiter.api.Test;

/** Tests for {@link RewardBasedEdgePruner}. */
public class RewardBasedEdgePrunerTest {
    @Test
    @SuppressWarnings("javadoc")
    public void testInit() {
        int count = 7;
        EdgePruner pruner = new RewardBasedEdgePruner(count, 1, -1);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testNoUpdateSameResult() {
        int count = 7;
        EdgePruner pruner = new RewardBasedEdgePruner(count, 1, -1);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testUpdates() {
        int count = 7;
        EdgePruner pruner = new RewardBasedEdgePruner(count, 1, -1);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(1, true);
        assertEquals(".1.....", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(3, true);
        assertEquals(".1.1...", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(5, true);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(3, true);
        assertEquals("...1...", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(5, false);
        assertEquals("...1...", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(3, false);
        assertEquals(".1.1...", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(3, false);
        assertEquals(".1.....", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(1, false);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testNotInWorksetNoEffect() {
        int count = 7;
        EdgePruner pruner = new RewardBasedEdgePruner(count, 1, -1);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        pruner.update(0, true);
        pruner.update(2, false);
        pruner.update(2, false);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSaturatePositive() {
        int count = 7;
        int quarter = Integer.MAX_VALUE / 4;
        EdgePruner pruner = new RewardBasedEdgePruner(count, quarter, -quarter);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));

        for (int i = 0; i < 5; i++) {
            pruner.update(1, true);
            assertEquals(".1.....", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
            pruner.update(3, true);
            pruner.update(5, true);
            assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        }

        pruner.update(1, true);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSaturateNegative() {
        int count = 7;
        int quarter = Integer.MAX_VALUE / 4;
        EdgePruner pruner = new RewardBasedEdgePruner(count, quarter, -quarter);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));

        for (int i = 0; i < 5; i++) {
            pruner.update(1, false);
            assertEquals("...1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
            pruner.update(3, false);
            pruner.update(5, false);
            assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
        }

        pruner.update(1, false);
        assertEquals(".1.1.1.", BitSets.bitsetToStr(pruner.prune(BitSets.makeBitset(1, 3, 5)), count));
    }
}
