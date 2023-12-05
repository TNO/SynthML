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

import org.eclipse.escet.cif.datasynth.varorder.orderers.VarOrderer;

/** The effect of applying a {@link VarOrderer variable orderer}. */
public enum VarOrdererEffect {
    /** Only update the variable order. */
    VAR_ORDER(true, false),

    /** Only update the various representations of the relations between the synthesis variables. */
    REPRESENTATIONS(false, true),

    /**
     * Update both the variable order and the various representations of the relations between the synthesis variables.
     */
    BOTH(true, true);

    /** Whether to update the variable order. */
    public final boolean updateVarOrder;

    /** Whether to update the various representations of the relations between the synthesis variables. */
    public final boolean updateRepresentations;

    /**
     * Constructor for the {@link VarOrdererEffect} enumeration.
     *
     * @param updateVarOrder Whether to update the variable order.
     * @param updateRepresentations Whether to update the various representations of the relations between the synthesis
     *     variables.
     */
    private VarOrdererEffect(boolean updateVarOrder, boolean updateRepresentations) {
        this.updateVarOrder = updateVarOrder;
        this.updateRepresentations = updateRepresentations;
    }
}
