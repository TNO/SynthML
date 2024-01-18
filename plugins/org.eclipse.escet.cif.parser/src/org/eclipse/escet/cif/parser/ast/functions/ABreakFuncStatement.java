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

package org.eclipse.escet.cif.parser.ast.functions;

import org.eclipse.escet.common.java.TextPosition;

/** Break internal function statement. */
public class ABreakFuncStatement extends AFuncStatement {
    /**
     * Constructor for the {@link ABreakFuncStatement} class.
     *
     * @param position Position information.
     */
    public ABreakFuncStatement(TextPosition position) {
        super(position);
    }
}
