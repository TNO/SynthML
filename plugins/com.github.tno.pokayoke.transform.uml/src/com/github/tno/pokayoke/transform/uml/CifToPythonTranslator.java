
package com.github.tno.pokayoke.transform.uml;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectWalker;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;

/** Translates basic CIF expressions and updates to Python. */
public class CifToPythonTranslator extends ACifObjectWalker<String> {
    private final CifContext cifContext;

    public CifToPythonTranslator(CifContext cifContext) {
        this.cifContext = cifContext;
    }

    public String translateExpression(AExpression expr) {
        if (expr == null) {
            return "True";
        }
        return visit(expr, cifContext);
    }

    public List<String> translateUpdates(List<AUpdate> updates) {
        return updates.stream().map(this::translateUpdate).collect(Collectors.toList());
    }

    private String translateUpdate(AUpdate update) {
        return visit(update, cifContext);
    }

    @Override
    protected String visit(String addressable, TextPosition assignmentPos, String value, CifContext ctx) {
        return String.format("%s = %s", addressable, value);
    }

    @Override
    protected String visit(BinaryOperator operator, TextPosition operatorPos, String left, String right,
            CifContext ctx)
    {
        String pyOperator = switch (operator) {
            case AND, OR -> operator.cifValue();
            case EQ -> "==";
            default -> throw new IllegalArgumentException("Integer types are not supported yet!");
        };
        return String.format("(%s) %s (%s)", left, pyOperator, right);
    }

    @Override
    protected String visit(UnaryOperator operator, TextPosition operatorPos, String child, CifContext ctx) {
        String pyOperator = switch (operator) {
            case NOT -> operator.cifValue();
            default -> throw new IllegalArgumentException("Integer types are not supported yet!");
        };
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
    protected String visit(ABoolExpression expr, CifContext ctx) {
        return expr.value ? "True" : "False";
    }

    @Override
    protected String visit(AIntExpression expr, CifContext ctx) {
        throw new IllegalArgumentException("Integer types are not supported yet!");
    }
}
