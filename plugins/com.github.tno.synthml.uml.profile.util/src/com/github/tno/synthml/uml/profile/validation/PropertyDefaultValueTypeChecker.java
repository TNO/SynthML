////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.validation;

import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifTypeChecker;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;
import com.github.tno.synthml.uml.profile.cif.TypeException;

public class PropertyDefaultValueTypeChecker extends CifTypeChecker {
    private static final String MESSAGE = "only literals are supported for property default values.";

    /**
     * Constructs a new property default value type checker.
     *
     * @param ctx The context for evaluating the expression.
     */
    public PropertyDefaultValueTypeChecker(CifContext ctx) {
        super(ctx);
    }

    @Override
    protected Type visit(Property property, TextPosition propertyPos, CifContext ctx) {
        throw new TypeException(MESSAGE, propertyPos);
    }

    @Override
    protected Type visit(BinaryOperator operator, TextPosition operatorPos, Type left, Type right, CifContext ctx) {
        throw new TypeException(MESSAGE, operatorPos);
    }

    @Override
    protected Type visit(UnaryOperator operator, TextPosition operatorPos, Type child, CifContext ctx) {
        throw new TypeException(MESSAGE, operatorPos);
    }

    @Override
    protected Type visit(NamedTemplateParameter parameter, TextPosition parameterReferencePos, CifContext ctx) {
        throw new TypeException(MESSAGE, parameterReferencePos);
    }
}
