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

/** Switch expression. */
public class ASwitchExpression extends AExpression {
    /** The control value of the 'switch' expression. */
    public final AExpression value;

    /** The cases of the 'switch' expression. */
    public final List<ASwitchCase> cases;

    /**
     * Constructor for the {@link ASwitchExpression} class.
     *
     * @param value The control value of the 'switch' expression.
     * @param cases The cases of the 'switch' expression.
     * @param position Position information.
     */
    public ASwitchExpression(AExpression value, List<ASwitchCase> cases, TextPosition position) {
        super(position);
        this.value = value;
        this.cases = cases;
    }
}
