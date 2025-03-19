
package com.github.tno.pokayoke.uml.profile.cif;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AElifExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

public abstract class ACifObjectWalker<T> extends ACifObjectVisitor<T, CifContext> {
    protected enum BinaryOperator {
        AND("and"), OR("or"), EQ("="), NE("!="), GE(">="), GT(">"), LE("<="), LT("<"), PLUS("+"), MINUS("-");

        private final String cifOperator;

        private BinaryOperator(String cifOperator) {
            this.cifOperator = cifOperator;
        }

        public String cifValue() {
            return cifOperator;
        }

        private static BinaryOperator valueOfCif(String operator) {
            return from(values()).any(v -> v.cifOperator.equals(operator));
        }
    }

    protected enum UnaryOperator {
        NOT("not"), MINUS("-");

        private final String cifOperator;

        private UnaryOperator(String cifOperator) {
            this.cifOperator = cifOperator;
        }

        public String cifValue() {
            return cifOperator;
        }

        private static UnaryOperator valueOfCif(String operator) {
            return from(values()).any(v -> v.cifOperator.equals(operator));
        }
    }

    @Override
    protected T visit(AAssignmentUpdate update, CifContext ctx) {
        if (update.addressable instanceof ANameExpression addressable) {
            TextPosition assignmentPos = update.value.position;
            String name = addressable.name.name;
            if (!ctx.isVariable(name)) {
                throw new CustomSyntaxException(String.format("unresolved variable '%s'", name), assignmentPos);
            }
            return visit(visit(addressable, ctx), assignmentPos, visit(update.value, ctx), ctx);
        }
        throw new CustomSyntaxException("expected a variable reference", update.position);
    }

    protected abstract T visit(T addressable, TextPosition assignmentPos, T value, CifContext ctx);

    @Override
    protected T visit(AIfUpdate update, CifContext ctx) {
        List<T> guards = update.guards.stream().map(grd -> visit(grd, ctx)).toList();
        List<T> thens = update.thens.stream().map(upd -> visit(upd, ctx)).toList();
        List<T> elifs = update.elifs.stream().map(upd -> visit(upd, ctx)).toList();
        List<T> elses = update.elses.stream().map(upd -> visit(upd, ctx)).toList();

        if (thens.isEmpty()) {
            throw new CustomSyntaxException("Expected a non-empty 'then'. ", update.position);
        }

        return visit(guards, thens, elifs, elses, update.position, ctx);
    }

    protected abstract T visit(List<T> guards, List<T> thens, List<T> elifs, List<T> elses, TextPosition updatePos,
            CifContext ctx);

    protected T visit(AElifUpdate update, CifContext ctx) {
        List<T> guards = update.guards.stream().map(grd -> visit(grd, ctx)).toList();
        List<T> thens = update.thens.stream().map(upd -> visit(upd, ctx)).toList();

        if (thens.isEmpty()) {
            throw new CustomSyntaxException("Expected a non-empty 'then'. ", update.position);
        }

        return visit(guards, thens, update.position, ctx);
    }

    protected abstract T visit(List<T> guards, List<T> thens, TextPosition updatePos, CifContext ctx);

    @Override
    protected T visit(ABinaryExpression expr, CifContext ctx) {
        TextPosition operatorPos = expr.right.position;
        BinaryOperator operator = BinaryOperator.valueOfCif(expr.operator);
        if (operator == null) {
            throw new CustomSyntaxException("unsupported operator: " + operator, operatorPos);
        }
        return visit(operator, operatorPos, visit(expr.left, ctx), visit(expr.right, ctx), ctx);
    }

    protected abstract T visit(BinaryOperator operator, TextPosition operatorPos, T left, T right, CifContext ctx);

    @Override
    protected T visit(ANameExpression expr, CifContext ctx) {
        String name = expr.name.name;
        if (expr.derivative) {
            throw new CustomSyntaxException("expected a non-derivative name", expr.position);
        }

        NamedElement element = ctx.getReferenceableElement(name);
        if (element instanceof EnumerationLiteral literal) {
            return visit(literal, expr.position, ctx);
        } else if (element instanceof Property property) {
            return visit(property, expr.position, ctx);
        } else {
            throw new CustomSyntaxException(String.format("unresolved name '%s'", name), expr.position);
        }
    }

    protected abstract T visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx);

    protected abstract T visit(Property property, TextPosition propertyPos, CifContext ctx);

    @Override
    protected T visit(AUnaryExpression expr, CifContext ctx) {
        TextPosition operatorPos = expr.position;
        UnaryOperator operator = UnaryOperator.valueOfCif(expr.operator);
        if (operator == null) {
            throw new CustomSyntaxException("unsupported operator: " + operator, operatorPos);
        }
        return visit(operator, operatorPos, visit(expr.child, ctx), ctx);
    }

    protected abstract T visit(UnaryOperator operator, TextPosition operatorPos, T child, CifContext ctx);

    @Override
    protected T visit(AInvariant invariant, CifContext ctx) {
        Optional<String> invKind = Optional.ofNullable(invariant.invKind).map(kind -> kind.text);

        List<String> events = new ArrayList<>();
        if (invariant.events != null) {
            invariant.events.stream().map(event -> event.name).collect(Collectors.toCollection(() -> events));
        }

        return visit(invKind, events, invariant.predicate.position, visit(invariant.predicate, ctx), ctx);
    }

    protected abstract T visit(Optional<String> invKind, List<String> events, TextPosition invariantPos, T predicate,
            CifContext ctx);

    @Override
    protected T visit(AIfExpression expression, CifContext ctx) {
        List<T> guards = expression.guards.stream().map(grd -> visit(grd, ctx)).toList();
        T thenExpr = visit(expression.then, ctx);
        List<T> elifs = expression.elifs.stream().map(upd -> visit(upd, ctx)).toList();
        T elseExpr = visit(expression.elseExpr, ctx);;

        return visit(guards, thenExpr, elifs, elseExpr, expression.position, ctx);
    }

    protected abstract T visit(List<T> guards, T thenExpr, List<T> elifs, T elseExpr, TextPosition updatePos,
            CifContext ctx);

    protected T visit(AElifExpression expression, CifContext ctx) {
        List<T> guards = expression.guards.stream().map(grd -> visit(grd, ctx)).toList();
        T thenExpr = visit(expression.then, ctx);

        return visit(guards, thenExpr, expression.position, ctx);
    }

    protected abstract T visit(List<T> guards, T thenExpr, TextPosition updatePos, CifContext ctx);
}
