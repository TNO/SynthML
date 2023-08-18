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

package org.eclipse.escet.cif.parser.ast.functions;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.TextPosition;

/** An 'else' part of an 'if' internal function statement. */
public class AElseFuncStatement extends ACifObject {
    /** The statements of the 'else'. */
    public final List<AFuncStatement> elses;

    /**
     * Constructor for the {@link AElseFuncStatement} class.
     *
     * @param elses The statements of the 'else'.
     * @param position Position information.
     */
    public AElseFuncStatement(List<AFuncStatement> elses, TextPosition position) {
        super(position);
        this.elses = elses;
    }
}
