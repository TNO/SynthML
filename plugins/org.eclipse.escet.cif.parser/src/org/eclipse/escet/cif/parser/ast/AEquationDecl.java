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

package org.eclipse.escet.cif.parser.ast;

import java.util.List;

import org.eclipse.escet.common.java.TextPosition;

/** Equations. */
public class AEquationDecl extends ADecl {
    /** Equations. */
    public final List<AEquation> equations;

    /**
     * Constructor for the {@link AEquationDecl} class.
     *
     * @param equations Equations.
     * @param position Position information.
     */
    public AEquationDecl(List<AEquation> equations, TextPosition position) {
        super(position);
        this.equations = equations;
    }
}
