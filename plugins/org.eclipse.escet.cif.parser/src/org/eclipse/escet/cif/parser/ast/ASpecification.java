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

/** Specification. */
public class ASpecification extends ACifObject {
    /** The body of the specification. */
    public final AGroupBody body;

    /**
     * Constructor for the {@link ASpecification} class.
     *
     * @param body The body of the specification.
     * @param position Position information.
     */
    public ASpecification(AGroupBody body, TextPosition position) {
        super(position);
        this.body = body;
    }
}
