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

package org.eclipse.escet.cif.parser.ast.tokens;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.escet.common.java.TextPosition;

/** CIF string literal token. No escape sequences are recognized. */
public class AStringToken extends ACifObject {
    /** String text, excluding the surrounding quotes, and with escape sequences replaced by their actual characters. */
    public final String txt;

    /**
     * Constructor for the {@link AStringToken} class.
     *
     * @param txt String text, including the surrounding quotes, and escape sequences.
     * @param position Position information.
     */
    public AStringToken(String txt, TextPosition position) {
        this(txt, position, true);
    }

    /**
     * Constructor for the {@link AStringToken} class.
     *
     * @param txt String text. If 'process' is {@code true}, must including the surrounding quotes, and escape
     *     sequences. Otherwise, must not include the surrounding quotes, and must not contain escape sequences.
     * @param position Position information.
     * @param process Whether to remove to the surrounding quotes, and escape sequences from the 'txt'.
     */
    public AStringToken(String txt, TextPosition position, boolean process) {
        super(position);
        this.txt = process ? Strings.unescape(Strings.slice(txt, 1, -1)) : txt;
    }
}
