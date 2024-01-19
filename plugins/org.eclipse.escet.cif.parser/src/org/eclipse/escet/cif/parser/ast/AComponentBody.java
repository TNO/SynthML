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

package org.eclipse.escet.cif.parser.ast;

import java.util.List;

/** The body of a component or component definition. */
public abstract class AComponentBody extends ACifObject {
    /** The declarations of the component or component definition. */
    public final List<ADecl> decls;

    /**
     * Constructor for the {@link AComponentBody} class.
     *
     * @param decls The declarations of the component or component definition.
     */
    public AComponentBody(List<ADecl> decls) {
        super(null);
        this.decls = decls;
    }
}
