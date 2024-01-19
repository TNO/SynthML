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

/** If expression. */
public class AIfExpression extends AExpression {
    /** The guards of the 'if' expression. */
    public final List<AExpression> guards;

    /** The 'then' expression of the 'if' expression. */
    public final AExpression then;

    /** The 'elif' expressions of the 'if' expression. */
    public final List<AElifExpression> elifs;

    /** The 'else' expression of the 'if' expression. */
    public final AExpression elseExpr;

    /**
     * Constructor for the {@link AIfExpression} class.
     *
     * @param guards The guards of the 'if' expression.
     * @param then The 'then' expression of the 'if' expression.
     * @param elifs The 'elif' expressions of the 'if' expression.
     * @param elseExpr The 'else' expression of the 'if' expression.
     * @param position Position information.
     */
    public AIfExpression(List<AExpression> guards, AExpression then, List<AElifExpression> elifs, AExpression elseExpr,
            TextPosition position)
    {
        super(position);
        this.guards = guards;
        this.then = then;
        this.elifs = elifs;
        this.elseExpr = elseExpr;
    }
}
