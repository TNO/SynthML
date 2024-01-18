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

import org.eclipse.escet.cif.parser.ast.tokens.AEventParamFlag;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;

/** Event parameter part. */
public class AEventParameterPart extends ACifObject {
    /** The name of the parameter. */
    public final AIdentifier name;

    /** The flags of the parameter. */
    public final List<AEventParamFlag> flags;

    /**
     * Constructor for the {@link AEventParameterPart} class.
     *
     * @param name The name of the parameter.
     * @param flags The flags of the parameter.
     */
    public AEventParameterPart(AIdentifier name, List<AEventParamFlag> flags) {
        super(null);
        this.name = name;
        this.flags = flags;
    }
}
