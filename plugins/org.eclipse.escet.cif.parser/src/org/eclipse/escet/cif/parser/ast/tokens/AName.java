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
import org.eclipse.escet.common.java.TextPosition;

/** Reference name token. */
public class AName extends ACifObject {
    /** Reference name, without any {@code $} characters. */
    public final String name;

    /**
     * Constructor for the {@link AName} class.
     *
     * @param name Reference name. May include {@code $} characters.
     * @param position Position information.
     */
    public AName(String name, TextPosition position) {
        super(position);
        this.name = name.replace("$", "");
    }
}
