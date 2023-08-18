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

import org.eclipse.escet.common.java.TextPosition;

/** Tuple type. */
public class ATupleType extends ACifType {
    /** The fields of the tuple type. */
    public final List<AField> fields;

    /**
     * Constructor for the {@link ATupleType} class.
     *
     * @param fields The fields of the tuple type.
     * @param position Position information.
     */
    public ATupleType(List<AField> fields, TextPosition position) {
        super(position);
        this.fields = fields;
    }
}
