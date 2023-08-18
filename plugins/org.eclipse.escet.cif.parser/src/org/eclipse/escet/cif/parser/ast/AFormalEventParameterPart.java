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

import org.eclipse.escet.cif.parser.ast.tokens.AEventParamFlag;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;

/** Formal event parameter part. */
public class AFormalEventParameterPart extends ACifObject {
    /** The name of the parameter. */
    public final AIdentifier name;

    /** The flags of the parameter. */
    public final List<AEventParamFlag> flags;

    /**
     * Constructor for the {@link AFormalEventParameterPart} class.
     *
     * @param name The name of the parameter.
     * @param flags The flags of the parameter.
     */
    public AFormalEventParameterPart(AIdentifier name, List<AEventParamFlag> flags) {
        super(null);
        this.name = name;
        this.flags = flags;
    }
}
