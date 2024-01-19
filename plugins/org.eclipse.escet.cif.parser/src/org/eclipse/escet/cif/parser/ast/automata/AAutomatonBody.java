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

import org.eclipse.escet.cif.parser.ast.AComponentBody;
import org.eclipse.escet.cif.parser.ast.ADecl;

/** The body of an automaton or automaton definition. */
public class AAutomatonBody extends AComponentBody {
    /** The locations of the automaton or automaton definition. */
    public final List<ALocation> locations;

    /**
     * Constructor for the {@link AAutomatonBody} class.
     *
     * @param decls The declarations of the automaton or automaton definition, excluding the locations.
     * @param locations The locations of the automaton or automaton definition.
     */
    public AAutomatonBody(List<ADecl> decls, List<ALocation> locations) {
        super(decls);
        this.locations = locations;
    }
}
