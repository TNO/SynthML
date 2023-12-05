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

/** Edge selector that always selects the first edge from the workset. */
public class FirstEdgeSelector extends EdgeSelector {
    @Override
    public int selectInternal(BitSet workset) {
        return workset.nextSetBit(0);
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        // No state to update.
    }
}
