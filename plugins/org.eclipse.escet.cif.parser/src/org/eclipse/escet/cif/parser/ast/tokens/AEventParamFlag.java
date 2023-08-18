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

package org.eclipse.escet.cif.parser.ast.tokens;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.TextPosition;

/** Event parameter flag token. */
public class AEventParamFlag extends ACifObject {
    /** The flag. Must be {@code "!"}, {@code "?"}, or {@code "~"}. */
    public final String flag;

    /**
     * Constructor for the {@link AEventParamFlag} class.
     *
     * @param flag The flag. Must be {@code "!"}, {@code "?"}, or {@code "~"}.
     * @param position Position information.
     */
    public AEventParamFlag(String flag, TextPosition position) {
        super(position);
        this.flag = flag;
    }
}
