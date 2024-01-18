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

/** Discrete variable declaration. Also used for local variables of internal functions. */
public class ADiscVariableDecl extends ADecl {
    /** The annotations of the discrete variables. */
    public final List<AAnnotation> annotations;

    /** The type of the discrete variables. */
    public final ACifType type;

    /** The discrete variables that are part of this discrete variable declaration. */
    public final List<ADiscVariable> variables;

    /**
     * Constructor for the {@link ADiscVariableDecl} class.
     *
     * @param annotations The annotations of the discrete variables.
     * @param type The type of the discrete variables.
     * @param variables The discrete variables that are part of this discrete variable declaration.
     * @param position Position information, or {@code null} for local variables of functions.
     */
    public ADiscVariableDecl(List<AAnnotation> annotations, ACifType type, List<ADiscVariable> variables,
            TextPosition position)
    {
        super(position);
        this.annotations = annotations;
        this.type = type;
        this.variables = variables;
    }
}
