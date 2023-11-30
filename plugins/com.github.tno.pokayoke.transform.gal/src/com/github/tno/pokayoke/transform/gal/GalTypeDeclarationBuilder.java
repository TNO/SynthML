
package com.github.tno.pokayoke.transform.gal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.GALTypeDeclaration;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.Transition;
import fr.lip6.move.gal.Variable;

/** Builder to conveniently construct {@link GALTypeDeclaration GAL type declarations}. */
public class GalTypeDeclarationBuilder {
    private final GALTypeDeclaration typeDecl = GalTranslationHelper.FACTORY.createGALTypeDeclaration();

    private final Map<String, Variable> variableMapping = new LinkedHashMap<>();

    public Transition addTransition(Transition transition) {
        Preconditions.checkNotNull(transition, "Expected a non-null transition.");
        Preconditions.checkArgument(!typeDecl.getTransitions().contains(transition),
                "Transition already declared: " + transition);
        typeDecl.getTransitions().add(transition);
        return transition;
    }

    public Variable addVariable(String name, int defaultValue) {
        Constant constValue = GalTranslationHelper.FACTORY.createConstant();
        constValue.setValue(defaultValue);
        return addVariable(name, constValue);
    }

    public Variable addVariable(String name, IntExpression defaultValue) {
        Preconditions.checkNotNull(name, "Expected a non-null variable name.");
        Preconditions.checkNotNull(defaultValue, "Expected a non-null default value.");
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        Preconditions.checkArgument(!variableMapping.containsKey(name), "Duplicate variable name: " + name);
        Variable variable = GalTranslationHelper.FACTORY.createVariable();
        variable.setName(name);
        variable.setValue(defaultValue);
        typeDecl.getVariables().add(variable);
        variableMapping.put(name, variable);
        return variable;
    }

    public int getTransitionCount() {
        return typeDecl.getTransitions().size();
    }

    public Variable getVariable(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null variable name.");
        return variableMapping.get(name);
    }

    public void setName(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null type name.");
        typeDecl.setName(name);
    }

    public GALTypeDeclaration build() {
        return typeDecl;
    }
}
