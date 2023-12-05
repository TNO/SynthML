//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.varorder.orderers;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.common.java.Assert;

/** Variable orderer that applies multiple other orderers sequentially, each to the result of the previous orderer. */
public class SequentialVarOrderer extends VarOrderer {
    /** The sequence of orderers to apply, in order. */
    private final List<VarOrderer> orderers;

    /**
     * Constructor for the {@link SequentialVarOrderer} class.
     *
     * @param orderers The sequence of orderers to apply, in order. Must be at least two orderers.
     */
    public SequentialVarOrderer(List<VarOrderer> orderers) {
        this.orderers = orderers;
        Assert.check(orderers.size() >= 2);
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output before applying the orderers.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying %d orderers, sequentially:", orderers.size());
        }

        // Initialize result to the input data.
        VarOrdererData resultData = inputData;

        // Apply each orderer, in order, to the result of the previous orderer.
        for (int i = 0; i < orderers.size(); i++) {
            // Separate debug output of this orderer from that of the previous one.
            if (i > 0 && dbgEnabled) {
                inputData.helper.dbg();
            }

            // Apply orderer and update the current result.
            VarOrderer orderer = orderers.get(i);
            resultData = orderer.order(resultData, dbgEnabled, dbgLevel + 1);
        }

        // Return the result.
        return resultData;
    }

    @Override
    public String toString() {
        return orderers.stream().map(VarOrderer::toString).collect(Collectors.joining(" -> "));
    }
}
