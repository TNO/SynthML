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

/** Parameter for a component definition. */
public abstract class AParameter extends ACifObject {
    /**
     * Constructor for the {@link AParameter} class.
     *
     * @param position Position information.
     */
    public AParameter(TextPosition position) {
        super(position);
    }
}
