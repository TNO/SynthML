//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.conversion;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocationExpression;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.datasynth.conversion.CifToSynthesisConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

import com.github.javabdd.BDD;
import com.google.common.base.Verify;

/** Helper class for creating CIF expressions. */
public class CIFExpressionHelper {
    private CIFExpressionHelper() {
    }

    public static BDD getDisjunctionBDDOfStates(String[] inputStates, SynthesisAutomaton synthesisAutomaton)
            throws UnsupportedPredicateException
    {
        List<BDD> bddLsit = new ArrayList<>();

        for (String inputState: inputStates) {
            String loc = inputState.replace("@state(", "").replace(")", "");
            BinaryExpression expression4Location = getBinaryExpression4Location(synthesisAutomaton, loc);
            BDD bdd4Loc = CifToSynthesisConverter.convertPred(expression4Location, false, synthesisAutomaton);
            bddLsit.add(bdd4Loc);
        }

        BDD disjunction = bddLsit.get(0);
        for (int i = 1; i < bddLsit.size(); i++) {
            disjunction = disjunction.or(bddLsit.get(i));
        }

        return disjunction;
    }

    public static BinaryExpression getBinaryExpression4Location(SynthesisAutomaton synthesisAutomaton,
            String stateAnnotation)
    {
        String[] stateAnnotations = stateAnnotation.split(",");
        Map<String, String> variables2Values = getVariable2ValueMap(stateAnnotations);

        Map<String, Expression> variables2Expressions = getVariable2ExpressionMap(synthesisAutomaton, variables2Values);
        BinaryExpression expression = (BinaryExpression)CifValueUtils
                .createConjunction(getStateExpressions(variables2Expressions, variables2Values), true);

        return expression;
    }

    public static Map<String, String> getVariable2ValueMap(String[] stateAnnotations) {
        Map<String, String> variables2Values = new HashMap<>();

        for (String stateAnnotation: stateAnnotations) {
            String variableName = stateAnnotation.split("=")[0].trim();
            String variableValue = stateAnnotation.split("=")[1].trim();
            variables2Values.put(variableName, variableValue.replace("'", ""));
        }

        return variables2Values;
    }

    public static Map<String, Expression> getVariable2ExpressionMap(SynthesisAutomaton synthesisAutomaton,
            Map<String, String> variables2Values)
    {
        SynthesisVariable[] variables = synthesisAutomaton.variables;

        Map<String, Expression> expressionMap = new HashMap<>();
        for (SynthesisVariable variable: variables) {
            PositionObject obj = variable.cifObject;
            String variableName = CifTextUtils.getAbsName(obj);

            if (obj instanceof Automaton) {
                SynthesisLocPtrVariable locationVariable = (SynthesisLocPtrVariable)variable;
                LocationExpression locationExpression = newLocationExpression();
                locationExpression.setType(newBoolType());
                String locationName = variables2Values.get(variableName);
                Location loc = locationVariable.aut.getLocations().stream()
                        .filter(location -> location.getName().equals(locationName)).toList().get(0);
                locationExpression.setLocation(loc);
                expressionMap.put(variableName, locationExpression);
            } else if (obj instanceof DiscVariable) {
                SynthesisDiscVariable locationVariable = (SynthesisDiscVariable)variable;
                DiscVariableExpression variableExpression = newDiscVariableExpression();
                variableExpression.setType(deepclone(locationVariable.var.getType()));
                variableExpression.setVariable(locationVariable.var);
                expressionMap.put(variableName, variableExpression);
            }
        }

        return expressionMap;
    }

    public static List<Expression> getStateExpressions(Map<String, Expression> variables2Expressions,
            Map<String, String> variables2Values)
    {
        List<Expression> expressions = new ArrayList<Expression>();
        for (Map.Entry<String, String> variable: variables2Values.entrySet()) {
            String variableName = variable.getKey();
            String variableValue = variable.getValue();

            if (!variableName.equals("sup") && !variableName.equals("Spec")) {
                Expression variableExpression = variables2Expressions.get(variableName);

                if (variableExpression instanceof LocationExpression) {
                    expressions.add(variableExpression);
                } else if (variableExpression instanceof DiscVariableExpression discVariableExpression) {
                    BinaryExpression binaryExpression = newBinaryExpression();
                    binaryExpression.setType(newBoolType());
                    binaryExpression.setLeft(deepclone(variableExpression));
                    binaryExpression.setOperator(BinaryOperator.EQUAL);

                    DiscVariable discVariable = discVariableExpression.getVariable();
                    CifType variableType = discVariable.getType();

                    if (variableType instanceof EnumType enumType) {
                        EList<EnumLiteral> enumLiterals = enumType.getEnum().getLiterals();
                        EnumLiteral enumliteral = enumLiterals.stream()
                                .filter(literal -> literal.getName().equals(variableValue)).toList().get(0);

                        binaryExpression.setRight(newEnumLiteralExpression(enumliteral, null, deepclone(variableType)));
                    } else if (variableType instanceof BoolType) {
                        if (variableValue.equals("true")) {
                            binaryExpression.setRight(CifValueUtils.makeTrue());
                        } else {
                            binaryExpression.setRight(CifValueUtils.makeFalse());
                        }
                    }
                    expressions.add(binaryExpression);
                }
            }
        }

        return expressions;
    }

    public static List<Expression> getActionGuard(String action, List<Edge> edges) {
        List<Edge> edgeList = new ArrayList<>();
        for (Edge edge: edges) {
            for (EdgeEvent event: edge.getEvents()) {
                EventExpression e = (EventExpression)event.getEvent();
                String name = e.getEvent().getName();
                if (name.equals(action)) {
                    edgeList.add(edge);
                }
            }
        }
        Verify.verify(edgeList.size() == 1, String.format("Expected exactly one edge for action %s", action));

        return deepclone(edgeList.get(0).getGuards());
    }
}
