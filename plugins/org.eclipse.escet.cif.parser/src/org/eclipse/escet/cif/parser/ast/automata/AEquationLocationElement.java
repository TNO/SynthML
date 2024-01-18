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

package org.eclipse.escet.cif.parser.ast.automata;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.AEquation;
import org.eclipse.escet.common.java.TextPosition;

/** Equations of a location. */
public class AEquationLocationElement extends ALocationElement {
    /** The equations of the location. */
    public final List<AEquation> equations;

    /**
     * Constructor for the {@link AEquationLocationElement} class.
     *
     * @param equations The equations of the location.
     * @param position Position information.
     */
    public AEquationLocationElement(List<AEquation> equations, TextPosition position) {
        super(position);
        this.equations = equations;
    }
}
