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

/** The body of a group or group definition. */
public class AGroupBody extends AComponentBody {
    /**
     * Constructor for the {@link AGroupBody} class.
     *
     * @param decls The declarations of the group or group definition.
     */
    public AGroupBody(List<ADecl> decls) {
        super(decls);
    }
}
