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

package org.eclipse.escet.cif.parser.ast.expressions;

import org.eclipse.escet.common.java.TextPosition;

/** String literal expression. */
public class AStringExpression extends AExpression {
    /** The value of the string literal expression, without surrounding quotes, and without escape sequences. */
    public final String value;

    /**
     * Constructor for the {@link AStringExpression} class.
     *
     * @param value The value of the string literal expression, without surrounding quotes, and without escape
     *     sequences.
     * @param position Position information.
     */
    public AStringExpression(String value, TextPosition position) {
        super(position);
        this.value = value;
    }
}
