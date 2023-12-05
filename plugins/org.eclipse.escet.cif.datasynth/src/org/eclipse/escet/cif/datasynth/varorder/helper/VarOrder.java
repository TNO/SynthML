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
import java.util.Objects;

import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;

/** Variable order. Puts all synthesis variables in a certain order. The variables may optionally be interleaved. */
public class VarOrder {
    /**
     * The variable order. The outer list represents ordered groups. Each inner list represents a group of ordered and
     * interleaved variables.
     */
    private final List<List<SynthesisVariable>> varOrder;

    /**
     * Constructor for the {@link VarOrder} class.
     *
     * @param varOrder The variable order. The outer list represents ordered groups. Each inner list represents a group
     *     of ordered and interleaved variables.
     */
    public VarOrder(List<List<SynthesisVariable>> varOrder) {
        this.varOrder = varOrder;
    }

    /**
     * Create a non-interleaved variable order from ordered variables.
     *
     * @param orderedVars The ordered variables.
     * @return The non-interleaved variable order.
     */
    public static VarOrder createFromOrderedVars(List<SynthesisVariable> orderedVars) {
        return new VarOrder(orderedVars.stream().map(v -> List.of(v)).toList());
    }

    /**
     * Returns the variable order.
     *
     * @return The variable order. The outer list represents ordered groups. Each inner list represents a group of
     *     ordered and interleaved variables.
     */
    public List<List<SynthesisVariable>> getVarOrder() {
        return varOrder;
    }

    /**
     * Returns the ordered variables of this variable order.
     *
     * @return The ordered variables.
     */
    public List<SynthesisVariable> getOrderedVars() {
        return varOrder.stream().flatMap(grp -> grp.stream()).toList();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof VarOrder that)) {
            return false;
        } else {
            return Objects.equals(this.varOrder, that.varOrder);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(varOrder);
    }
}
