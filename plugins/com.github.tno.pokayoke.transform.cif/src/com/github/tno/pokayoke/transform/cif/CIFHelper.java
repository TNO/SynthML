/**
 *
 */

package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.escet.cif.metamodel.cif.automata.AutomataFactory;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.VariableValue;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.common.position.metamodel.position.Position;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.transform.common.FileHelper;

public class CIFHelper {
    private final Model model;

    static final AutomataFactory FACTORY = AutomataFactory.eINSTANCE;

    public CIFHelper(Model model) {
        this.model = model;
    }

    /**
     * Initialize the automaton for the activity.
     *
     * @param activity The activity diagram to be transformed.
     * @return Initialized automaton
     */
    public static Automaton initializeAutomaton(Model model) {
        Automaton aut = newAutomaton();
        // Transform enumeration data type and literal
        List<String> enumVariable = new ArrayList<String>();
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration UMLEnumVariable) {
                EnumDecl cifEnum = transformEnumerations(UMLEnumVariable);
                aut.getDeclarations().add(cifEnum);
                enumVariable.add(cifEnum.getName());
            }
        }

        // Transform properties (data variables)
        Class contextClass = (Class)model.getMember("Context");
        for (Property property: contextClass.getAllAttributes()) {
            String dataType = property.getType().getName();
            // Transform boolean variables
            if (dataType.equals("Boolean")) {
                DiscVariable cifBoolVariable = newDiscVariable();
                cifBoolVariable.setName(property.getName());
                cifBoolVariable.setType(newBoolType());
                if (property.getDefaultValue() != null) {
                    cifBoolVariable.setValue(extractBoolVariableValue(property));
                }
                aut.getDeclarations().add(cifBoolVariable);
            }
            // Transform enum variables if the data type is one of the extracted enumeration
            if (enumVariable.contains(dataType)) {
                DiscVariable cifEnumVariable = newDiscVariable();
                cifEnumVariable.setName(property.getName());
                cifEnumVariable.setType(newEnumType());
                if (property.getDefault() != null) {
                    cifEnumVariable.setValue(extractEnumLiteral(property));
                }
                aut.getDeclarations().add(cifEnumVariable);
            }
        }

        return aut;
    }

    public static EnumDecl transformEnumerations(Enumeration enumVariable) {
        EnumDecl cifEnumVariable = newEnumDecl();
        cifEnumVariable.setName(enumVariable.getName());
        for (EnumerationLiteral el: enumVariable.getOwnedLiterals()) {
            cifEnumVariable.getLiterals().add(newEnumLiteral(el.toString(), null));
        }
        return cifEnumVariable;
    }

    public static VariableValue extractBoolVariableValue(Property variable) {
        // check if there is a default value in UML

        VariableValue value = newVariableValue();
        BoolExpression boolExpress = newBoolExpression();
        boolExpress.setType(newBoolType());
        boolExpress.setValue(variable.getDefaultValue().booleanValue());
        value.getValues().add(boolExpress);
        return value;
    }

    public static VariableValue extractEnumLiteral(Property variable) {
        VariableValue value = newVariableValue();
        EnumLiteralExpression enumExpress = newEnumLiteralExpression();
        enumExpress.setType(newEnumType());
        enumExpress.setLiteral(newEnumLiteral(variable.getDefault(), null));
        value.getValues().add(enumExpress);
        return value;
    }


    public static void main(String[] args) {
        Model model = FileHelper.loadModel("C:\\Users\\nanyang\\workspace\\NestedDiagram\\flattened_model.uml");
        new CIFHelper(model).initializeAutomaton(model);
        // FileHelper.storeModel(model, targetPath);
    }
}
