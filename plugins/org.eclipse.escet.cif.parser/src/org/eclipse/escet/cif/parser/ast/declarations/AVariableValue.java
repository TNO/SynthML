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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;

/** Initial values for an {@link ADiscVariable}. */
public class AVariableValue extends ACifObject {
    /**
     * The possible initial values for the variable, or {@code null} to indicate that any value in its domain is a valid
     * initial value.
     */
    public final List<AExpression> values;

    /**
     * Constructor for the {@link AVariableValue} class.
     *
     * @param values The possible initial values for the variable, or {@code null} to indicate that any value in its
     *     domain is a valid initial value.
     * @param position Position information.
     */
    public AVariableValue(List<AExpression> values, TextPosition position) {
        super(position);
        Assert.check(values == null || values.size() > 0);
        this.values = values;
    }
}
