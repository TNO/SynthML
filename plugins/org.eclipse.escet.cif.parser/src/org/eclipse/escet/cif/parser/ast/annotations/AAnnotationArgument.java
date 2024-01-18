//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.parser.ast.annotations;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.Token;

/** Annotation argument. */
public class AAnnotationArgument extends ACifObject {
    /** The name of the annotation argument. */
    public final Token name;

    /** The value of the annotation argument. */
    public final AExpression value;

    /**
     * Constructor for the {@link AAnnotationArgument} class.
     *
     * @param name The name of the annotation argument.
     * @param value The value of the annotation argument.
     */
    public AAnnotationArgument(Token name, AExpression value) {
        super(name.position);
        this.name = name;
        this.value = value;
    }
}
