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

package org.eclipse.escet.cif.parser.ast.types;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Type range. */
public class ARange extends ACifObject {
    /** Lower bound of the type range. */
    public final AExpression lower;

    /**
     * Upper bound of the type range, or {@code null} if equal to upper bound. May be {@code null} for list types, but
     * not for integer types.
     */
    public final AExpression upper;

    /**
     * Constructor for the {@link ARange} class.
     *
     * @param lower Lower bound of the type range.
     * @param upper Upper bound of the type range, or {@code null} if equal to upper bound. May be {@code null} for list
     *     types, but not for integer types.
     * @param position Position information.
     */
    public ARange(AExpression lower, AExpression upper, TextPosition position) {
        super(position);
        this.lower = lower;
        this.upper = upper;
    }
}
