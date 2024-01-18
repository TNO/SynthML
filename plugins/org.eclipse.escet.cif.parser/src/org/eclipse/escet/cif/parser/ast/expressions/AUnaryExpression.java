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

package org.eclipse.escet.cif.parser.ast.expressions;

import org.eclipse.escet.common.java.TextPosition;

/** Unary expression. */
public class AUnaryExpression extends AExpression {
    /** The unary operator, as scanned text. */
    public final String operator;

    /** The child expression of the unary expression. */
    public final AExpression child;

    /**
     * Constructor for the {@link AUnaryExpression} class.
     *
     * @param operator The unary operator, as scanned text.
     * @param child The child expression of the unary expression.
     * @param position Position information.
     */
    public AUnaryExpression(String operator, AExpression child, TextPosition position) {
        super(position);
        this.operator = operator;
        this.child = child;
    }
}
