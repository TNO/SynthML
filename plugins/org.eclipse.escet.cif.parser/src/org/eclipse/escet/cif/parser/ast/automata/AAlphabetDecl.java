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

import org.eclipse.escet.cif.parser.ast.ADecl;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** Alphabet. */
public class AAlphabetDecl extends ADecl {
    /** The events of the alphabet, or {@code null}. */
    public final List<AName> events;

    /**
     * Constructor for the {@link AAlphabetDecl} class.
     *
     * @param events The events of the alphabet, or {@code null}.
     * @param position Position information.
     */
    public AAlphabetDecl(List<AName> events, TextPosition position) {
        super(position);
        this.events = events;
    }
}
