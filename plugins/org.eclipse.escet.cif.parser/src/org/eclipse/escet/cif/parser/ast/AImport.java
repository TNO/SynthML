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

import org.eclipse.escet.cif.parser.ast.tokens.AStringToken;
import org.eclipse.escet.common.java.TextPosition;

/** Import. */
public class AImport extends ACifObject {
    /**
     * The absolute or relative local file system path to the CIF source file to import, allowing both {@code "/"} and
     * {@code "\"} as file separators.
     */
    public final AStringToken source;

    /**
     * Constructor for the {@link AImport} class.
     *
     * @param source The absolute or relative local file system path to the CIF source file to import, allowing both
     *     {@code "/"} and {@code "\"} as file separators.
     * @param position Position information.
     */
    public AImport(AStringToken source, TextPosition position) {
        super(position);
        this.source = source;
    }
}
