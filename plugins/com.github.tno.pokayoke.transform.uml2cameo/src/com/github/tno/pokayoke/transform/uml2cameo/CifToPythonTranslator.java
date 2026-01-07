////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.uml2cameo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.github.tno.synthml.uml.profile.cif.ACifObjectWalker;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;

/** Translates basic CIF expressions and updates to Python. */
public class CifToPythonTranslator extends ACifObjectWalker<String> {
    public String translateExpression(AExpression expr, CifContext context) {
        if (expr == null) {
            return "True";
        }
        return visit(expr, context);
    }

    public List<String> translateUpdates(List<AUpdate> updates, CifContext context) {
        return updates.stream().map(update -> translateUpdate(update, context)).collect(Collectors.toList());
    }

    public String translateUpdate(AUpdate update, CifContext context) {
        return visit(update, context);
    }

    @Override
    protected String visit(String addressable, TextPosition assignmentPos, String value, CifContext ctx) {
        return String.format("%s = %s", addressable, value);
    }

    @Override
    protected String visit(List<String> guards, List<String> thens, List<String> elifs, List<String> elses,
            TextPosition updatePos, CifContext ctx)
    {
        String guard = conjoinExprs(guards);
        String then = mergeAll(increaseIndentation(thens), "\n").orElse("\tpass");
        String elif = mergeAll(elifs, "\n").map(e -> "\n" + e).orElse("");
        String elze = mergeAll(increaseIndentation(elses), "\n").orElse("\tpass");

        return String.format("if %s:\n%s%s\nelse:\n%s", guard, then, elif, elze);
    }

    @Override
    protected String visit(List<String> guards, List<String> thens, TextPosition updatePos, CifContext ctx) {
        String guard = conjoinExprs(guards);
        String then = mergeAll(increaseIndentation(thens), "\n").orElse("\tpass");

        return String.format("elif %s:\n%s", guard, then);
    }

    @Override
    protected String visit(BinaryOperator operator, TextPosition operatorPos, String left, String right,
            CifContext ctx)
    {
        String pyOperator = switch (operator) {
            case EQ -> "==";
            default -> operator.cifValue();
        };
        return String.format("(%s) %s (%s)", left, pyOperator, right);
    }

    @Override
    protected String visit(UnaryOperator operator, TextPosition operatorPos, String child, CifContext ctx) {
        // No conversion needed from CIF to Python
        String pyOperator = operator.cifValue();
        return String.format("%s (%s)", pyOperator, child);
    }

    @Override
    protected String visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx) {
        return String.format("'%s'", literal.getName());
    }

    @Override
    protected String visit(Property property, TextPosition propertyPos, CifContext ctx) {
        return property.getName();
    }

    @Override
    protected String visit(NamedTemplateParameter parameter, TextPosition parameterReferencePos, CifContext ctx) {
        return parameter.getName();
    }

    @Override
    protected String visit(ANameExpression expr, CifContext ctx) {
        if (expr.derivative) {
            throw new CustomSyntaxException("Expected a non-derivative name", expr.position);
        }

        String name = expr.name.name;

        if (name.startsWith(EffectPrestateRenamer.PREFIX)) {
            return name;
        } else {
            return super.visit(expr, ctx);
        }
    }

    @Override
    protected String visit(ABoolExpression expr, CifContext ctx) {
        return expr.value ? "True" : "False";
    }

    @Override
    protected String visit(AIntExpression expr, CifContext ctx) {
        return expr.value;
    }

    @Override
    protected String visit(Optional<String> invKind, List<String> events, TextPosition invariantPos, String predicate,
            CifContext ctx)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String visit(List<String> guards, String thenExpr, List<String> elifs, String elseExpr,
            TextPosition expressionPos, CifContext ctx)
    {
        String guard = conjoinExprs(guards);
        String elif = mergeAll(elifs, " ").map(e -> " " + e).orElse("");

        return String.format("(%s) if (%s)%s else (%s)", thenExpr, guard, elif, elseExpr);
    }

    @Override
    protected String visit(List<String> guards, String thenExpr, TextPosition expressionPos, CifContext ctx) {
        String guard = conjoinExprs(guards);

        return String.format("else (%s) if (%s)", thenExpr, guard);
    }

    /**
     * Increases the tab indentation of the given Python element by one.
     *
     * @param element The Python element.
     * @return The given Python element, with an increased tab indentation.
     */
    static String increaseIndentation(String element) {
        return "\t" + element.replace("\n", "\n\t");
    }

    /**
     * Increases the tab indentation of all given Python elements by one.
     *
     * @param elements The Python elements.
     * @return The given Python elements, with an increased tab indentation.
     */
    static List<String> increaseIndentation(List<String> elements) {
        return elements.stream().map(CifToPythonTranslator::increaseIndentation).toList();
    }

    /**
     * Gives the conjunction of all given Python expressions as a single expression.
     *
     * @param exprs The Python expressions to conjoin.
     * @return The conjunction of all given Python expressions as a single expression.
     */
    static String conjoinExprs(List<String> exprs) {
        return exprs.stream().reduce((left, right) -> String.format("(%s) and (%s)", left, right)).orElse("True");
    }

    /**
     * Merges the given list of Python elements into a single element.
     *
     * @param elements The Python elements to merge.
     * @param delimiter The delimiter for merging the elements.
     * @return The reduced Python element, or {@link Optional#empty()} in case the list of input elements was empty.
     */
    static Optional<String> mergeAll(List<String> elements, String delimiter) {
        return elements.stream().reduce((left, right) -> String.format("%s%s%s", left, delimiter, right));
    }
}
