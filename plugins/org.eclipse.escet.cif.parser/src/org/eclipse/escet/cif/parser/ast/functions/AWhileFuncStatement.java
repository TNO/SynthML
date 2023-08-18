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

package org.eclipse.escet.cif.parser.ast.functions;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** While internal function statement. */
public class AWhileFuncStatement extends AFuncStatement {
    /** The guards of the while internal function statement. */
    public final List<AExpression> guards;

    /** The body statements of the while internal function statement. */
    public final List<AFuncStatement> statements;

    /**
     * Constructor for the {@link AWhileFuncStatement} class.
     *
     * @param guards The guards of the while internal function statement.
     * @param statements The body statements of the while internal function statement.
     * @param position Position information.
     */
    public AWhileFuncStatement(List<AExpression> guards, List<AFuncStatement> statements, TextPosition position) {
        super(position);
        this.guards = guards;
        this.statements = statements;
    }
}
