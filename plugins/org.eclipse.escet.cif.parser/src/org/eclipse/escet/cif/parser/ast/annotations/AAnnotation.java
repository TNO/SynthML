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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.setext.runtime.Token;

/** Annotation. */
public class AAnnotation extends ACifObject {
    /** The name of the annotation. */
    public final Token name;

    /** The arguments of the annotation. */
    public final List<AAnnotationArgument> arguments;

    /**
     * Constructor for the {@link AAnnotation} class.
     *
     * @param name The name of the annotation.
     * @param arguments The arguments of the annotation.
     */
    public AAnnotation(Token name, List<AAnnotationArgument> arguments) {
        super(name.position);
        this.name = name;
        this.arguments = arguments;
    }
}
