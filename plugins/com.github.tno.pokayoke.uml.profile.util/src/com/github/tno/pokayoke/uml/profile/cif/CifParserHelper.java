
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdatesParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

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

    public static AExpression parseGuard(Action action) throws SyntaxException {
        if (action == null) {
            return null;
        }
        return parseExpression(PokaYokeUmlProfileUtil.getGuard(action), action);
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

    public static List<AUpdate> parseEffects(Action action) throws SyntaxException {
        if (action == null) {
            return null;
        }
        return parseUpdates(PokaYokeUmlProfileUtil.getEffects(action), action);
    }

    private static String getLocation(Element context) {
        Resource resource = context == null ? null : context.eResource();
        URI uri = resource == null ? null : resource.getURI();
        return uri == null ? "unknown" : uri.toString();
    }
}
