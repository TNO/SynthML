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

package org.eclipse.escet.cif.parser.ast.declarations;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Type definition. */
public class ATypeDef extends ACifObject {
    /** The type of the type definition. */
    public final ACifType type;

    /** The name of the type definition. */
    public final AIdentifier name;

    /**
     * Constructor for the {@link ATypeDef} class.
     *
     * @param type The type of the type definition.
     * @param name The name of the type definition.
     * @param position Position information.
     */
    public ATypeDef(ACifType type, AIdentifier name, TextPosition position) {
        super(position);
        this.type = type;
        this.name = name;
    }
}
