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

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** Component instantiation. */
public class ACompInstDecl extends ADecl {
    /** The name of the component instantiation. */
    public final AIdentifier instName;

    /** The name of the component definition to instantiate. */
    public final AName defName;

    /** The parameters of the component instantiation. */
    public final List<AExpression> parameters;

    /**
     * Constructor for the {@link ACompInstDecl} class.
     *
     * @param instName The name of the component instantiation.
     * @param defName The name of the component definition to instantiate.
     * @param parameters The parameters of the component instantiation.
     * @param position Position information.
     */
    public ACompInstDecl(AIdentifier instName, AName defName, List<AExpression> parameters, TextPosition position) {
        super(position);
        this.instName = instName;
        this.defName = defName;
        this.parameters = parameters;
    }
}
