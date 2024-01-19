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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.TextPosition;

/** A case of a 'switch' expression. */
public class ASwitchCase extends ACifObject {
    /** The key of the 'switch' case, or {@code null} for 'else'. */
    public final AExpression key;

    /** The value of the 'switch' case. */
    public final AExpression value;

    /**
     * Constructor for the {@link ASwitchCase} class.
     *
     * @param key The key of the 'switch' case, or {@code null} for 'else'.
     * @param value The value of the 'switch' case.
     * @param position Position information.
     */
    public ASwitchCase(AExpression key, AExpression value, TextPosition position) {
        super(position);
        this.key = key;
        this.value = value;
    }
}
