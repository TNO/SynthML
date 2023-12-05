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

import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.common.java.Strings;

/** Variable orderer that sorts the variables based on their name, without interleaving. */
public class SortedVarOrderer extends VarOrderer {
    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link SortedVarOrderer} class.
     *
     * @param effect The effect of applying the variable orderer.
     */
    public SortedVarOrderer(VarOrdererEffect effect) {
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying sorted variable order:");
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
        }

        // Get variables in model order.
        List<SynthesisVariable> modelOrder = inputData.varsInModelOrder;

        // Sort variables based on their name.
        List<SynthesisVariable> sortedOrder = modelOrder.stream()
                .sorted((v, w) -> Strings.SORTER.compare(v.rawName, w.rawName)).collect(Collectors.toList());

        // Return new variable order.
        return new VarOrdererData(inputData, VarOrder.createFromOrderedVars(sortedOrder), effect);
    }

    @Override
    public String toString() {
        return fmt("sorted(effect=%s)", enumValueToParserArg(effect));
    }
}
