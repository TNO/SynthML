//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

import static org.eclipse.escet.common.java.Strings.fmt;

import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;

/** Variable orderer that orders the variables as in the model, without interleaving. */
public class ModelVarOrderer extends VarOrderer {
    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link ModelVarOrderer} class.
     *
     * @param effect The effect of applying the variable orderer.
     */
    public ModelVarOrderer(VarOrdererEffect effect) {
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying model variable order:");
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
        }

        // Return new variable order.
        VarOrder modelOrder = VarOrder.createFromOrderedVars(inputData.varsInModelOrder);
        return new VarOrdererData(inputData, modelOrder, effect);
    }

    @Override
    public String toString() {
        return fmt("model(effect=%s)", enumValueToParserArg(effect));
    }
}
