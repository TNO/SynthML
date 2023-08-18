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

package org.eclipse.escet.cif.parser.ast.automata;

import org.eclipse.escet.cif.parser.ast.AInvariantDecl;

/** Invariants of a location. */
public class AInvariantLocationElement extends ALocationElement {
    /** The invariant declaration. */
    public final AInvariantDecl invariantDecl;

    /**
     * Constructor for the {@link AInvariantLocationElement} class.
     *
     * @param invariantDecl The invariant declaration.
     */
    public AInvariantLocationElement(AInvariantDecl invariantDecl) {
        super(invariantDecl.position);
        this.invariantDecl = invariantDecl;
    }
}
