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

/** Function call expression. */
public class AFuncCallExpression extends AExpression {
    /** The function of the function call expression. */
    public final AExpression function;

    /** The arguments of the function call expression, or {@code null}. */
    public final List<AExpression> arguments;

    /**
     * Constructor for the {@link AFuncCallExpression} class.
     *
     * @param function The function of the function call expression.
     * @param arguments The arguments of the function call expression, or {@code null}.
     * @param position Position information.
     */
    public AFuncCallExpression(AExpression function, List<AExpression> arguments, TextPosition position) {
        super(position);
        this.function = function;
        this.arguments = arguments;
    }
}
