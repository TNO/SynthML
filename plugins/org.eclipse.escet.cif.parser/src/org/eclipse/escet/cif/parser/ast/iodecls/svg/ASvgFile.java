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

import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.cif.parser.ast.tokens.AStringToken;
import org.eclipse.escet.common.java.TextPosition;

/** An SVG file I/O declaration. */
public class ASvgFile extends AIoDecl {
    /**
     * The absolute or relative local file system path to the SVG image, allowing both {@code "/"} and {@code "\"} as
     * file separators.
     */
    public final AStringToken svgPath;

    /**
     * Constructor for the {@link ASvgFile} class.
     *
     * @param svgPath The absolute or relative local file system path to the SVG image, allowing both {@code "/"} and
     *     {@code "\"} as file separators.
     * @param position Position information.
     */
    public ASvgFile(AStringToken svgPath, TextPosition position) {
        super(position);
        this.svgPath = svgPath;
    }
}
