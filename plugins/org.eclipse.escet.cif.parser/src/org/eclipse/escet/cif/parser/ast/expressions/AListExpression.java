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

import java.util.List;

import org.eclipse.escet.common.java.TextPosition;

/** List expression. */
public class AListExpression extends AExpression {
    /** The elements of the list expression. */
    public final List<AExpression> elements;

    /**
     * Constructor for the {@link AListExpression} class.
     *
     * @param elements The elements of the list expression.
     * @param position Position information.
     */
    public AListExpression(List<AExpression> elements, TextPosition position) {
        super(position);
        this.elements = elements;
    }
}
