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

/**
 * Empty set or dictionary expression.
 *
 * @see ADictExpression
 * @see ASetExpression
 */
public class AEmptySetDictExpression extends AExpression {
    /**
     * Constructor for the {@link AEmptySetDictExpression} class.
     *
     * @param position Position information.
     */
    public AEmptySetDictExpression(TextPosition position) {
        super(position);
    }
}
