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

import org.eclipse.escet.common.java.TextPosition;

/** CIF declaration. Note that it is much broader than the declarations package in the metamodel. */
public abstract class ADecl extends ACifObject {
    /**
     * Constructor for the {@link ADecl} class.
     *
     * @param position Position information.
     */
    public ADecl(TextPosition position) {
        super(position);
    }
}
