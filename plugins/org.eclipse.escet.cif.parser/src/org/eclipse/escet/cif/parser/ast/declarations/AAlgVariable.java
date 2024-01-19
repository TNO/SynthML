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

/** Algebraic variable. */
public class AAlgVariable extends ACifObject {
    /** The name of the algebraic variable. */
    public final AIdentifier name;

    /** The value of the algebraic variable, or {@code null} if not specified. */
    public final AExpression value;

    /**
     * Constructor for the {@link AAlgVariable} class.
     *
     * @param name The name of the algebraic variable.
     * @param value The value of the algebraic variable, or {@code null} if not specified.
     * @param position Position information.
     */
    public AAlgVariable(AIdentifier name, AExpression value, TextPosition position) {
        super(position);
        this.name = name;
        this.value = value;
    }
}
