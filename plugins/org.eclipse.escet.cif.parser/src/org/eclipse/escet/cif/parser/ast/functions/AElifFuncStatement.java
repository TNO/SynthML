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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** An 'elif' part of an 'if' internal function statement. */
public class AElifFuncStatement extends ACifObject {
    /** The guards of the elif. */
    public final List<AExpression> guards;

    /** The 'then' statements of the 'elif'. */
    public final List<AFuncStatement> thens;

    /**
     * Constructor for the {@link AElifFuncStatement} class.
     *
     * @param guards The guards of the 'elif'.
     * @param thens The 'then' statements of the 'elif'.
     * @param position Position information.
     */
    public AElifFuncStatement(List<AExpression> guards, List<AFuncStatement> thens, TextPosition position) {
        super(position);
        this.guards = guards;
        this.thens = thens;
    }
}
