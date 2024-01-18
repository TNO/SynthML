//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.parser.ast;

import org.eclipse.escet.cif.parser.ast.automata.ALocation;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;

/** Equation. */
public class AEquation extends ACifObject {
    /** The variable that is given a value. */
    public final AIdentifier variable;

    /** Whether the derivative of the variable is given a value. */
    public final boolean derivative;

    /** The value for the (derivative of) the variable. */
    public final AExpression value;

    /**
     * The parent of the equation. Either an {@link AEquationDecl} or an {@link ALocation}. Is {@code null} until set by
     * the parser.
     */
    public ACifObject parent;

    /**
     * Constructor for the {@link AEquation} class.
     *
     * @param variable The variable that is given a value.
     * @param derivative Whether the derivative of the variable is given a value.
     * @param value The value for the (derivative of) the variable.
     * @param position Position information.
     */
    public AEquation(AIdentifier variable, boolean derivative, AExpression value, TextPosition position) {
        super(position);
        this.variable = variable;
        this.derivative = derivative;
        this.value = value;
    }
}
