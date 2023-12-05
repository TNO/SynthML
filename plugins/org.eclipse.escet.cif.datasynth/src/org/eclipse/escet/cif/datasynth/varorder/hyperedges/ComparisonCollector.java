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

package org.eclipse.escet.cif.datasynth.varorder.hyperedges;

import static org.eclipse.escet.common.java.Lists.list;

import java.util.List;

import org.eclipse.escet.cif.common.CifEquationUtils;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.AlgVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifWalker;

/** Comparison binary expression collector. */
class ComparisonCollector extends CifWalker {
    /** The comparison binary expressions collected so far. */
    private List<BinaryExpression> comparisons;

    /**
     * Collect and return the comparison binary expressions in the given expression.
     *
     * @param expr The expression in which to collect, recursively.
     * @return The collected comparison binary expressions.
     */
    List<BinaryExpression> collectComparisons(Expression expr) {
        // Initialization.
        comparisons = list();

        // Collect.
        walkExpression(expr);

        // Cleanup and return the collected comparisons.
        List<BinaryExpression> rslt = comparisons;
        comparisons = null;
        return rslt;
    }

    @Override
    @SuppressWarnings("incomplete-switch")
    public void preprocessBinaryExpression(BinaryExpression expr) {
        switch (expr.getOperator()) {
            case EQUAL:
            case UNEQUAL:
            case LESS_EQUAL:
            case LESS_THAN:
            case GREATER_EQUAL:
            case GREATER_THAN:
                comparisons.add(expr);
                break;
        }
    }

    @Override
    protected void preprocessAlgVariableExpression(AlgVariableExpression expr) {
        // Get the possible values of the variable.
        AlgVariable var = expr.getVariable();
        List<Expression> values = CifEquationUtils.getValuesForAlgVar(var, false);

        // Collect for each possible value.
        for (Expression value: values) {
            walkExpression(value);
        }
    }
}
