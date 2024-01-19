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

import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.Token;

/** Event parameter. */
public class AEventParameter extends AParameter {
    /** The controllability of the parameters, or {@code null} if not specified. */
    public final Token controllability;

    /** The parts of the parameters. */
    public final List<AEventParameterPart> parts;

    /** The type of the event, or {@code null} if not applicable. */
    public final ACifType type;

    /**
     * Constructor for the {@link AEventParameter} class.
     *
     * @param controllability The controllability of the parameters, or {@code null} if not specified.
     * @param parts The parts of the parameters.
     * @param type The type of the event, or {@code null} if not applicable.
     * @param position Position information.
     */
    public AEventParameter(Token controllability, List<AEventParameterPart> parts, ACifType type,
            TextPosition position)
    {
        super(position);
        this.controllability = controllability;
        this.parts = parts;
        this.type = type;
    }
}
