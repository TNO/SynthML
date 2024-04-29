
package com.github.tno.pokayoke.uml.profile.validation;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.cif.TypeException;

public class PropertyDefaultValueTypeChecker extends CifTypeChecker {
    private static final String MESSAGE = "only literals are supported for property default values.";

    /**
     * Constructs a new property default value type checker.
     *
     * @param elem The context for evaluating the expression.
     */
    public PropertyDefaultValueTypeChecker(Element elem) {
        super(elem);
    }

    @Override
    protected Type visit(Property property, TextPosition propertyPos, CifContext ctx) {
        throw new TypeException(MESSAGE, propertyPos);
    }

    @Override
    protected Type visit(AAssignmentUpdate update, CifContext ctx) {
        throw new TypeException(MESSAGE, update.position);
    }

    @Override
    protected Type visit(Type addressable, TextPosition assignmentPos, Type value, CifContext ctx) {
        throw new TypeException(MESSAGE, assignmentPos);
    }

    @Override
    protected Type visit(BinaryOperator operator, TextPosition operatorPos, Type left, Type right, CifContext ctx) {
        throw new TypeException(MESSAGE, operatorPos);
    }

    @Override
    protected Type visit(UnaryOperator operator, TextPosition operatorPos, Type child, CifContext ctx) {
        throw new TypeException(MESSAGE, operatorPos);
    }
}
