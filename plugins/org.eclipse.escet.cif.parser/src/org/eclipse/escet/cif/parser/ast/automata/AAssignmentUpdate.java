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

package org.eclipse.escet.cif.parser.ast.automata;

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Assignment. */
public class AAssignmentUpdate extends AUpdate {
    /** The addressable that is assigned. */
    public final AExpression addressable;

    /** The new value of the variable. */
    public final AExpression value;

    /**
     * Constructor for the {@link AAssignmentUpdate} class.
     *
     * @param addressable The addressable that is assigned.
     * @param value The new value of the variable.
     * @param position Position information.
     */
    public AAssignmentUpdate(AExpression addressable, AExpression value, TextPosition position) {
        super(position);
        this.addressable = addressable;
        this.value = value;
    }
}
