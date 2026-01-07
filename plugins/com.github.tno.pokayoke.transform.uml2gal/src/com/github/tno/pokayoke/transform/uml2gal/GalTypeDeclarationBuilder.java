////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.uml2gal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.GALTypeDeclaration;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.Transition;
import fr.lip6.move.gal.Variable;

/** Builder to conveniently construct {@link GALTypeDeclaration GAL type declarations}. */
public class GalTypeDeclarationBuilder {
    private final GALTypeDeclaration typeDecl = Uml2GalTranslationHelper.FACTORY.createGALTypeDeclaration();

    private final Map<String, Variable> variableMapping = new LinkedHashMap<>();

    public Transition addTransition(Transition transition) {
        Preconditions.checkArgument(!typeDecl.getTransitions().contains(transition),
                "Transition already declared: " + transition);
        typeDecl.getTransitions().add(transition);
        return transition;
    }

    public Variable addVariable(String name, IntExpression initialValue) {
        Uml2GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        Preconditions.checkArgument(!variableMapping.containsKey(name), "Duplicate variable name: " + name);
        Variable variable = Uml2GalTranslationHelper.FACTORY.createVariable();
        variable.setName(name);
        variable.setValue(initialValue);
        typeDecl.getVariables().add(variable);
        variableMapping.put(name, variable);
        return variable;
    }

    public int getTransitionCount() {
        return typeDecl.getTransitions().size();
    }

    public Variable getVariable(String name) {
        return variableMapping.get(name);
    }

    public void setName(String name) {
        typeDecl.setName(name);
    }

    public GALTypeDeclaration build() {
        return typeDecl;
    }
}
