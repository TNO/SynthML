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
import java.util.Random;

/** Edge selector that selects a random edge from the workset. */
public class RandomEdgeSelector extends EdgeSelector {
    /**
     * The random generator to use to randomly select an edge from the workset. It is initialized with a seed to be
     * deterministically random.
     */
    private final Random random = new Random(0);

    @Override
    public int selectInternal(BitSet workset) {
        int cardinality = workset.cardinality();
        int selectedNr = random.nextInt(cardinality); // Random number in the range [0..cardinality).
        int selectedIdx = workset.stream().skip(selectedNr).findFirst().getAsInt();
        return selectedIdx;
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        // No state to update.
    }
}
