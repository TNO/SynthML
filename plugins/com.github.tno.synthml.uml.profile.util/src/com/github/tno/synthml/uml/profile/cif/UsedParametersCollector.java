
package com.github.tno.synthml.uml.profile.cif;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

/** Finds all template parameters used in the parse tree rooted at the provided element. */
public class UsedParametersCollector extends ACifObjectWalker<Stream<NamedTemplateParameter>> {
    public UsedParametersCollector() {
    }

    private static Stream<NamedTemplateParameter> concat(Stream<Stream<NamedTemplateParameter>> streams) {
        return streams.flatMap(Function.identity());
    }

    private static Stream<NamedTemplateParameter> concat(List<Stream<NamedTemplateParameter>> streams) {
        return concat(streams.stream());
    }

    public Stream<NamedTemplateParameter> collect(ACifObject expr, CifContext ctx) {
        return visit(expr, ctx);
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(Stream<NamedTemplateParameter> addressable,
            TextPosition assignmentPos, Stream<NamedTemplateParameter> value, CifContext ctx)
    {
        return Stream.concat(addressable, value);
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(List<Stream<NamedTemplateParameter>> guards,
            List<Stream<NamedTemplateParameter>> thens, List<Stream<NamedTemplateParameter>> elifs,
            List<Stream<NamedTemplateParameter>> elses, TextPosition updatePos, CifContext ctx)
    {
        return concat(Stream.of(concat(guards), concat(thens), concat(elifs), concat(elses)));
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(List<Stream<NamedTemplateParameter>> guards,
            List<Stream<NamedTemplateParameter>> thens, TextPosition updatePos, CifContext ctx)
    {
        return Stream.concat(concat(guards), concat(thens));
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(BinaryOperator operator, TextPosition operatorPos,
            Stream<NamedTemplateParameter> left, Stream<NamedTemplateParameter> right, CifContext ctx)
    {
        return Stream.concat(left, right);
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(EnumerationLiteral literal, TextPosition literalPos,
            CifContext ctx)
    {
        return Stream.empty();
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(Property property, TextPosition propertyPos, CifContext ctx) {
        return Stream.empty();
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(NamedTemplateParameter parameter, TextPosition parameterReferencePos,
            CifContext ctx)
    {
        return Stream.of(parameter);
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(UnaryOperator operator, TextPosition operatorPos,
            Stream<NamedTemplateParameter> child, CifContext ctx)
    {
        return child;
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(Optional<String> invKind, List<String> events,
            TextPosition invariantPos, Stream<NamedTemplateParameter> predicate, CifContext ctx)
    {
        return predicate;
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(List<Stream<NamedTemplateParameter>> guards,
            Stream<NamedTemplateParameter> thenExpr, List<Stream<NamedTemplateParameter>> elifs,
            Stream<NamedTemplateParameter> elseExpr, TextPosition expressionPos, CifContext ctx)
    {
        return concat(Stream.of(concat(guards), thenExpr, concat(elifs), elseExpr));
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(List<Stream<NamedTemplateParameter>> guards,
            Stream<NamedTemplateParameter> thenExpr, TextPosition expressionPos, CifContext ctx)
    {
        return Stream.concat(concat(guards), thenExpr);
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(ABoolExpression expr, CifContext ctx) {
        return Stream.empty();
    }

    @Override
    protected Stream<NamedTemplateParameter> visit(AIntExpression expr, CifContext ctx) {
        return Stream.empty();
    }
}
