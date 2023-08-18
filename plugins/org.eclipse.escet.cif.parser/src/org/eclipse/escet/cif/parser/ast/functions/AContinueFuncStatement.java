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

import org.eclipse.escet.common.java.TextPosition;

/** Continue internal function statement. */
public class AContinueFuncStatement extends AFuncStatement {
    /**
     * Constructor for the {@link AContinueFuncStatement} class.
     *
     * @param position Position information.
     */
    public AContinueFuncStatement(TextPosition position) {
        super(position);
    }
}
