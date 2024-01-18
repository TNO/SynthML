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

package org.eclipse.escet.cif.parser.ast.iodecls.print;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;

/** A 'for' filter of a {@link APrint}. */
public class APrintFor extends ACifObject {
    /** The kind of the 'for' filter. */
    public final APrintForKind kind;

    /** The name of the 'for' filter, if {@link #kind} is {@link APrintForKind#NAME}, or {@code null} otherwise. */
    public final String name;

    /**
     * Constructor for the {@link APrintFor} class.
     *
     * @param kind The kind of the 'for' filter.
     * @param name The name of the 'for' filter, if {@link #kind} is {@link APrintForKind#NAME}, or {@code null}
     *     otherwise.
     * @param position Position information.
     */
    public APrintFor(APrintForKind kind, String name, TextPosition position) {
        super(position);
        this.kind = kind;
        this.name = name;
        Assert.ifAndOnlyIf(kind == APrintForKind.NAME, name != null);
    }
}
