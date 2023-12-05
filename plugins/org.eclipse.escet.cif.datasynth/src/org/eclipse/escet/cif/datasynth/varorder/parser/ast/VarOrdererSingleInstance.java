//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.varorder.parser.ast;

import java.util.List;

import org.eclipse.escet.setext.runtime.Token;

/** Single variable orderer instance. */
public class VarOrdererSingleInstance extends VarOrdererInstance {
    /** The name of the variable orderer. */
    public final Token name;

    /** The arguments of the variable orderer. */
    public final List<VarOrdererArg> arguments;

    /** Whether argument parentheses were given ({@code true}) or just an identifier ({@code false}). */
    public final boolean hasArgs;

    /**
     * Constructor for the {@link VarOrdererSingleInstance} class.
     *
     * @param name The name of the variable orderer.
     * @param arguments The arguments of the variable orderer.
     * @param hasArgs Whether argument parentheses were given ({@code true}) or just an identifier ({@code false}).
     */
    public VarOrdererSingleInstance(Token name, List<VarOrdererArg> arguments, boolean hasArgs) {
        super(name.position);
        this.name = name;
        this.arguments = arguments;
        this.hasArgs = hasArgs;
    }
}
