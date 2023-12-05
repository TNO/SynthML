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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;

import org.eclipse.escet.common.java.BitSets;
import org.junit.jupiter.api.Test;

/** Tests for {@link FirstEdgeSelector}. */
public class FirstEdgeSelectorTest {
    @Test
    @SuppressWarnings("javadoc")
    public void test() {
        BitSet workset = BitSets.makeBitset(0, 1, 2, 3);
        assertEquals(0, new FirstEdgeSelector().select(workset));

        workset = BitSets.makeBitset(1, 2, 3);
        assertEquals(1, new FirstEdgeSelector().select(workset));

        workset = BitSets.makeBitset(4, 9, 2, 7);
        assertEquals(2, new FirstEdgeSelector().select(workset));
    }
}
