
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumDecl;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteral;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newVariableValue;

import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifValidationUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.declarations.VariableValue;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Verify;

public class CIFHelper {
    private CIFHelper() {
    }

    /**
     * Initialize the automaton for the activity diagram. Add enumeration data, properties, event and edge variables to
     * the automaton.
     *
     * @param model The UML model to transform.
     * @param activity The main activity diagram in the model to transform.
     * @param dataStore The name map.
     * @return Initialized automaton
     */
    public static Automaton initializeAutomaton(Model model, Activity activity, DataStore dataStore) {
        Automaton aut = newAutomaton();
        // Transform enumeration data type and literal.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration UMLEnumVariable) {
                EnumDecl cifEnum = transformEnumerations(UMLEnumVariable, dataStore);
                aut.getDeclarations().add(cifEnum);
                dataStore.addEnumeration(UMLEnumVariable.getName(), cifEnum);
            }
        }

        // Transform properties (data variables).
        Class contextClass = (Class)model.getMember("Context");
        for (Property property: contextClass.getAllAttributes()) {
            String dataType = property.getType().getName();
            // Transform boolean variables.
            if (dataType.equals("Boolean")) {
                validateNaming(property.getName());
                DiscVariable cifBoolVariable = newDiscVariable(property.getName(), null, newBoolType(), null);
                // The default value of attributes can be empty (null): https://www.omg.org/spec/FUML/1.5/PDF (page 32).
                if (property.getDefaultValue() != null) {
                    cifBoolVariable.setValue(extractBoolVariableValue(property));
                }
                aut.getDeclarations().add(cifBoolVariable);
                dataStore.addVariable(cifBoolVariable.getName(), cifBoolVariable);
            }
            // Transform the enum variable if its data type is a defined enumeration.
            if (dataStore.isEnumeration(dataType)) {
                EnumType enumType = newEnumType(dataStore.getEnumeration(dataType), null);
                DiscVariable cifEnumVariable = newDiscVariable(property.getName(), null, enumType, null);
                // The default value of attributes can be empty (null): https://www.omg.org/spec/FUML/1.5/PDF (page 32).
                if (property.getDefault() != null) {
                    cifEnumVariable.setValue(extractEnumLiteral(property, dataStore.getEnumeration(dataType)));
                }

                aut.getDeclarations().add(cifEnumVariable);
                dataStore.addVariable(cifEnumVariable.getName(), cifEnumVariable);
            }
        }

        // Rename join, fork and merge node to make sure that each node has a unique name. This step can be removed if
        // we could ensure unique names in the flattened model.
        renameJoinForkMergeNode(activity);

        // Create an edge variable (boolean) for each edge in the activity diagram.
        createEdgeVariables(activity, aut, dataStore);

        // Create an automaton event for each node in the activity diagram.
        createEvents(activity, aut, dataStore);

        return aut;
    }

    public static void createEdgeVariables(Activity activity, Automaton aut, DataStore dataStore) {
        for (ActivityEdge edge: activity.getEdges()) {
            // Name the edges.
            String source = edge.getSource().getName();
            String target = edge.getTarget().getName();
            String edgeName = "edge__" + source + "__" + target;
            validateNaming(edgeName);
            edge.setName(edgeName);

            // Define a boolean variable and set the initial value to false.
            VariableValue value = newVariableValue();
            value.getValues().add(CifValueUtils.makeFalse());
            DiscVariable cifBoolVariable = newDiscVariable(edgeName, null, newBoolType(), value);

            // Add this variable to the name map and the automaton
            dataStore.addVariable(edgeName, cifBoolVariable);
            aut.getDeclarations().add(cifBoolVariable);
        }
    }

    public static void renameJoinForkMergeNode(Activity activity) {
        int j = 0, m = 0, f = 0;
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof JoinNode) {
                node.setName(node.getClass().getSimpleName().replace("Impl", "") + String.valueOf(j));
                j++;
            }
            if (node instanceof MergeNode) {
                node.setName(node.getClass().getSimpleName().replace("Impl", "") + String.valueOf(m));
                m++;
            }

            if (node instanceof ForkNode) {
                node.setName(node.getClass().getSimpleName().replace("Impl", "") + String.valueOf(f));
                f++;
            }

            if (node instanceof InitialNode) {
                node.setName("InitialNode");
            }

            if (node instanceof ActivityFinalNode) {
                node.setName("ActivityFinalNode");
            }
        }
    }

    public static void createEvents(Activity activity, Automaton aut, DataStore dataStore) {
        for (ActivityNode node: activity.getNodes()) {
            // Define a new event and add it to the map and automaton
            validateNaming(node.getName());
            Event autEvent = newEvent();
            autEvent.setName(node.getName());
            dataStore.addEvent(autEvent.getName(), autEvent);
            aut.getDeclarations().add(autEvent);
        }
    }

    public static EnumDecl transformEnumerations(Enumeration enumVariable, DataStore dataStore) {
        validateNaming(enumVariable.getName());
        EnumDecl cifEnumVariable = newEnumDecl(null, enumVariable.getName(), null);
        // Iterate over enumeration literals
        for (EnumerationLiteral el: enumVariable.getOwnedLiterals()) {
            validateNaming(el.getName());
            EnumLiteral enumLiteral = newEnumLiteral(el.getName(), null);
            cifEnumVariable.getLiterals().add(enumLiteral);
            dataStore.addEnumerationLiteral(el.getName(), enumLiteral);
        }
        return cifEnumVariable;
    }

    public static VariableValue extractBoolVariableValue(Property variable) {
        BoolExpression boolExpress = newBoolExpression();
        boolExpress.setType(newBoolType());
        boolExpress.setValue(variable.getDefaultValue().booleanValue());
        VariableValue value = newVariableValue();
        value.getValues().add(boolExpress);
        return value;
    }

    public static VariableValue createBoolValue(Boolean boolValue) {
        BoolExpression boolExpress = newBoolExpression(null, newBoolType(), boolValue);
        VariableValue value = newVariableValue();
        value.getValues().add(boolExpress);
        return value;
    }

    public static VariableValue extractEnumLiteral(Property variable, EnumDecl enumDecl) {
        EnumType enumType = newEnumType(enumDecl, null);
        EnumLiteral enumLiteral = enumDecl.getLiterals().stream().filter(l -> (l.getName() == variable.getDefault()))
                .collect(Collectors.toList()).get(0);
        EnumLiteralExpression enumExpress = newEnumLiteralExpression(enumLiteral, null, enumType);
        VariableValue value = newVariableValue();
        value.getValues().add(enumExpress);
        return value;
    }

    public static void validateNaming(String name) {
        Verify.verify(CifValidationUtils.isValidName(name), String.format("%s is not a valid CIF name", name));
    }
}
