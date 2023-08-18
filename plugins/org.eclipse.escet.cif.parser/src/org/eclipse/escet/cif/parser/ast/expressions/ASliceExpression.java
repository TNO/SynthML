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

/** Slice expression. */
public class ASliceExpression extends AExpression {
    /** The child of the slice expression. */
    public final AExpression child;

    /** The begin index of the slice expression, or {@code null}. */
    public final AExpression begin;

    /** The end index of the slice expression, or {@code null}. */
    public final AExpression end;

    /**
     * Constructor for the {@link ASliceExpression} class.
     *
     * @param child The child of the projection expression.
     * @param begin The begin index of the slice expression, or {@code null}.
     * @param end The end index of the slice expression, or {@code null}.
     * @param position Position information.
     */
    public ASliceExpression(AExpression child, AExpression begin, AExpression end, TextPosition position) {
        super(position);
        this.child = child;
        this.begin = begin;
        this.end = end;
    }
}
