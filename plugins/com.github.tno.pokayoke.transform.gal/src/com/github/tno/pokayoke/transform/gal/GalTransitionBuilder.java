
package com.github.tno.pokayoke.transform.gal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.AssignType;
import fr.lip6.move.gal.Assignment;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.Comparison;
import fr.lip6.move.gal.ComparisonOperators;
import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.ParamRef;
import fr.lip6.move.gal.Parameter;
import fr.lip6.move.gal.Statement;
import fr.lip6.move.gal.Transition;
import fr.lip6.move.gal.TypedefDeclaration;
import fr.lip6.move.gal.Variable;
import fr.lip6.move.gal.VariableReference;

/** Builder to conveniently construct {@link Transition GAL transitions}. */
public class GalTransitionBuilder {
    private final Transition transition = GalTranslationHelper.FACTORY.createTransition();

    private final List<BooleanExpression> guards = new ArrayList<>();

    private final Map<String, Parameter> paramMapping = new LinkedHashMap<>();

    public Statement addAssignment(Variable variable, Parameter value) {
        Preconditions.checkNotNull(value, "Expected a non-null parameter.");
        ParamRef reference = GalTranslationHelper.FACTORY.createParamRef();
        reference.setRefParam(value);
        return addAssignment(variable, reference);
    }

    public Statement addAssignment(Variable variable, int value) {
        Constant constant = GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(value);
        return addAssignment(variable, constant);
    }

    public Statement addAssignment(Variable variable, IntExpression value) {
        Preconditions.checkNotNull(variable, "Expected a non-null variable.");
        VariableReference reference = GalTranslationHelper.FACTORY.createVariableReference();
        reference.setRef(variable);
        Preconditions.checkNotNull(value, "Expected a non-null expression.");
        Assignment assignment = GalTranslationHelper.FACTORY.createAssignment();
        assignment.setLeft(reference);
        assignment.setRight(value);
        assignment.setType(AssignType.ASSIGN);
        return addAction(assignment);
    }

    public Statement addAction(Statement statement) {
        Preconditions.checkNotNull(statement, "Expected a non-null statement.");
        Preconditions.checkArgument(!transition.getActions().contains(statement),
                "Action already declared: " + statement);
        transition.getActions().add(statement);
        return statement;
    }

    public void addActions(Collection<? extends Statement> statements) {
        Preconditions.checkNotNull(statements, "Expected a non-null collection of statements.");
        statements.forEach(this::addAction);
    }

    public BooleanExpression addEqualityGuard(Variable left, int right) {
        Constant constant = GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(right);
        return addEqualityGuard(left, constant);
    }

    public BooleanExpression addEqualityGuard(Variable left, IntExpression right) {
        Preconditions.checkNotNull(left, "Expected a non-null variable.");
        VariableReference reference = GalTranslationHelper.FACTORY.createVariableReference();
        reference.setRef(left);
        return addEqualityGuard(reference, right);
    }

    public BooleanExpression addEqualityGuard(IntExpression left, IntExpression right) {
        Preconditions.checkNotNull(left, "Expected a non-null left expression.");
        Preconditions.checkNotNull(right, "Expected a non-null right expression.");
        Comparison comparison = GalTranslationHelper.FACTORY.createComparison();
        comparison.setOperator(ComparisonOperators.EQ);
        comparison.setLeft(left);
        comparison.setRight(right);
        return addGuard(comparison);
    }

    public BooleanExpression addGuard(BooleanExpression guard) {
        Preconditions.checkNotNull(guard, "Expected a non-null guard.");
        guards.add(guard);
        return guard;
    }

    public void addGuards(Collection<? extends BooleanExpression> guards) {
        Preconditions.checkNotNull(guards, "Expected a non-null collection of guards.");
        guards.forEach(this::addGuard);
    }

    public Parameter addParam(String name, TypedefDeclaration paramType) {
        Preconditions.checkNotNull(name, "Expected a non-null parameter name.");
        Preconditions.checkNotNull(paramType, "Expected a non-null parameter type.");
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        Parameter param = GalTranslationHelper.FACTORY.createParameter();
        param.setName("$" + name);
        param.setType(paramType);
        return addParam(param);
    }

    public Parameter addParam(Parameter param) {
        Preconditions.checkNotNull(param, "Expected a non-null parameter.");
        Preconditions.checkArgument(!transition.getParams().contains(param), "Parameter already declared: " + param);
        String name = param.getName();
        Preconditions.checkArgument(!paramMapping.containsKey(name), "Duplicate parameter name: " + name);
        transition.getParams().add(param);
        paramMapping.put(name, param);
        return param;
    }

    public Parameter getParam(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null parameter name.");
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        return paramMapping.get("$" + name);
    }

    public void setName(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null transition name.");
        transition.setName(name);
    }

    public Transition build() {
        transition.setGuard(GalTranslationHelper.combineAsAnd(guards));
        return transition;
    }
}
