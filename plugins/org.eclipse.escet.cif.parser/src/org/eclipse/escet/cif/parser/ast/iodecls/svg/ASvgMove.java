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

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.common.java.TextPosition;

/** An SVG move declaration. */
public class ASvgMove extends AIoDecl {
    /** The SVG element 'id' expression. */
    public final AExpression svgId;

    /** The x coordinate expression. */
    public final AExpression x;

    /** The y coordinate expression. */
    public final AExpression y;

    /** The SVG file to which the declaration applies, or {@code null} to inherit the SVG file. */
    public final ASvgFile svgFile;

    /**
     * Constructor for the {@link ASvgMove} class.
     *
     * @param svgId The SVG element 'id' expression.
     * @param x The x coordinate expression.
     * @param y The y coordinate expression.
     * @param svgFile The SVG file to which the declaration applies, or {@code null} to inherit the SVG file.
     * @param position Position information.
     */
    public ASvgMove(AExpression svgId, AExpression x, AExpression y, ASvgFile svgFile, TextPosition position) {
        super(position);
        this.svgId = svgId;
        this.x = x;
        this.y = y;
        this.svgFile = svgFile;
    }
}
