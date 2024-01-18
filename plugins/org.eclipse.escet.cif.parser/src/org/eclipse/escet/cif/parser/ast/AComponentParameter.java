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

package org.eclipse.escet.cif.parser.ast;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** Component parameter. */
public class AComponentParameter extends AParameter {
    /** The component definition type of the parameter. */
    public final AName type;

    /** The names of the parameters. */
    public final List<AIdentifier> names;

    /**
     * Constructor for the {@link AComponentParameter} class.
     *
     * @param type The component definition type of the parameter.
     * @param names The names of the parameters.
     * @param position Position information.
     */
    public AComponentParameter(AName type, List<AIdentifier> names, TextPosition position) {
        super(position);
        this.type = type;
        this.names = names;
    }
}
