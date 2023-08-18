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

/** Standard library function reference expression. */
public class AStdLibFunctionExpression extends AExpression {
    /** The standard library function, as scanned text. */
    public final String function;

    /**
     * Constructor for the {@link AStdLibFunctionExpression} class.
     *
     * @param function The standard library function, as scanned text.
     * @param position Position information.
     */
    public AStdLibFunctionExpression(String function, TextPosition position) {
        super(position);
        this.function = function;
    }
}
