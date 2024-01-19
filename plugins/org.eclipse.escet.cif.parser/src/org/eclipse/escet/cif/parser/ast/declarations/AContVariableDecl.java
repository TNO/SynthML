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
import org.eclipse.escet.common.java.TextPosition;

/** Continuous variable declaration. */
public class AContVariableDecl extends ADecl {
    /** The continuous variables that are part of this continuous variable declaration. */
    public final List<AContVariable> variables;

    /**
     * Constructor for the {@link AContVariableDecl} class.
     *
     * @param variables The continuous variables that are part of this continuous variable declaration.
     * @param position Position information.
     */
    public AContVariableDecl(List<AContVariable> variables, TextPosition position) {
        super(position);
        this.variables = variables;
    }
}
