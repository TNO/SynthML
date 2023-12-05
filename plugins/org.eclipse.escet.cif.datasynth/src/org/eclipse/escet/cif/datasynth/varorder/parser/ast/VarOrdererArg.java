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

import org.eclipse.escet.setext.runtime.Token;

/** Argument of a variable orderer. */
public abstract class VarOrdererArg {
    /** The name of the argument. */
    public final Token name;

    /**
     * Constructor for the {@link VarOrdererArg} class.
     *
     * @param name The name of the argument.
     */
    public VarOrdererArg(Token name) {
        this.name = name;
    }
}
