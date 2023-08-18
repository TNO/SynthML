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

package org.eclipse.escet.cif.parser.ast.declarations;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ADecl;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Input variable declaration. */
public class AInputVariableDecl extends ADecl {
    /** The type of the input variables. */
    public final ACifType type;

    /** The names of the input variables. */
    public final List<AIdentifier> names;

    /**
     * Constructor for the {@link AInputVariableDecl} class.
     *
     * @param type The type of the input variable declaration.
     * @param names The names of the input variables.
     * @param position Position information.
     */
    public AInputVariableDecl(ACifType type, List<AIdentifier> names, TextPosition position) {
        super(position);
        this.type = type;
        this.names = names;
    }
}
