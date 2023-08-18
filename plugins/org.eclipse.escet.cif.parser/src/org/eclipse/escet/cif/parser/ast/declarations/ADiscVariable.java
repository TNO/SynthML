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

package org.eclipse.escet.cif.parser.ast.declarations;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;

/** Discrete variable. Also used for local variables of internal functions. */
public class ADiscVariable extends ACifObject {
    /** The name of the discrete variable. */
    public final AIdentifier name;

    /** The initial value of the discrete variable, or {@code null} for the default value. */
    public final AVariableValue value;

    /**
     * Constructor for the {@link ADiscVariable} class.
     *
     * @param name The name of the discrete variable.
     * @param value The initial value of the discrete variable, or {@code null} for the default value.
     * @param position Position information.
     */
    public ADiscVariable(AIdentifier name, AVariableValue value, TextPosition position) {
        super(position);
        this.name = name;
        this.value = value;
    }
}
