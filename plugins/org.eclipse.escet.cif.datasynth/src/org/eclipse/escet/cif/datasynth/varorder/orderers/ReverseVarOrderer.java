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

import static org.eclipse.escet.common.java.Lists.reverse;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.List;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RepresentationKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;

/** Variable orderer that reverses the input variable order, preserving its interleaving. */
public class ReverseVarOrderer extends VarOrderer {
    /** The kind of relations to use to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link ReverseVarOrderer} class.
     *
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public ReverseVarOrderer(RelationsKind relationsKind, VarOrdererEffect effect) {
        this.relationsKind = relationsKind;
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output before reversing the variable order.
        List<SynthesisVariable> orderedVars = inputData.varOrder.getOrderedVars();
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Reversing the variable order:");
            inputData.helper.dbg(dbgLevel + 1, "Relations: %s", enumValueToParserArg(relationsKind));
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
            inputData.helper.dbgRepresentation(dbgLevel + 1, RepresentationKind.HYPER_EDGES, relationsKind);
            inputData.helper.dbg();
            inputData.helper.dbgMetricsForVarOrder(dbgLevel + 1, orderedVars, "before", relationsKind);
        }

        // Reverse the order.
        List<List<SynthesisVariable>> varOrder = inputData.varOrder.getVarOrder();
        varOrder = varOrder.stream().map(grp -> reverse(grp)).toList(); // Reverse inner lists (groups).
        varOrder = reverse(varOrder); // Reverse outer list (groups).

        // Debug output after reversing the variable order.
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForVarOrder(dbgLevel + 1, orderedVars, "reversed", relationsKind);
        }

        // Return new variable order.
        return new VarOrdererData(inputData, new VarOrder(varOrder), effect);
    }

    @Override
    public String toString() {
        return fmt("reverse(relations=%s, effect=%s)", enumValueToParserArg(relationsKind),
                enumValueToParserArg(effect));
    }
}
