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

import org.eclipse.escet.common.java.TextPosition;

/** One or more variable orderer instances. */
public abstract class VarOrdererInstance {
    /** The position of the variable orderer instance(s). */
    public final TextPosition position;

    /**
     * Constructor for the {@link VarOrdererInstance} class.
     *
     * @param position The position of the variable orderer instance(s).
     */
    public VarOrdererInstance(TextPosition position) {
        this.position = position;
    }
}
