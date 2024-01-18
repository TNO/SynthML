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

import org.eclipse.escet.cif.parser.ast.declarations.ADiscVariableDecl;
import org.eclipse.escet.common.java.TextPosition;

/** Internal function body. */
public class AInternalFuncBody extends AFuncBody {
    /** The local variables of the internal function. */
    public final List<ADiscVariableDecl> variables;

    /** The statements of the internal function. */
    public final List<AFuncStatement> statements;

    /** The position information of the {@code end} keyword. */
    public final TextPosition endPos;

    /**
     * Constructor for the {@link AInternalFuncBody} class.
     *
     * @param variables The local variables of the internal function.
     * @param statements The statements of the internal function.
     * @param endPos The position information of the {@code end} keyword.
     */
    public AInternalFuncBody(List<ADiscVariableDecl> variables, List<AFuncStatement> statements, TextPosition endPos) {
        super(null);
        this.variables = variables;
        this.statements = statements;
        this.endPos = endPos;
    }
}
