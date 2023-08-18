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
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Constant declaration. */
public class AConstDecl extends ADecl {
    /** The type of the constant declaration. */
    public final ACifType type;

    /** The constants that are part of this constant declaration. */
    public final List<AConstant> constants;

    /**
     * Constructor for the {@link AConstDecl} class.
     *
     * @param type The type of the constant declaration.
     * @param constants The constants that are part of this constant declaration.
     * @param position Position information.
     */
    public AConstDecl(ACifType type, List<AConstant> constants, TextPosition position) {
        super(position);
        this.type = type;
        this.constants = constants;
    }
}
