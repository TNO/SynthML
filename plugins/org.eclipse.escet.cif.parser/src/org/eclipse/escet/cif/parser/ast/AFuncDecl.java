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

import org.eclipse.escet.cif.parser.ast.functions.AFuncBody;
import org.eclipse.escet.cif.parser.ast.functions.AFuncParam;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.types.ACifType;
import org.eclipse.escet.common.java.TextPosition;

/** Function declaration. */
public class AFuncDecl extends ADecl {
    /** The name of the function declaration. */
    public final AIdentifier name;

    /** The return types of the function declaration. */
    public final List<ACifType> returnTypes;

    /** The parameters of the function declaration. */
    public final List<AFuncParam> parameters;

    /** The body of the function declaration. */
    public final AFuncBody body;

    /**
     * Constructor for the {@link AFuncDecl} class.
     *
     * @param name The name of the function declaration.
     * @param returnTypes The return types of the function declaration.
     * @param parameters The parameters of the function declaration.
     * @param body The body of the function declaration.
     * @param position Position information.
     */
    public AFuncDecl(AIdentifier name, List<ACifType> returnTypes, List<AFuncParam> parameters, AFuncBody body,
            TextPosition position)
    {
        super(position);
        this.name = name;
        this.returnTypes = returnTypes;
        this.parameters = parameters;
        this.body = body;
    }
}
