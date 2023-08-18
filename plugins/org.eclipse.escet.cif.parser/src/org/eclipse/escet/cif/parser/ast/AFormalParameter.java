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

package org.eclipse.escet.cif.parser.ast;

import org.eclipse.escet.common.java.TextPosition;

/** Formal parameter for a component definition. */
public abstract class AFormalParameter extends ACifObject {
    /**
     * Constructor for the {@link AFormalParameter} class.
     *
     * @param position Position information.
     */
    public AFormalParameter(TextPosition position) {
        super(position);
    }
}
