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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.TextPosition;

/** An 'elif' expression part of an 'if' expression. */
public class AElifExpression extends ACifObject {
    /** The guards of the 'elif' expression. */
    public final List<AExpression> guards;

    /** The 'then' expression of the 'elif' expression. */
    public final AExpression then;

    /**
     * Constructor for the {@link AElifExpression} class.
     *
     * @param guards The guards of the 'elif' expression.
     * @param then The 'then' expression of the 'elif' expression.
     * @param position Position information.
     */
    public AElifExpression(List<AExpression> guards, AExpression then, TextPosition position) {
        super(position);
        this.guards = guards;
        this.then = then;
    }
}
