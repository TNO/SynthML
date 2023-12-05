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

import java.util.Set;

import org.eclipse.escet.cif.common.CifEquationUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.AlgVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.InputVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.java.CifWalker;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

/** CIF variable object collector. */
class VariableCollector extends CifWalker {
    /** The CIF variable objects collected so far. */
    private Set<PositionObject> cifVarObjs;

    /**
     * Collect CIF variable objects in the given update, recursively.
     *
     * @param update The update.
     * @param cifVarObjs The CIF variable objects collected so far. Is extended in-place.
     */
    void collectCifVarObjs(Update update, Set<PositionObject> cifVarObjs) {
        this.cifVarObjs = cifVarObjs;
        walkUpdate(update);
        this.cifVarObjs = null;
    }

    /**
     * Collect CIF variable objects in the given expression, recursively.
     *
     * @param expr The expression.
     * @param cifVarObjs The CIF variable objects collected so far. Is extended in-place.
     */
    void collectCifVarObjs(Expression expr, Set<PositionObject> cifVarObjs) {
        this.cifVarObjs = cifVarObjs;
        walkExpression(expr);
        this.cifVarObjs = null;
    }

    @Override
    protected void preprocessDiscVariableExpression(DiscVariableExpression expr) {
        cifVarObjs.add(expr.getVariable());
    }

    @Override
    protected void preprocessInputVariableExpression(InputVariableExpression expr) {
        cifVarObjs.add(expr.getVariable());
    }

    @Override
    protected void preprocessLocationExpression(LocationExpression expr) {
        // Only add automaton if location pointer variable will be created for it.
        Location loc = expr.getLocation();
        Automaton aut = CifLocationUtils.getAutomaton(loc);
        if (aut.getLocations().size() > 1) {
            cifVarObjs.add(aut);
        }
    }

    @Override
    protected void preprocessAlgVariableExpression(AlgVariableExpression expr) {
        // Obtain single value expression, to get 'if' expression over locations, if equations per location are
        // used. That way, the location pointer variable (for the automaton) is also collected.
        AlgVariable var = expr.getVariable();
        Expression value = CifEquationUtils.getSingleValueForAlgVar(var);

        // Collect in the value expression.
        walkExpression(value);
    }
}
