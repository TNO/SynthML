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

package org.eclipse.escet.cif.parser.ast;

import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.Token;

/** Component. */
public class ACompDecl extends ADecl {
    /** Automaton supervisory kind, or {@code null} for groups or if not specified. */
    public final Token kind;

    /** The name of the component. */
    public final AIdentifier name;

    /** The body of the component. */
    public final AComponentBody body;

    /**
     * Constructor for the {@link ACompDecl} class.
     *
     * @param kind Automaton supervisory kind, or {@code null} for groups or if not specified.
     * @param name The name of the component.
     * @param body The body of the component.
     * @param position Position information.
     */
    public ACompDecl(Token kind, AIdentifier name, AComponentBody body, TextPosition position) {
        super(position);
        this.kind = kind;
        this.name = name;
        this.body = body;
    }
}
