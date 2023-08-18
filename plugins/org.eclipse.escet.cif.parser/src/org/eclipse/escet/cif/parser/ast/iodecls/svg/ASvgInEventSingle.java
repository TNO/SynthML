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

package org.eclipse.escet.cif.parser.ast.iodecls.svg;

import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** A CIF/SVG input mapping event choice that chooses a single fixed event. */
public class ASvgInEventSingle extends ASvgInEvent {
    /** The textual reference to the event. */
    public final AName name;

    /**
     * Constructor for the {@link ASvgInEventSingle} class.
     *
     * @param name The textual reference to the event.
     * @param position Position information.
     */
    public ASvgInEventSingle(AName name, TextPosition position) {
        super(position);
        this.name = name;
    }
}
