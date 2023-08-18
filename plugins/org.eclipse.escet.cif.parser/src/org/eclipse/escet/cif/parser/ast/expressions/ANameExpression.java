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

package org.eclipse.escet.cif.parser.ast.expressions;

import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;

/** Name/reference expression. */
public class ANameExpression extends AExpression {
    /** The name of the name expression. */
    public final AName name;

    /** Whether the reference is a derivative reference. */
    public final boolean derivative;

    /**
     * Constructor for the {@link ANameExpression} class.
     *
     * @param name The name of the name expression.
     * @param derivative Whether the reference is a derivative reference.
     * @param position Position information.
     */
    public ANameExpression(AName name, boolean derivative, TextPosition position) {
        super(position);
        this.name = name;
        this.derivative = derivative;
    }
}
