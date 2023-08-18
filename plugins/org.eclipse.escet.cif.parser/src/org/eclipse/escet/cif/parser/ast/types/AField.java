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

package org.eclipse.escet.cif.parser.ast.types;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;

/** Tuple fields. */
public class AField extends ACifObject {
    /** The names of the fields. */
    public final List<AIdentifier> names;

    /** The type of the field. */
    public final ACifType type;

    /**
     * Constructor for the {@link AField} class.
     *
     * @param names The names of the fields.
     * @param type The type of the field.
     */
    public AField(List<AIdentifier> names, ACifType type) {
        super(null);
        this.names = names;
        this.type = type;
    }
}
