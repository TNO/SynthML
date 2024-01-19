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

package org.eclipse.escet.cif.parser.ast.types;

import org.eclipse.escet.common.java.TextPosition;

/** Dictionary type. */
public class ADictType extends ACifType {
    /** The key type of the dictionary type. */
    public final ACifType keyType;

    /** The value type of the dictionary type. */
    public final ACifType valueType;

    /**
     * Constructor for the {@link ADictType} class.
     *
     * @param keyType The key type of the dictionary type.
     * @param valueType The value type of the dictionary type.
     * @param position Position information.
     */
    public ADictType(ACifType keyType, ACifType valueType, TextPosition position) {
        super(position);
        this.keyType = keyType;
        this.valueType = valueType;
    }
}
