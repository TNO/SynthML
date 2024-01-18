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

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;

/** Initial predicates. */
public class AInitialDecl extends ADecl {
    /** Initialization predicates. */
    public final List<AExpression> preds;

    /**
     * Constructor for the {@link AInitialDecl} class.
     *
     * @param preds Initialization predicates.
     * @param position Position information.
     */
    public AInitialDecl(List<AExpression> preds, TextPosition position) {
        super(position);
        this.preds = preds;
        Assert.check(!preds.isEmpty());
    }
}
