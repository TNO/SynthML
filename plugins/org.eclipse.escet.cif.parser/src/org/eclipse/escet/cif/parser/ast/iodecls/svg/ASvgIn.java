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

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.common.java.TextPosition;

/** A CIF/SVG input mapping. */
public class ASvgIn extends AIoDecl {
    /** The SVG element 'id' expression. */
    public final AExpression svgId;

    /** The event to choose for this input mapping. */
    public final ASvgInEvent event;

    /** The updates for this input mapping. */
    public final List<AUpdate> updates;

    /** The SVG file to which the mapping applies, or {@code null} to inherit the SVG file. */
    public final ASvgFile svgFile;

    /**
     * Constructor for the {@link ASvgIn} class.
     *
     * @param svgId The SVG element 'id' expression.
     * @param event The event to choose for this input mapping.
     * @param updates The updates for this input mapping.
     * @param svgFile The SVG file to which the mapping applies, or {@code null} to inherit the SVG file.
     * @param position Position information.
     */
    public ASvgIn(AExpression svgId, ASvgInEvent event, List<AUpdate> updates, ASvgFile svgFile, TextPosition position) {
        super(position);
        this.svgId = svgId;
        this.event = event;
        this.updates = updates;
        this.svgFile = svgFile;
    }
}
