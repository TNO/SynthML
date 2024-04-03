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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ADecl;
import org.eclipse.escet.cif.parser.ast.annotations.AAnnotation;
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Algebraic variable declaration. */
public class AAlgVariableDecl extends ADecl {
    /** The annotations of the algebraic variables. */
    public final List<AAnnotation> annotations;

    /** The type of the algebraic variable declaration. */
    public final ACifType type;

    /** The algebraic variables that are part of this algebraic variable declaration. */
    public final List<AAlgVariable> variables;

    /**
     * Constructor for the {@link AAlgVariableDecl} class.
     *
     * @param annotations The annotations of the algebraic variables.
     * @param type The type of the algebraic variable declaration.
     * @param variables The algebraic variables that are part of this algebraic variable declaration.
     * @param position Position information.
     */
    public AAlgVariableDecl(List<AAnnotation> annotations, ACifType type, List<AAlgVariable> variables,
            TextPosition position)
    {
        super(position);
        this.annotations = annotations;
        this.type = type;
        this.variables = variables;
    }
}
