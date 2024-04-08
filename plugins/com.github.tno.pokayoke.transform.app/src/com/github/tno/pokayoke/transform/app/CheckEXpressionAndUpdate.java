
package com.github.tno.pokayoke.transform.app;

import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.InputVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.java.CifWalker;

/** Check that an expression or update does not contain location expressions and input variable expressions. */
public class CheckEXpressionAndUpdate extends CifWalker {
    public void check(Expression expression) {
        walkExpression(expression);
    }

    public void check(Update update) {
        walkUpdate(update);
    }

    @Override
    public void preprocessLocationExpression(LocationExpression expression) {
        throw new AssertionError("Unexpected location expression in choice guard.");
    }

    @Override
    public void preprocessInputVariableExpression(InputVariableExpression expression) {
        throw new AssertionError("Unexpected input variable expression in choice guard.");
    }

    @Override
    public void walkExpression(Expression expression) {
        super.walkExpression(expression);
    }
}
