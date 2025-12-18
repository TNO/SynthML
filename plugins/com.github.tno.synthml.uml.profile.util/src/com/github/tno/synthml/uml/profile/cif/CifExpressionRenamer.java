////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.cif;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AElifExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AName;

/** Renames {@link ANameExpression name expressions} using a specified renaming function. */
public class CifExpressionRenamer extends ACifObjectVisitor<ACifObject, Map<String, String>> {
    /** The renaming function. */
    private final Function<String, String> renamer;

    /**
     * Constructs a new {@link CifExpressionRenamer}.
     *
     * @param renamer The renaming function.
     */
    public CifExpressionRenamer(Function<String, String> renamer) {
        this.renamer = renamer;
    }

    /**
     * Renames all name expressions in the given CIF parser AST object.
     *
     * @param object The input CIF parser AST object.
     * @param renaming The mapping from old to new names, which is maintained while renaming and is modified in-place.
     * @return The updated CIF parser AST object.
     */
    public ACifObject rename(ACifObject object, Map<String, String> renaming) {
        return visit(object, renaming);
    }

    @Override
    protected ACifObject visit(AAssignmentUpdate update, Map<String, String> renaming) {
        AExpression addressable = (AExpression)visit(update.addressable, renaming);
        AExpression value = (AExpression)visit(update.value, renaming);
        return new AAssignmentUpdate(addressable, value, update.position);
    }

    @Override
    protected ACifObject visit(AIfUpdate update, Map<String, String> renaming) {
        List<AExpression> guards = update.guards.stream().map(guard -> (AExpression)visit(guard, renaming)).toList();
        List<AUpdate> thens = update.thens.stream().map(then -> (AUpdate)visit(then, renaming)).toList();
        List<AElifUpdate> elifs = update.elifs.stream().map(elif -> visit(elif, renaming)).toList();
        List<AUpdate> elses = update.elses.stream().map(elze -> (AUpdate)visit(elze, renaming)).toList();
        return new AIfUpdate(guards, thens, elifs, elses, update.position);
    }

    protected AElifUpdate visit(AElifUpdate update, Map<String, String> renaming) {
        List<AExpression> guards = update.guards.stream().map(guard -> (AExpression)visit(guard, renaming)).toList();
        List<AUpdate> thens = update.thens.stream().map(then -> (AUpdate)visit(then, renaming)).toList();
        return new AElifUpdate(guards, thens, update.position);
    }

    @Override
    protected ACifObject visit(ABinaryExpression expr, Map<String, String> renaming) {
        AExpression left = (AExpression)visit(expr.left, renaming);
        AExpression right = (AExpression)visit(expr.right, renaming);
        return new ABinaryExpression(expr.operator, left, right, expr.position);
    }

    @Override
    protected ACifObject visit(ABoolExpression expr, Map<String, String> renaming) {
        return expr;
    }

    @Override
    protected ACifObject visit(AIntExpression expr, Map<String, String> renaming) {
        return expr;
    }

    @Override
    protected ACifObject visit(ANameExpression expr, Map<String, String> renaming) {
        String newName = renaming.computeIfAbsent(expr.name.name, renamer);
        return new ANameExpression(new AName(newName, expr.name.position), expr.derivative, expr.position);
    }

    @Override
    protected ACifObject visit(AUnaryExpression expr, Map<String, String> renaming) {
        AExpression child = (AExpression)visit(expr.child, renaming);
        return new AUnaryExpression(expr.operator, child, expr.position);
    }

    @Override
    protected ACifObject visit(AInvariant invariant, Map<String, String> renaming) {
        AExpression predicate = (AExpression)visit(invariant.predicate, renaming);
        return new AInvariant(invariant.name, predicate, invariant.invKind, invariant.events);
    }

    @Override
    protected ACifObject visit(AIfExpression expr, Map<String, String> renaming) {
        List<AExpression> guards = expr.guards.stream().map(guard -> (AExpression)visit(guard, renaming)).toList();
        AExpression then = (AExpression)visit(expr.then, renaming);
        List<AElifExpression> elifs = expr.elifs.stream().map(elif -> (AElifExpression)visit(elif, renaming)).toList();
        AExpression elseExpr = (AExpression)visit(expr.elseExpr, renaming);

        return new AIfExpression(guards, then, elifs, elseExpr, expr.position);
    }

    @Override
    protected ACifObject visit(AElifExpression expr, Map<String, String> renaming) {
        List<AExpression> guards = expr.guards.stream().map(guard -> (AExpression)visit(guard, renaming)).toList();
        AExpression then = (AExpression)visit(expr.then, renaming);
        return new AElifExpression(guards, then, expr.position);
    }
}
