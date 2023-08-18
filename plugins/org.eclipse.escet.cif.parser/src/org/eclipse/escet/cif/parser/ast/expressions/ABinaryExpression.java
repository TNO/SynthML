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

/** Binary expression. */
public class ABinaryExpression extends AExpression {
    /** The binary operator, as scanned text. */
    public final String operator;

    /** The left child of the binary expression. */
    public final AExpression left;

    /** The right child of the binary expression. */
    public final AExpression right;

    /**
     * Constructor for the {@link ABinaryExpression} class.
     *
     * @param operator The binary operator, as scanned text.
     * @param left The left child of the binary expression.
     * @param right The right child of the binary expression.
     * @param position Position information.
     */
    public ABinaryExpression(String operator, AExpression left, AExpression right, TextPosition position) {
        super(position);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
}
