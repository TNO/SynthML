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

/** Dictionary key/value pair. */
public class ADictPair extends ACifObject {
    /** The key of the key/value pair. */
    public final AExpression key;

    /** The value of the key/value pair. */
    public final AExpression value;

    /**
     * Constructor for the {@link ADictPair} class.
     *
     * @param key The key of the key/value pair.
     * @param value The value of the key/value pair.
     * @param position Position information.
     */
    public ADictPair(AExpression key, AExpression value, TextPosition position) {
        super(position);
        this.key = key;
        this.value = value;
    }
}
