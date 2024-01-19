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

/**
 * Dictionary expression, for non-empty dictionaries.
 *
 * @see AEmptySetDictExpression
 */
public class ADictExpression extends AExpression {
    /** The key/value pairs of the dictionary expression. Contains at least one pair. */
    public final List<ADictPair> pairs;

    /**
     * Constructor for the {@link ADictExpression} class.
     *
     * @param pairs The key/value pairs of the dictionary expression. Contains at least one pair.
     * @param position Position information.
     */
    public ADictExpression(List<ADictPair> pairs, TextPosition position) {
        super(position);
        this.pairs = pairs;
    }
}
