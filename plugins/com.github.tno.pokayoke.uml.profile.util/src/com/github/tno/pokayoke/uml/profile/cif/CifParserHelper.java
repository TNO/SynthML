
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.cif.parser.CifExpressionParser;
import com.github.tno.pokayoke.cif.parser.CifInvariantParser;
import com.github.tno.pokayoke.cif.parser.CifUpdatesParser;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;

/** Helps parsing CIF expressions. */
public class CifParserHelper {
    private CifParserHelper() {
        // Empty for utility classes
    }

    public static AExpression parseExpression(String expression, Element context) throws SyntaxException {
        if (expression == null) {
            return null;
        } else if (expression.isBlank()) {
            throw new CustomSyntaxException("cannot be blank.", TextPosition.createDummy(getLocation(context)));
        }
        CifExpressionParser expressionParser = new CifExpressionParser();
        return expressionParser.parseString(expression, getLocation(context));
    }

    public static AExpression parseExpression(ValueSpecification valueSpecification) throws SyntaxException {
        if (valueSpecification == null) {
            return null;
        }
        return parseExpression(valueSpecification.stringValue(), valueSpecification);
    }

    public static AExpression parseGuard(RedefinableElement element) throws SyntaxException {
        if (element == null) {
            return null;
        }
        return parseExpression(PokaYokeUmlProfileUtil.getGuard(element), element);
    }

    public static List<AExpression> parseBodies(OpaqueExpression expression) throws SyntaxException {
        if (expression == null) {
            return null;
        }
        List<AExpression> parsedBodies = new LinkedList<>();
        for (String body: expression.getBodies()) {
            parsedBodies.add(parseExpression(body, expression));
        }
        return parsedBodies;
    }

    public static List<AUpdate> parseUpdates(String updates, Element context) throws SyntaxException {
        if (updates == null) {
            return Collections.emptyList();
        } else if (updates.isBlank()) {
            throw new CustomSyntaxException("cannot be blank.", TextPosition.createDummy(getLocation(context)));
        }
        CifUpdatesParser updatesParser = new CifUpdatesParser();
        return updatesParser.parseString(updates, getLocation(context));
    }

    public static List<List<AUpdate>> parseEffects(RedefinableElement element) throws SyntaxException {
        if (element == null) {
            return null;
        }
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<List<AUpdate>> updates = new ArrayList<>(effects.size());
        for (String effect: effects) {
            updates.add(parseUpdates(effect, element));
        }
        return updates;
    }

    public static AInvariant parseInvariant(Constraint constraint) throws SyntaxException {
        if (constraint == null) {
            return null;
        }
        return parseInvariant(constraint.getSpecification());
    }

    public static AInvariant parseInvariant(ValueSpecification valueSpec) throws SyntaxException {
        if (valueSpec == null) {
            return null;
        } else if (valueSpec instanceof OpaqueExpression expr) {
            return parseInvariant(expr);
        } else {
            throw new RuntimeException("Unsupported value specification: " + valueSpec);
        }
    }

    public static AInvariant parseInvariant(OpaqueExpression expression) throws SyntaxException {
        if (expression == null) {
            return null;
        }
        List<String> bodies = expression.getBodies();
        Preconditions.checkArgument(bodies.size() <= 1, "Expected at most one body, but got " + bodies.size());
        return parseInvariant(bodies.isEmpty() ? "true" : bodies.get(0), expression);
    }

    public static AInvariant parseInvariant(String invariant, Element context) throws SyntaxException {
        if (invariant == null) {
            return null;
        } else if (invariant.isBlank()) {
            throw new CustomSyntaxException("cannot be blank.", TextPosition.createDummy(getLocation(context)));
        }
        return new CifInvariantParser().parseString(invariant, getLocation(context));
    }

    private static String getLocation(Element context) {
        Resource resource = context == null ? null : context.eResource();
        URI uri = resource == null ? null : resource.getURI();
        return uri == null ? "unknown" : uri.toString();
    }
}
