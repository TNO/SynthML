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

/** Projection expression. */
public class AProjectionExpression extends AExpression {
    /** The child of the projection expression. */
    public final AExpression child;

    /** The index of the projection expression. */
    public final AExpression index;

    /**
     * Constructor for the {@link AProjectionExpression} class.
     *
     * @param child The child of the projection expression.
     * @param index The index of the projection expression.
     * @param position Position information.
     */
    public AProjectionExpression(AExpression child, AExpression index, TextPosition position) {
        super(position);
        this.child = child;
        this.index = index;
    }
}
