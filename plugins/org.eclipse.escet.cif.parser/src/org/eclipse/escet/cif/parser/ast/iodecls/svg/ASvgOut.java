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

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.cif.parser.ast.tokens.AStringToken;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;

/** A CIF/SVG output mapping. */
public class ASvgOut extends AIoDecl {
    /** The SVG element 'id' expression. */
    public final AExpression svgId;

    /** The SVG attribute name, or {@code null} to set text instead. */
    public final AStringToken svgAttr;

    /** The position of the {@code text} keyword, or {@code null} to set an attribute instead. */
    public final TextPosition svgTextPos;

    /** The expression to evaluate to obtain the value to set. */
    public final AExpression value;

    /** The SVG file to which the mapping applies, or {@code null} to inherit the SVG file. */
    public final ASvgFile svgFile;

    /**
     * Constructor for the {@link ASvgOut} class.
     *
     * @param svgId The SVG element 'id' expression.
     * @param svgAttr The SVG attribute name, or {@code null} to set text instead.
     * @param svgTextPos The position of the {@code text} keyword, or {@code null} to set an attribute instead.
     * @param value The expression to evaluate to obtain the value to set.
     * @param svgFile The SVG file to which the mapping applies, or {@code null} to inherit the SVG file.
     * @param position Position information.
     */
    public ASvgOut(AExpression svgId, AStringToken svgAttr, TextPosition svgTextPos, AExpression value,
            ASvgFile svgFile, TextPosition position)
    {
        super(position);
        this.svgId = svgId;
        this.svgAttr = svgAttr;
        this.svgTextPos = svgTextPos;
        this.value = value;
        this.svgFile = svgFile;

        Assert.check((svgAttr == null) != (svgTextPos == null));
    }
}
