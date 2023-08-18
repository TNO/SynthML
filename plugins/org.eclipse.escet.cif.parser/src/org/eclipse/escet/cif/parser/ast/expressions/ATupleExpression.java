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

import java.util.List;

import org.eclipse.escet.common.java.TextPosition;

/** Tuple expression. */
public class ATupleExpression extends AExpression {
    /** The elements of the tuple expression. */
    public final List<AExpression> elements;

    /**
     * Constructor for the {@link ATupleExpression} class.
     *
     * @param elements The elements of the tuple expression.
     * @param position Position information.
     */
    public ATupleExpression(List<AExpression> elements, TextPosition position) {
        super(position);
        this.elements = elements;
    }
}
