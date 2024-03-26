package com.github.tno.pokayoke.uml.profile.cif;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
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

		NamedElement element = ctx.getElement(name);
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
}
