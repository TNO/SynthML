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

package org.eclipse.escet.cif.parser.ast.iodecls.svg;

import java.util.List;

import org.eclipse.escet.common.java.TextPosition;

/** A CIF/SVG input mapping event choice that uses an if/then/else construct to choose the event. */
public class ASvgInEventIf extends ASvgInEvent {
    /** The entries of the if/then/else. */
    public final List<ASvgInEventIfEntry> entries;

    /**
     * Constructor for the {@link ASvgInEventIf} class.
     *
     * @param entries The entries of the if/then/else.
     * @param position Position information.
     */
    public ASvgInEventIf(List<ASvgInEventIfEntry> entries, TextPosition position) {
        super(position);
        this.entries = entries;
    }
}
