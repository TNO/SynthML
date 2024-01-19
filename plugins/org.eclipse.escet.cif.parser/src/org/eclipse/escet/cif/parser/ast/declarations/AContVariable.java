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

package org.eclipse.escet.cif.parser.ast.declarations;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;

/** Continuous variable. */
public class AContVariable extends ACifObject {
    /** The name of the continuous variable. */
    public final AIdentifier name;

    /** The initial value of the continuous variable, or {@code null} for the default value. */
    public final AExpression value;

    /** The derivative of the continuous variable, or {@code null} if not specified. */
    public final AExpression derivative;

    /**
     * Constructor for the {@link AContVariable} class.
     *
     * @param name The name of the continuous variable.
     * @param value The initial value of the continuous variable, or {@code null} for the default value.
     * @param derivative The derivative of the continuous variable, or {@code null} if not specified.
     * @param position Position information.
     */
    public AContVariable(AIdentifier name, AExpression value, AExpression derivative, TextPosition position) {
        super(position);
        this.name = name;
        this.value = value;
        this.derivative = derivative;
    }
}
