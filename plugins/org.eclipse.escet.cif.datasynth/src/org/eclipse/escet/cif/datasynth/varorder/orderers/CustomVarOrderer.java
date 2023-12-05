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

import java.util.List;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;

/** Variable orderer that orders the variables to a user-specified custom order and interleaving. */
public class CustomVarOrderer extends VarOrderer {
    /** The variable order. */
    private final VarOrder order;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link CustomVarOrderer} class.
     *
     * @param order The variable order.
     * @param effect The effect of applying the variable orderer.
     */
    public CustomVarOrderer(VarOrder order, VarOrdererEffect effect) {
        this.order = order;
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying a custom variable order:");
            inputData.helper.dbg(dbgLevel + 1, "Order: %s", getOrderText());
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
        }

        // Return new variable order.
        return new VarOrdererData(inputData, order, effect);
    }

    /**
     * Returns the custom order, in option syntax.
     *
     * @return The custom order text.
     */
    private String getOrderText() {
        StringBuilder txt = new StringBuilder();
        List<List<SynthesisVariable>> groups = order.getVarOrder();
        for (int i = 0; i < groups.size(); i++) {
            List<SynthesisVariable> group = groups.get(i);
            if (i > 0) {
                txt.append(";");
            }
            for (int j = 0; j < group.size(); j++) {
                if (j > 0) {
                    txt.append(",");
                }
                SynthesisVariable var = group.get(j);
                txt.append(var.rawName);
            }
        }
        return txt.toString();
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("custom(effect=");
        txt.append(enumValueToParserArg(effect));
        txt.append(", order=\"");
        txt.append(getOrderText());
        txt.append("\")");
        return txt.toString();
    }
}
