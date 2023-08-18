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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.iodecls.AIoDecl;
import org.eclipse.escet.common.java.TextPosition;

/** A print I/O declaration. */
public class APrint extends AIoDecl {
    /** The text(s) to print. */
    public final APrintTxt txt;

    /** The 'for' filters to use. May be empty. */
    public final List<APrintFor> fors;

    /** The 'when' filters to use, or {@code null} if not available. */
    public final APrintWhen when;

    /** The 'file' to use, or {@code null} if not available. */
    public final APrintFile file;

    /**
     * Constructor for the {@link APrint} class.
     *
     * @param txt The text(s) to print.
     * @param fors The 'for' filters to use. May be empty.
     * @param when The 'when' filters to use, or {@code null} if not available.
     * @param file The 'file' to use, or {@code null} if not available.
     * @param position Position information.
     */
    public APrint(APrintTxt txt, List<APrintFor> fors, APrintWhen when, APrintFile file, TextPosition position) {
        super(position);
        this.txt = txt;
        this.fors = fors;
        this.when = when;
        this.file = file;
    }
}
