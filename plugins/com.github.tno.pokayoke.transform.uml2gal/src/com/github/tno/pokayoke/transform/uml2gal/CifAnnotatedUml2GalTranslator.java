
package com.github.tno.pokayoke.transform.uml2gal;

import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.And;
import fr.lip6.move.gal.AssignType;
import fr.lip6.move.gal.Assignment;
import fr.lip6.move.gal.BinaryIntExpression;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.Comparison;
import fr.lip6.move.gal.ComparisonOperators;
import fr.lip6.move.gal.ConstParameter;
import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.Not;
import fr.lip6.move.gal.Or;
import fr.lip6.move.gal.ParamRef;
import fr.lip6.move.gal.UnaryMinus;
import fr.lip6.move.gal.Variable;
import fr.lip6.move.gal.VariableReference;

/** Translates annotated UML models to GAL specifications where CIF is the annotation language. */
public class CifAnnotatedUml2GalTranslator extends Uml2GalTranslator {
    private final String inputPath;

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final CifUpdateParser updateParser = new CifUpdateParser();

    public CifAnnotatedUml2GalTranslator(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    protected BooleanExpression translateBoolExpr(String expr) {
        return translateExpressionToBoolean(expressionParser.parseString(expr, inputPath));
    }

    @Override
    protected IntExpression translateIntExpr(String expr) {
        return translateExpressionToInt(expressionParser.parseString(expr, inputPath));
    }

    @Override
    protected Assignment translateAssignment(String update) {
        return translateUpdate(updateParser.parseString(update, inputPath));
    }

    private BooleanExpression translateExpressionToBoolean(AExpression expr) {
        if (expr instanceof ABinaryExpression binExpr) {
            return translateBinaryExpressionToBoolean(binExpr);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return translateBoolExpressionToBoolean(boolExpr);
        } else if (expr instanceof ANameExpression nameExpr) {
            return translateNameExpressionToBoolean(nameExpr);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return translateUnaryExpressionToBoolean(unaryExpr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    private BooleanExpression translateBinaryExpressionToBoolean(ABinaryExpression expr) {
        // Try translating the expression as a logical connective, e.g., conjunction or disjunction.
        switch (expr.operator) {
            case "and" -> {
                And and = Uml2GalTranslationHelper.FACTORY.createAnd();
                and.setLeft(translateExpressionToBoolean(expr.left));
                and.setRight(translateExpressionToBoolean(expr.right));
                return and;
            }
            case "or" -> {
                Or or = Uml2GalTranslationHelper.FACTORY.createOr();
                or.setLeft(translateExpressionToBoolean(expr.left));
                or.setRight(translateExpressionToBoolean(expr.right));
                return or;
            }
        }

        // Try translating the expression as a comparison of integers, e.g., less than or equal to.
        Comparison comparison = Uml2GalTranslationHelper.FACTORY.createComparison();
        comparison.setLeft(translateExpressionToInt(expr.left));
        comparison.setRight(translateExpressionToInt(expr.right));
        comparison.setOperator(translateComparisonOperator(expr.operator));
        return comparison;
    }

    private ComparisonOperators translateComparisonOperator(String operator) {
        return switch (operator) {
            case "=" -> ComparisonOperators.EQ;
            case "!=" -> ComparisonOperators.NE;
            case ">=" -> ComparisonOperators.GE;
            case ">" -> ComparisonOperators.GT;
            case "<=" -> ComparisonOperators.LE;
            case "<" -> ComparisonOperators.LT;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    private BooleanExpression translateBoolExpressionToBoolean(ABoolExpression expr) {
        return expr.value ? Uml2GalTranslationHelper.FACTORY.createTrue()
                : Uml2GalTranslationHelper.FACTORY.createFalse();
    }

    private BooleanExpression translateNameExpressionToBoolean(ANameExpression expr) {
        Comparison comparison = Uml2GalTranslationHelper.FACTORY.createComparison();
        comparison.setLeft(translateNameExpressionToInt(expr));
        Constant right = Uml2GalTranslationHelper.FACTORY.createConstant();
        right.setValue(1);
        comparison.setRight(right);
        comparison.setOperator(ComparisonOperators.EQ);
        return comparison;
    }

    private BooleanExpression translateUnaryExpressionToBoolean(AUnaryExpression expr) {
        switch (expr.operator) {
            case "not" -> {
                Not not = Uml2GalTranslationHelper.FACTORY.createNot();
                not.setValue(translateExpressionToBoolean(expr.child));
                return not;
            }
            default -> throw new RuntimeException("Unsupported unary expression: " + expr.operator);
        }
    }

    private IntExpression translateExpressionToInt(AExpression expr) {
        if (expr instanceof ABinaryExpression binExpr) {
            return translateBinaryExpressionToInt(binExpr);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return translateBoolExpressionToInt(boolExpr);
        } else if (expr instanceof AIntExpression intExpr) {
            return translateIntExpressionToInt(intExpr);
        } else if (expr instanceof ANameExpression nameExpr) {
            return translateNameExpressionToInt(nameExpr);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return translateUnaryExpressionToInt(unaryExpr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    private IntExpression translateBinaryExpressionToInt(ABinaryExpression expr) {
        IntExpression left = translateExpressionToInt(expr.left);
        IntExpression right = translateExpressionToInt(expr.right);

        switch (expr.operator) {
            case "+", "-" -> {
                BinaryIntExpression binExpr = Uml2GalTranslationHelper.FACTORY.createBinaryIntExpression();
                binExpr.setLeft(left);
                binExpr.setOp(expr.operator);
                binExpr.setRight(right);
                return binExpr;
            }
            default -> throw new RuntimeException("Unsupported binary expression: " + expr.operator);
        }
    }

    private IntExpression translateBoolExpressionToInt(ABoolExpression expr) {
        Constant constant = Uml2GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(expr.value ? BOOL_TRUE : BOOL_FALSE);
        return constant;
    }

    private IntExpression translateIntExpressionToInt(AIntExpression expr) {
        Constant constant = Uml2GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(Integer.parseInt(expr.value));
        return constant;
    }

    private IntExpression translateNameExpressionToInt(ANameExpression expr) {
        Preconditions.checkArgument(!expr.derivative, "Expected a non-derivative name expression.");

        String name = expr.name.name;

        // Try translating the expression as a variable name.
        Variable variable = typeBuilder.getVariable(name);
        if (variable != null) {
            VariableReference reference = Uml2GalTranslationHelper.FACTORY.createVariableReference();
            reference.setRef(variable);
            return reference;
        }

        // Try translating the expression as an enumeration literal, which got translated to specification parameters.
        ConstParameter param = specificationBuilder.getParam(name);
        if (param != null) {
            ParamRef reference = Uml2GalTranslationHelper.FACTORY.createParamRef();
            reference.setRefParam(param);
            return reference;
        } else {
            throw new RuntimeException("Unsupported name expression: " + name);
        }
    }

    private IntExpression translateUnaryExpressionToInt(AUnaryExpression expr) {
        switch (expr.operator) {
            case "-" -> {
                UnaryMinus minus = Uml2GalTranslationHelper.FACTORY.createUnaryMinus();
                minus.setValue(translateExpressionToInt(expr.child));
                return minus;
            }
            default -> throw new RuntimeException("Unsupported unary expression: " + expr.operator);
        }
    }

    private Assignment translateUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return translateAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    private Assignment translateAssignmentUpdate(AAssignmentUpdate update) {
        IntExpression left = translateExpressionToInt(update.addressable);

        if (left instanceof VariableReference reference) {
            Assignment assignment = Uml2GalTranslationHelper.FACTORY.createAssignment();
            assignment.setLeft(reference);
            assignment.setRight(translateExpressionToInt(update.value));
            assignment.setType(AssignType.ASSIGN);
            return assignment;
        } else {
            throw new RuntimeException("Expected a variable reference, but got: " + left);
        }
    }
}
