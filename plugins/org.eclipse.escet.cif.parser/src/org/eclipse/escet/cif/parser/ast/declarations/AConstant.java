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

package org.eclipse.escet.cif.parser.ast.declarations;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;

/** Constant. */
public class AConstant extends ACifObject {
    /** The name of the constant. */
    public final AIdentifier name;

    /** The value of the constant. */
    public final AExpression value;

    /**
     * Constructor for the {@link AConstant} class.
     *
     * @param name The name of the constant.
     * @param value The value of the constant.
     * @param position Position information.
     */
    public AConstant(AIdentifier name, AExpression value, TextPosition position) {
        super(position);
        this.name = name;
        this.value = value;
    }
}
