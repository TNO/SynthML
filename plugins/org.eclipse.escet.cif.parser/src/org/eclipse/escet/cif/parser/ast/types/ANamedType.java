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

import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** Named type (type reference, enumeration reference, etc). */
public class ANamedType extends ACifType {
    /** The name of the named type. */
    public final AName name;

    /**
     * Constructor for the {@link ANamedType} class.
     *
     * @param name The name of the named type.
     * @param position Position information.
     */
    public ANamedType(AName name, TextPosition position) {
        super(position);
        this.name = name;
    }
}
