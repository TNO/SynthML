////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.uml2gal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.github.tno.synthml.uml.profile.cif.ACifObjectWalker;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;

import fr.lip6.move.gal.AssignType;
import fr.lip6.move.gal.Assignment;
import fr.lip6.move.gal.BinaryIntExpression;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.Comparison;
import fr.lip6.move.gal.ComparisonOperators;
import fr.lip6.move.gal.ConstParameter;
import fr.lip6.move.gal.GalFactory;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.Not;
import fr.lip6.move.gal.ParamRef;
import fr.lip6.move.gal.UnaryMinus;
import fr.lip6.move.gal.Variable;
import fr.lip6.move.gal.VariableReference;

public class CifToGalExpressionTranslator extends ACifObjectWalker<Object> {
    private final CifContext cifContext;

    private final GalSpecificationBuilder specificationBuilder;

    private final GalTypeDeclarationBuilder typeBuilder;

    public CifToGalExpressionTranslator(CifContext cifContext, GalSpecificationBuilder specificationBuilder,
            GalTypeDeclarationBuilder typeBuilder)
    {
        this.cifContext = cifContext;
        this.specificationBuilder = specificationBuilder;
        this.typeBuilder = typeBuilder;
    }

    public BooleanExpression translateBoolExpr(AExpression expr) {
        return expr == null ? null : toBool(visit(expr, cifContext));
    }

    public IntExpression translateIntExpr(AExpression expr) {
        return expr == null ? null : toInt(visit(expr, cifContext));
    }

    public List<Assignment> translateUpdates(List<AUpdate> updates) {
        return updates.stream().map(this::translateAssignment).collect(Collectors.toList());
    }

    private Assignment translateAssignment(AUpdate update) {
        Object result = visit(update, cifContext);
        if (result instanceof Assignment assignment) {
            return assignment;
        } else {
            throw new RuntimeException("Unexpected result type:" + result);
        }
    }

    @Override
    protected Object visit(Object addressable, TextPosition assignmentPos, Object value, CifContext ctx) {
        if (addressable instanceof VariableReference reference) {
            Assignment assignment = Uml2GalTranslationHelper.FACTORY.createAssignment();
            assignment.setLeft(reference);
            assignment.setRight(toInt(value));
            assignment.setType(AssignType.ASSIGN);
            return assignment;
        } else {
            throw new IllegalArgumentException("Unexpected adressable type:" + addressable);
        }
    }

    @Override
    protected Object visit(List<Object> guards, List<Object> thens, List<Object> elifs, List<Object> elses,
            TextPosition updatePos, CifContext ctx)
    {
        throw new UnsupportedOperationException("Conditional updates are unsupported.");
    }

    @Override
    protected Object visit(List<Object> guards, List<Object> thens, TextPosition updatePos, CifContext ctx) {
        throw new UnsupportedOperationException("Conditional updates are unsupported.");
    }

    @Override
    protected Object visit(List<Object> guards, Object thenExpr, List<Object> elifs, Object elseExpr,
            TextPosition expressionPos, CifContext ctx)
    {
        throw new UnsupportedOperationException("Conditional expressions are unsupported.");
    }

    @Override
    protected Object visit(List<Object> guards, Object thenExpr, TextPosition expressionPos, CifContext ctx) {
        throw new UnsupportedOperationException("Conditional expressions are unsupported.");
    }

    @Override
    protected Object visit(BinaryOperator operator, TextPosition operatorPos, Object left, Object right,
            CifContext ctx)
    {
        return switch (operator) {
            case AND -> {
                yield Uml2GalTranslationHelper.combineAsAnd(toBool(left), toBool(right));
            }
            case OR -> {
                yield Uml2GalTranslationHelper.combineAsOr(toBool(left), toBool(right));
            }
            case EQ -> {
                if (left instanceof BooleanExpression leftBool && right instanceof BooleanExpression rightBool) {
                    // Try to stick to booleans as long as possible
                    yield equiv(leftBool, rightBool);
                } else {
                    yield createComparison(operator, left, right);
                }
            }
            case NE -> {
                if (left instanceof BooleanExpression leftBool && right instanceof BooleanExpression rightBool) {
                    // Try to stick to booleans as long as possible
                    yield not(equiv(leftBool, rightBool));
                } else {
                    yield createComparison(operator, left, right);
                }
            }
            case LT, LE, GE, GT -> {
                yield createComparison(operator, left, right);
            }
            case PLUS, MINUS -> {
                BinaryIntExpression binExpr = Uml2GalTranslationHelper.FACTORY.createBinaryIntExpression();
                binExpr.setLeft(toInt(left));
                binExpr.setOp(operator.cifValue());
                binExpr.setRight(toInt(right));
                yield binExpr;
            }
        };
    }

    private Comparison createComparison(BinaryOperator operator, Object left, Object right) {
        ComparisonOperators comparisonOperator = switch (operator) {
            case EQ -> ComparisonOperators.EQ;
            case NE -> ComparisonOperators.NE;
            case GE -> ComparisonOperators.GE;
            case GT -> ComparisonOperators.GT;
            case LE -> ComparisonOperators.LE;
            case LT -> ComparisonOperators.LT;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };

        Comparison comparison = Uml2GalTranslationHelper.FACTORY.createComparison();
        comparison.setLeft(toInt(left));
        comparison.setRight(toInt(right));
        comparison.setOperator(comparisonOperator);
        return comparison;
    }

    @Override
    protected Object visit(UnaryOperator operator, TextPosition operatorPos, Object child, CifContext ctx) {
        return switch (operator) {
            case NOT -> {
                yield not(toBool(child));
            }
            case MINUS -> {
                UnaryMinus minus = Uml2GalTranslationHelper.FACTORY.createUnaryMinus();
                minus.setValue(toInt(child));
                yield minus;
            }
        };
    }

    @Override
    protected Object visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx) {
        ConstParameter param = specificationBuilder.getParam(literal.getName());

        ParamRef reference = Uml2GalTranslationHelper.FACTORY.createParamRef();
        reference.setRefParam(param);
        return reference;
    }

    @Override
    protected Object visit(Property property, TextPosition propertyPos, CifContext ctx) {
        Variable variable = typeBuilder.getVariable(property.getName());

        VariableReference reference = Uml2GalTranslationHelper.FACTORY.createVariableReference();
        reference.setRef(variable);
        return reference;
    }

    @Override
    protected Object visit(NamedTemplateParameter parameter, TextPosition parameterReferencePos, CifContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object visit(ABoolExpression expr, CifContext ctx) {
        return Uml2GalTranslationHelper.toBooleanExpression(expr.value);
    }

    @Override
    protected Object visit(AIntExpression expr, CifContext ctx) {
        return Uml2GalTranslationHelper.toIntExpression(Integer.parseInt(expr.value));
    }

    @Override
    protected Object visit(Optional<String> invKind, List<String> events, TextPosition invariantPos, Object predicate,
            CifContext ctx)
    {
        throw new UnsupportedOperationException();
    }

    private IntExpression toInt(Object expression) {
        if (expression instanceof IntExpression intExpression) {
            return intExpression;
        } else if (expression instanceof BooleanExpression booleanExpression) {
            return Uml2GalTranslationHelper.toIntExpression(booleanExpression);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + expression);
        }
    }

    private BooleanExpression toBool(Object expression) {
        if (expression instanceof BooleanExpression booleanExpression) {
            return booleanExpression;
        } else if (expression instanceof IntExpression intExpression) {
            return Uml2GalTranslationHelper.toBooleanExpression(intExpression);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + expression);
        }
    }

    /**
     * Alternative for using {@link GalFactory#createEquiv() boolean equivalence (i.e. <->)} in transition guards.
     * <p>
     * Boolean equivalence is translated into a <a href="https://en.wikipedia.org/wiki/XNOR_gate">logical XNOR</a>.
     * </p>
     *
     * @param left The left equivalence parameter.
     * @param right The right equivalence parameter.
     * @return The equivalence expression.
     * @see <a href="https://lip6.github.io/ITSTools-web/galbasics.html#boolean-expressions">Guarded Action Language
     *     Basics</a>
     */
    private BooleanExpression equiv(BooleanExpression left, BooleanExpression right) {
        BooleanExpression leftCopy = EcoreUtil.copy(left);
        BooleanExpression rightCopy = EcoreUtil.copy(right);

        return Uml2GalTranslationHelper.combineAsOr(Uml2GalTranslationHelper.combineAsAnd(left, right),
                Uml2GalTranslationHelper.combineAsAnd(not(leftCopy), not(rightCopy)));
    }

    private BooleanExpression not(BooleanExpression expression) {
        Not not = Uml2GalTranslationHelper.FACTORY.createNot();
        not.setValue(expression);
        return not;
    }
}
