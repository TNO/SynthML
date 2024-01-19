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

/** Set type. */
public class ASetType extends ACifType {
    /** The element type of the set type. */
    public final ACifType elementType;

    /**
     * Constructor for the {@link ASetType} class.
     *
     * @param elementType The element type of the set type.
     * @param position Position information.
     */
    public ASetType(ACifType elementType, TextPosition position) {
        super(position);
        this.elementType = elementType;
    }
}
