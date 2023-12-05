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

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.escet.common.java.BitSets;
import org.junit.jupiter.api.Test;

/** Tests for {@link RandomEdgeSelector}. */
public class RandomEdgeSelectorTest {
    @Test
    @SuppressWarnings("javadoc")
    public void testSamples() {
        BitSet workset = BitSets.makeBitset(0, 1, 2, 3);
        List<Integer> samples = sampleWorkset(workset, 10);
        assertEquals(list(2, 3, 0, 2, 2, 1, 2, 0, 2, 3), samples);

        workset = BitSets.makeBitset(1, 2, 3);
        samples = sampleWorkset(workset, 10);
        assertEquals(list(1, 2, 2, 3, 3, 3, 3, 1, 1, 3), samples);

        workset = BitSets.makeBitset(4, 9, 2, 7);
        samples = sampleWorkset(workset, 10);
        assertEquals(list(7, 9, 2, 7, 7, 4, 7, 2, 7, 9), samples);
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testValid() {
        EdgeSelector selector = new RandomEdgeSelector();
        int[] bits = new int[] {2, 5, 9, 16, 1025};
        BitSet workset = BitSets.makeBitset(bits);
        for (int i = 0; i < 1024; i++) {
            int value = selector.select(workset);
            assertTrue(value >= 0);
            assertTrue(IntStream.of(bits).boxed().toList().contains(value));
        }
    }

    /**
     * Sample the workset.
     *
     * @param workset The workset.
     * @param count The number of samples to take.
     * @return The samples.
     */
    private List<Integer> sampleWorkset(BitSet workset, int count) {
        EdgeSelector selector = new RandomEdgeSelector();
        List<Integer> samples = listc(count);
        for (int i = 0; i < count; i++) {
            samples.add(selector.select(workset));
        }
        return samples;
    }
}
