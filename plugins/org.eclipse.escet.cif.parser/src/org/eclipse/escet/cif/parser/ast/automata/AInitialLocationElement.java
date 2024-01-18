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

package org.eclipse.escet.cif.parser.ast.automata;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Initialization predicates of a location. */
public class AInitialLocationElement extends ALocationElement {
    /** The initialization predicates of the location, or {@code null}. */
    public final List<AExpression> preds;

    /**
     * Constructor for the {@link AInitialLocationElement} class.
     *
     * @param preds The initialization predicates of the location, or {@code null}.
     * @param position Position information.
     */
    public AInitialLocationElement(List<AExpression> preds, TextPosition position) {
        super(position);
        this.preds = preds;
    }
}
