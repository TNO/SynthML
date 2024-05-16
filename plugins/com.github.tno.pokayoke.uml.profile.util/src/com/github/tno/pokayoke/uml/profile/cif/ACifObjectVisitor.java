
package com.github.tno.pokayoke.uml.profile.cif;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;

public abstract class ACifObjectVisitor<T, C> {
    protected T visit(ACifObject object, C ctx) {
        if (object instanceof AExpression expr) {
            return visit(expr, ctx);
        } else if (object instanceof AInvariant invariant) {
            return visit(invariant, ctx);
        } else if (object instanceof AUpdate update) {
            return visit(update, ctx);
        } else {
            throw new RuntimeException("Unsupported object: " + object.getClass().getSimpleName());
        }
    }

    protected T visit(AUpdate update, C ctx) {
        if (update instanceof AAssignmentUpdate assignment) {
            return visit(assignment, ctx);
        } else {
            throw new RuntimeException("Unsupported update: " + update.getClass().getSimpleName());
        }
    }

    protected abstract T visit(AAssignmentUpdate update, C ctx);

    protected T visit(AExpression expr, C ctx) {
        if (expr instanceof ABinaryExpression binExpr) {
            return visit(binExpr, ctx);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return visit(boolExpr, ctx);
        } else if (expr instanceof AIntExpression intExpr) {
            return visit(intExpr, ctx);
        } else if (expr instanceof ANameExpression nameExpr) {
            return visit(nameExpr, ctx);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return visit(unaryExpr, ctx);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr.getClass().getSimpleName());
        }
    }

    protected abstract T visit(ABinaryExpression expr, C ctx);

    protected abstract T visit(ABoolExpression expr, C ctx);

    protected abstract T visit(AIntExpression expr, C ctx);

    protected abstract T visit(ANameExpression expr, C ctx);

    protected abstract T visit(AUnaryExpression expr, C ctx);

    protected abstract T visit(AInvariant invariant, C ctx);
}
