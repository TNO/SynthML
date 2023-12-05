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

package org.eclipse.escet.cif.datasynth.spec;

import org.eclipse.escet.cif.metamodel.cif.declarations.InputVariable;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;

/** Information on an input variable of a specification, used for synthesis. */
public class SynthesisInputVariable extends SynthesisTypedVariable {
    /** The CIF variable that corresponds to this synthesis variable. */
    public final InputVariable var;

    /**
     * Constructor for the {@link SynthesisInputVariable} class.
     *
     * @param var The CIF input variable that corresponds to this synthesis variable.
     * @param type The normalized type of the variable.
     * @param count The number of potential values of the variable.
     * @param lower The lower bound (minimum value) of the variable.
     * @param upper The upper bound (maximum value) of the variable.
     */
    public SynthesisInputVariable(InputVariable var, CifType type, int count, int lower, int upper) {
        super(var, type, count, lower, upper);
        this.var = var;
    }

    @Override
    public String getKindText() {
        return "input variable";
    }
}
