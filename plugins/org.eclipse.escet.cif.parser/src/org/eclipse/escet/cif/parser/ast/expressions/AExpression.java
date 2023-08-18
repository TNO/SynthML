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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.common.java.TextPosition;

/** CIF expression. */
public abstract class AExpression extends ACifObject {
    /**
     * Constructor for the {@link AExpression} class.
     *
     * @param position Position information.
     */
    public AExpression(TextPosition position) {
        super(position);
    }
}
