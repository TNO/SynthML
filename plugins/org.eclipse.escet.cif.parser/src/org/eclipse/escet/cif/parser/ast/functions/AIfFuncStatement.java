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

/** An 'if' internal function statement. */
public class AIfFuncStatement extends AFuncStatement {
    /** The guards of the 'if' internal function statement. */
    public final List<AExpression> guards;

    /** The 'then' statements of the 'if' internal function statement. */
    public final List<AFuncStatement> thens;

    /** The 'elif' statements of the 'if' internal function statement. */
    public final List<AElifFuncStatement> elifs;

    /** The 'else' statements, or {@code null} if not available. */
    public final AElseFuncStatement elseStat;

    /**
     * Constructor for the {@link AIfFuncStatement} class.
     *
     * @param guards The guards of the 'if' internal function statement.
     * @param thens The 'then' statements of the 'if' internal function statement.
     * @param elifs The 'elif' statements of the 'if' internal function statement.
     * @param elseStat The 'else' statements, or {@code null} if not available.
     * @param position Position information.
     */
    public AIfFuncStatement(List<AExpression> guards, List<AFuncStatement> thens, List<AElifFuncStatement> elifs,
            AElseFuncStatement elseStat, TextPosition position)
    {
        super(position);
        this.guards = guards;
        this.thens = thens;
        this.elifs = elifs;
        this.elseStat = elseStat;
    }
}
