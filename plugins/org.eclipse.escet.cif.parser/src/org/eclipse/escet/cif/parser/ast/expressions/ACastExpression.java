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

import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Cat expression. */
public class ACastExpression extends AExpression {
    /** The child of the cast expression. */
    public final AExpression child;

    /** The target type of the cast expression. */
    public final ACifType type;

    /**
     * Constructor for the {@link ACastExpression} class.
     *
     * @param child The child of the cast expression.
     * @param type The target type of the cast expression.
     * @param position Position information.
     */
    public ACastExpression(AExpression child, ACifType type, TextPosition position) {
        super(position);
        this.child = child;
        this.type = type;
    }
}
