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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** An entry of a {@link ASvgInEventIf}. */
public class ASvgInEventIfEntry extends ACifObject {
    /** The guard value, or {@code null} for 'else'. */
    public final AExpression guard;

    /** The textual reference to the event. */
    public final AName name;

    /**
     * Constructor for the {@link ASvgInEventIfEntry} class.
     *
     * @param guard The guard value, or {@code null} for 'else'.
     * @param name The textual reference to the event.
     * @param position Position information.
     */
    public ASvgInEventIfEntry(AExpression guard, AName name, TextPosition position) {
        super(position);
        this.guard = guard;
        this.name = name;
    }
}
