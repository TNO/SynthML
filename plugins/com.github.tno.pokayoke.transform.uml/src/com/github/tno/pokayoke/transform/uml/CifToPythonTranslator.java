
package com.github.tno.pokayoke.transform.uml;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectWalker;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.google.common.base.Optional;

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
    protected String visit(ABoolExpression expr, CifContext ctx) {
        return expr.value ? "True" : "False";
    }

    @Override
    protected String visit(AIntExpression expr, CifContext ctx) {
        return expr.value;
    }

    @Override
    protected String visit(TextPosition operatorPos, List<String> guards, String then, List<String> elifs, String elze,
            CifContext ctx)
    {
        String elif = elifs.stream().map(e -> "else " + e).reduce("", (left, right) -> left + " " + right);
        return String.format("(%s) if (%s) %s else (%s)", then, combineAnd(guards), elif, elze);
    }

    @Override
    protected String visit(TextPosition operatorPos, List<String> guards, String then, CifContext ctx) {
        return String.format("(%s) if (%s)", then, combineAnd(guards));
    }

    // TODO JavaDoc
    private String combineAnd(Collection<String> operands) {
        return operands.stream().reduce((left, right) -> String.format("(%s) and (%s)", left, right)).orElse("True");
    }

    @Override
    protected String visit(Optional<String> invKind, List<String> events, TextPosition operatorPos, String predicate,
            CifContext ctx)
    {
        throw new NotImplementedException();
    }
}
