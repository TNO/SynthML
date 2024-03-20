package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Arrays;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;

public abstract class ACifObjectWalker<T> extends ACifObjectVisitor<T, CifContext> {
	public enum BinaryOperator {
		AND("and"), OR("or"), EQ("="), NE("!="), GE(">="), GT(">"), LE("<="), LT("<"), PLUS("+"), MINUS("-");

		private final String cifOperator;

		private BinaryOperator(String cifOperator) {
			this.cifOperator = cifOperator;
		}

		public String cifValue() {
			return cifOperator;
		}

		public static BinaryOperator valueOfCif(String cifOperator) {
			return Arrays.stream(values()).filter(v -> v.cifOperator.equals(cifOperator)).findAny()
					.orElseThrow(() -> new RuntimeException("Unsupported operator: " + cifOperator));
		}
	};

	public enum UnaryOperator {
		NOT("not"), MINUS("-");

		private final String cifOperator;

		private UnaryOperator(String cifOperator) {
			this.cifOperator = cifOperator;
		}

		public String cifValue() {
			return cifOperator;
		}

		public static UnaryOperator valueOfCif(String cifOperator) {
			return Arrays.stream(values()).filter(v -> v.cifOperator.equals(cifOperator)).findAny()
					.orElseThrow(() -> new RuntimeException("Unsupported operator: " + cifOperator));
		}
	};
	
	@Override
	protected T visit(AAssignmentUpdate update, CifContext ctx) {
		if (update.addressable instanceof ANameExpression addressable) {
			String name = addressable.name.name;
			Verify.verify(ctx.isVariable(name), "Unresolved variable '%s'", name);
			return visit(visit(addressable, ctx), visit(update.value, ctx), ctx);
		}
		throw new RuntimeException(
				"Expected a variable reference, but got: " + update.addressable.getClass().getSimpleName());
	}
	
	protected abstract T visit(T addressable, T value, CifContext ctx);

	@Override
	protected T visit(ABinaryExpression expr, CifContext ctx) {
		BinaryOperator operator = BinaryOperator.valueOfCif(expr.operator);
		return visit(operator, visit(expr.left, ctx), visit(expr.right, ctx), ctx);
	}

	protected abstract T visit(BinaryOperator operator, T left, T right, CifContext ctx);

	@Override
	protected T visit(ANameExpression expr, CifContext ctx) {
		String name = expr.name.name;
		Preconditions.checkArgument(!expr.derivative, "Expected a non-derivative name expression for '%s'.", name);

		if (ctx.isEnumerationLiteral(name)) {
			return visit(ctx.getEnumerationLiteral(name), ctx);
		} else if (ctx.isVariable(name)) {
			return visit(ctx.getVariable(name), ctx);
		} else {
			throw new RuntimeException(Strings.lenientFormat("Unresolved name '%s'", name));
		}
	}

	protected abstract T visit(EnumerationLiteral literal, CifContext ctx);

	protected abstract T visit(Property property, CifContext ctx);

	@Override
	protected T visit(AUnaryExpression expr, CifContext ctx) {
		UnaryOperator operator = UnaryOperator.valueOfCif(expr.operator);
		return visit(operator, visit(expr.child, ctx), ctx);
	}

	protected abstract T visit(UnaryOperator operator, T child, CifContext ctx);
}
