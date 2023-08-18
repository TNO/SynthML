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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.Token;

/** Component definition. */
public class ACompDefDecl extends ADecl {
    /** Automaton supervisory kind, or {@code null} for group definitions or if not specified. */
    public final Token kind;

    /** The name of the component definition. */
    public final AIdentifier name;

    /** The formal parameters of the component definition. */
    public final List<AFormalParameter> parameters;

    /** The body of the component definition. */
    public final AComponentBody body;

    /**
     * Constructor for the {@link ACompDefDecl} class.
     *
     * @param kind Automaton supervisory kind, or {@code null} for group definitions or if not specified.
     * @param name The name of the component definition.
     * @param parameters The formal parameters of the component definition.
     * @param body The body of the component definition.
     * @param position Position information.
     */
    public ACompDefDecl(Token kind, AIdentifier name, List<AFormalParameter> parameters, AComponentBody body,
            TextPosition position)
    {
        super(position);
        this.kind = kind;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}
