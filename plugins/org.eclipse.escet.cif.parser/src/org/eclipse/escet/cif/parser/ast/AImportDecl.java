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

import java.util.List;

import org.eclipse.escet.common.java.TextPosition;

/** Import declaration. */
public class AImportDecl extends ADecl {
    /** Imports. */
    public final List<AImport> imports;

    /**
     * Constructor for the {@link AImportDecl} class.
     *
     * @param imports Imports.
     * @param position Position information.
     */
    public AImportDecl(List<AImport> imports, TextPosition position) {
        super(position);
        this.imports = imports;
    }
}
