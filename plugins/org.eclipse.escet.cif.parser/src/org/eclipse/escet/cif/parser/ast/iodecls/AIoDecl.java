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

package org.eclipse.escet.cif.parser.ast.iodecls;

import org.eclipse.escet.cif.parser.ast.ADecl;
import org.eclipse.escet.common.java.TextPosition;

/** CIF I/O declaration. */
public abstract class AIoDecl extends ADecl {
    /**
     * Constructor for the {@link AIoDecl} class.
     *
     * @param position Position information.
     */
    public AIoDecl(TextPosition position) {
        super(position);
    }
}
