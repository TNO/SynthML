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

package org.eclipse.escet.cif.parser.ast.functions;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Return internal function statement. */
public class AReturnFuncStatement extends AFuncStatement {
    /** The return values of the return internal function statement. */
    public final List<AExpression> values;

    /**
     * Constructor for the {@link AReturnFuncStatement} class.
     *
     * @param values The return values of the return internal function statement.
     * @param position Position information.
     */
    public AReturnFuncStatement(List<AExpression> values, TextPosition position) {
        super(position);
        this.values = values;
    }
}
