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
import org.eclipse.escet.common.java.TextPosition;

/** An SVG copy declaration. */
public class ASvgCopy extends AIoDecl {
    /** The SVG element 'id' expression. */
    public final AExpression svgId;

    /** The prefix text expression, or {@code null} if not specified. */
    public final AExpression pre;

    /** The postfix text expression, or {@code null} if not specified. */
    public final AExpression post;

    /** The SVG file to which the declaration applies, or {@code null} to inherit the SVG file. */
    public final ASvgFile svgFile;

    /**
     * Constructor for the {@link ASvgCopy} class.
     *
     * @param svgId The SVG element 'id' expression.
     * @param pre The prefix text expression, or {@code null} if not specified.
     * @param post The postfix text expression, or {@code null} if not specified.
     * @param svgFile The SVG file to which the declaration applies, or {@code null} to inherit the SVG file.
     * @param position Position information.
     */
    public ASvgCopy(AExpression svgId, AExpression pre, AExpression post, ASvgFile svgFile, TextPosition position) {
        super(position);
        this.svgId = svgId;
        this.pre = pre;
        this.post = post;
        this.svgFile = svgFile;
    }
}
