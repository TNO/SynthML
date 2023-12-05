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

import static org.eclipse.escet.common.java.Lists.copy;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;

/** Variable orderer that orders the variables in a random order, without interleaving. */
public class RandomVarOrderer extends VarOrderer {
    /** The random order seed number in case a fixed seed is to be used, or {@code null} otherwise. */
    private final Long seed;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link RandomVarOrderer} class.
     *
     * @param seed The random order seed number in case a fixed seed is to be used, or {@code null} otherwise.
     * @param effect The effect of applying the variable orderer.
     */
    public RandomVarOrderer(Long seed, VarOrdererEffect effect) {
        this.seed = seed;
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying a random variable order:");
            inputData.helper.dbg(dbgLevel + 1, "Seed: %s", (seed == null) ? "random" : seed);
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
        }

        // Get variables in model order.
        List<SynthesisVariable> modelOrder = inputData.varsInModelOrder;

        // Shuffle to random order.
        List<SynthesisVariable> randomOrder = copy(modelOrder);
        if (seed == null) {
            Collections.shuffle(randomOrder);
        } else {
            Collections.shuffle(randomOrder, new Random(seed));
        }

        // Return new variable order.
        return new VarOrdererData(inputData, VarOrder.createFromOrderedVars(randomOrder), effect);
    }

    @Override
    public String toString() {
        return fmt("random(%seffect=%s)", (seed == null) ? "" : fmt("seed=%d, ", seed), enumValueToParserArg(effect));
    }
}
