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

/** Argument of a variable orderer that has as value one or more variable orderer instances. */
public class VarOrdererOrdererArg extends VarOrdererArg {
    /** The value of the argument. */
    public final VarOrdererInstance value;

    /**
     * Constructor for the {@link VarOrdererOrdererArg} class.
     *
     * @param name The name of the argument.
     * @param value The value of the argument.
     */
    public VarOrdererOrdererArg(Token name, VarOrdererInstance value) {
        super(name);
        this.value = value;
    }
}
