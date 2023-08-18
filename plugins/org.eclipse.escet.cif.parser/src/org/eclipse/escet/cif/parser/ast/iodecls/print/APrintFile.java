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

package org.eclipse.escet.cif.parser.ast.iodecls.print;

import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.cif.parser.ast.tokens.AStringToken;
import org.eclipse.escet.common.java.TextPosition;

/** A print file I/O declaration. */
public class APrintFile extends AIoDecl {
    /**
     * The absolute or relative local file system path to the file to which to print, allowing both {@code "/"} and
     * {@code "\"} as file separators.
     */
    public final AStringToken path;

    /**
     * Constructor for the {@link APrintFile} class.
     *
     * @param path The absolute or relative local file system path to the file to which to print, allowing both
     *     {@code "/"} and {@code "\"} as file separators.
     * @param position Position information.
     */
    public APrintFile(AStringToken path, TextPosition position) {
        super(position);
        this.path = path;
    }
}
