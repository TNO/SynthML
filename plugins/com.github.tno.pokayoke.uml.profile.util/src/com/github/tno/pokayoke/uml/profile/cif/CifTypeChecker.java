package com.github.tno.pokayoke.uml.profile.cif;

import static com.google.common.base.Strings.lenientFormat;

import java.util.Objects;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;

/**
 * Type checker for CIF annotated UML models, currently supporting:
 * <ul>
 * <li>Boolean</li>
 * <li>Integer</li>
 * <li>Enumeration</li>
 * </ul>
 */
public class CifTypeChecker extends ACifObjectWalker<Type> {
	/**
	 * Returns the type of the result when {@code expr} is evaluated against
	 * {@code elem} and checks if this type is boolean.
	 * 
	 * @param elem the context for evaluating the expression.
	 * @param expr the expression to evaluate.
	 * @return the type of the expression result.
	 * @throws RuntimeException if {@code expr} cannot be evaluated or if the result
	 *                          type is not a boolean.
	 */
	public static Type checkBooleanExpression(Element elem, AExpression expr) throws RuntimeException {
		CifTypeChecker typeChecker = new CifTypeChecker();
		CifContext ctx = new CifContext(elem);
		Type exprType = typeChecker.visit(expr, ctx);
		if (!ctx.getBooleanType().equals(exprType)) {
			throw new RuntimeException("Expected Boolean return type but got " + getLabel(exprType));
		}
		return exprType;
	}

	/**
	 * Returns the type of the result when {@code expr} is evaluated against
	 * {@code elem} and checks if this type is supported.
	 * 
	 * @param elem the context for evaluating the expression.
	 * @param expr the expression to evaluate.
	 * @return the type of the expression result.
	 * @throws RuntimeException if {@code expr} cannot be evaluated or if the result
	 *                          type is not supported.
	 */
	public static Type checkExpression(Element elem, AExpression expr) throws RuntimeException {
		CifTypeChecker typeChecker = new CifTypeChecker();
		CifContext ctx = new CifContext(elem);
		Type exprType = typeChecker.visit(expr, ctx);
		if (!isSupported(exprType, ctx)) {
			throw new RuntimeException("Unsupported type: " + getLabel(exprType));
		}
		return exprType;
	}

	/**
	 * Returns the type of the result when {@code upd} is evaluated against
	 * {@code elem} and checks if this type is supported.
	 * 
	 * @param elem the context for evaluating the update.
	 * @param upd  the update to evaluate.
	 * @return the type of the update addressable.
	 * @throws RuntimeException if {@code expr} cannot be evaluated, if the result
	 *                          type is not supported or if the update value type
	 *                          and update addressable type are not equal.
	 */
	public static Type checkUpdate(Element elem, AUpdate upd) throws RuntimeException {
		CifTypeChecker typeChecker = new CifTypeChecker();
		CifContext ctx = new CifContext(elem);
		Type updateType = typeChecker.visit(upd, ctx);
		if (!isSupported(updateType, ctx)) {
			throw new RuntimeException("Unsupported type: " + getLabel(updateType));
		}
		return updateType;
	}
	
	/**
	 * Returns the {@link TypedElement#getType() type} of the {@code elem} and checks if this type is supported.
	 * 
	 * @param elem the context and type provider.
	 * @return the type of the update addressable.
	 * @throws RuntimeException if the result type is not supported.
	 */
	public static Type checkSupportedType(TypedElement elem) throws RuntimeException {
		CifContext ctx = new CifContext(elem);
		Type elemType = elem.getType();
		if (!isSupported(elemType, ctx)) {
			throw new RuntimeException("Unsupported type: " + getLabel(elemType));
		}
		return elemType;
	}

	private CifTypeChecker() {
		// Empty for utility classes
	}

	@Override
	protected Type visit(ABoolExpression expr, CifContext ctx) {
		return ctx.getBooleanType();
	}

	@Override
	protected Type visit(AIntExpression expr, CifContext ctx) {
		return ctx.getIntegerType();
	}

	@Override
	protected Type visit(EnumerationLiteral literal, CifContext ctx) {
		return literal.getEnumeration();
	}

	@Override
	protected Type visit(Property property, CifContext ctx) {
		return property.getType();
	}

	@Override
	protected Type visit(Type addressable, Type value, CifContext ctx) {
		if (!Objects.equals(addressable, value)) {
			throw new RuntimeException(
					lenientFormat("Expected %s but got %s near keyword ':='", getLabel(addressable), getLabel(value)));
		}
		return addressable;
	}

	@Override
	protected Type visit(BinaryOperator operator, Type left, Type right, CifContext ctx) {
		String errorMsg = lenientFormat("The operator '%s' is undefined for the argument type(s) %s, %s",
				operator.cifValue(), getLabel(left), getLabel(right));
		if (!Objects.equals(left, right)) {
			throw new RuntimeException(errorMsg);
		}
		
		final Type compareType = left;
		return switch (operator) {
		case AND, OR -> {
			if (!ctx.getBooleanType().equals(compareType)) {
				throw new RuntimeException(errorMsg);
			}
			yield ctx.getBooleanType();
		}
		case PLUS, MINUS -> {
			if (!ctx.getIntegerType().equals(compareType)) {
				throw new RuntimeException(errorMsg);
			}
			yield compareType;
		}
		case GE, GT, LE, LT -> {
			if (!ctx.getIntegerType().equals(compareType)) {
				throw new RuntimeException(errorMsg);
			}
			yield ctx.getBooleanType();
		}
		// Equality supports all types
		case EQ, NE -> ctx.getBooleanType();
		};
	}

	@Override
	protected Type visit(UnaryOperator operator, Type child, CifContext ctx) {
		switch (operator) {
		case NOT:
			if (!ctx.getBooleanType().equals(child)) {
				throw new RuntimeException(lenientFormat("Expected Boolean but got %s near keyword '%s'",
						getLabel(child), operator.cifValue()));
			}
			break;
		case MINUS:
			if (!ctx.getIntegerType().equals(child)) {
				throw new RuntimeException(lenientFormat("Expected Integer but got %s near keyword '%s'",
						getLabel(child), operator.cifValue()));
			}
			break;
		}
		return child;
	}

	private static String getLabel(Type type) {
		return type == null ? "null" : type.getLabel(true);
	}

	private static boolean isSupported(Type type, CifContext ctx) {
		if (type == null) {
			return false;
		}
		return ctx.getBooleanType().equals(type) || ctx.getIntegerType().equals(type) || type instanceof Enumeration;
	}
}
