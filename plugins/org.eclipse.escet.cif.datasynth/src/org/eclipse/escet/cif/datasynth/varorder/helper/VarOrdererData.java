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

package org.eclipse.escet.cif.datasynth.varorder.helper;

import java.util.List;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;

/** Data used by variable orderers, being provided as input and produced as result. */
public class VarOrdererData {
    /** The non-interleaved variables in model order. */
    public final List<SynthesisVariable> varsInModelOrder;

    /** The variable order. */
    public final VarOrder varOrder;

    /** The helper for variable ordering. */
    public final VarOrderHelper helper;

    /**
     * Constructor for the {@link VarOrdererData} class.
     *
     * @param varsInModelOrder The non-interleaved variables in model order.
     * @param varOrder The variable order.
     * @param helper The helper for variable ordering.
     */
    public VarOrdererData(List<SynthesisVariable> varsInModelOrder, VarOrder varOrder, VarOrderHelper helper) {
        this.varsInModelOrder = varsInModelOrder;
        this.varOrder = varOrder;
        this.helper = helper;
    }

    /**
     * Constructor for the {@link VarOrdererData} class.
     *
     * @param data The existing {@link VarOrdererData} instance upon which to base the new instance.
     * @param newOrder The new variable order.
     * @param effect The variable orderer effect.
     */
    public VarOrdererData(VarOrdererData data, VarOrder newOrder, VarOrdererEffect effect) {
        this(data.varsInModelOrder, effect.updateVarOrder ? newOrder : data.varOrder, effect.updateRepresentations
                ? new VarOrderHelper(data.helper, newOrder.getOrderedVars()) : data.helper);
    }
}
