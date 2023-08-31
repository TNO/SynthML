
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
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FlowFinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Verify;

public class CifHelper {
    private CifHelper() {
    }

    /**
     * Add enumerations and variables of the UML model. Add events for the nodes and variables for the edges.
     *
     * @param model The UML model to transform.
     * @param activity The main activity in the model to transform.
     * @param dataStore Where variables and events are stored.
     * @return Initialized automaton.
     */
    public static Automaton initializeAutomaton(Model model, Activity activity, DataStore dataStore) {
        Automaton aut = newAutomaton();

        // Extract and transform enumeration.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration umlEnumeration) {
                EnumDecl cifEnum = transformEnumeration(umlEnumeration, dataStore);
                aut.getDeclarations().add(cifEnum);
                dataStore.addEnumeration(umlEnumeration.getName(), cifEnum);
            }
        }

        // Extract and transform properties (data variables).
        Class contextClass = (Class)model.getMember("Context");
        for (Property property: contextClass.getAllAttributes()) {
            String dataType = property.getType().getName();

            if (dataType.equals("Boolean")) {
                validateName(property.getName());
                DiscVariable cifBoolVariable = newDiscVariable(property.getName(), null, newBoolType(), null);

                // The default value of attributes and properties can be unspecified:
                // https://www.omg.org/spec/FUML/1.5/PDF (page 32 and 39).
                if (property.getDefaultValue() != null) {
                    cifBoolVariable.setValue(createBoolValue(property.getDefaultValue().booleanValue()));
                }
                aut.getDeclarations().add(cifBoolVariable);
                dataStore.addVariable(cifBoolVariable.getName(), cifBoolVariable);
            } else if (dataStore.isEnumeration(dataType)) {
                validateName(property.getName());
                EnumType enumType = newEnumType(dataStore.getEnumeration(dataType), null);
                DiscVariable cifEnum = newDiscVariable(property.getName(), null, enumType, null);

                // The default value of attributes and properties can be unspecified:
                // https://www.omg.org/spec/FUML/1.5/PDF (page 32 and 39).
                if (property.getDefault() != null) {
                    cifEnum.setValue(extractEnumLiteral(property, dataStore.getEnumeration(dataType)));
                }
                aut.getDeclarations().add(cifEnum);
                dataStore.addVariable(cifEnum.getName(), cifEnum);
            }
        }

        // Rename join, fork, merge, decision, initial and final nodes to make sure that each node has a unique name.
        // This step can be removed if we ensure unique names in the flattened model.
        renameJoinForkMergeDecisionInitialFinalNodes(activity);

        // Create an edge variable (boolean) for each edge in the activity.
        createEdgeVariables(activity, aut, dataStore);

        // Create an automaton event for each node in the activity.
        createEvents(activity, aut, dataStore);

        return aut;
    }

    public static void createEdgeVariables(Activity activity, Automaton aut, DataStore dataStore) {
        for (ActivityEdge edge: activity.getEdges()) {
            // Name the edges.
            String source = edge.getSource().getName();
            String target = edge.getTarget().getName();
            String edgeName = "edge__" + source + "__" + target;
            validateName(edgeName);
            edge.setName(edgeName);

            // Define a boolean variable and set the initial value to true if its source is an initial node, otherwise,
            // set the value to false.
            VariableValue value = newVariableValue();
            if (edge.getSource() instanceof InitialNode) {
                value.getValues().add(CifValueUtils.makeTrue());
            } else {
                value.getValues().add(CifValueUtils.makeFalse());
            }
            DiscVariable cifBoolVariable = newDiscVariable(edgeName, null, newBoolType(), value);

            // Add this variable to the name map and the automaton.
            dataStore.addVariable(edgeName, cifBoolVariable);
            aut.getDeclarations().add(cifBoolVariable);
        }
    }

    public static void renameJoinForkMergeDecisionInitialFinalNodes(Activity activity) {
        int j = 0, m = 0, f = 0, i = 0, a = 0, ff = 0, d = 0;
        for (ActivityNode node: activity.getNodes()) {
            assert node.getName() == null;
            if (node instanceof JoinNode) {
                node.setName(node.eClass().getName() + String.valueOf(j));
                j++;
            } else if (node instanceof MergeNode) {
                node.setName(node.eClass().getName() + String.valueOf(m));
                m++;
            } else if (node instanceof ForkNode) {
                node.setName(node.eClass().getName() + String.valueOf(f));
                f++;
            } else if (node instanceof DecisionNode) {
                node.setName(node.eClass().getName() + String.valueOf(d));
                d++;
            } else if (node instanceof InitialNode) {
                // An activity diagram can have multiple initial nodes which invoke concurrent executions of multiple
                // flows: https://www.omg.org/spec/UML/2.5.1/PDF (page 429).
                node.setName(node.eClass().getName() + String.valueOf(i));
                i++;
            } else if (node instanceof ActivityFinalNode) {
                // An activity diagram can have multiple activity final nodes. The first one to accept a token
                // terminates the execution of the entire activity: https://www.omg.org/spec/UML/2.5.1/PDF (page 430).
                node.setName(node.eClass().getName() + String.valueOf(a));
                a++;
            } else if (node instanceof FlowFinalNode) {
                node.setName(node.eClass().getName() + String.valueOf(ff));
                ff++;
            }
            assert node.getName() != null;
        }
    }

    public static void createEvents(Activity activity, Automaton aut, DataStore dataStore) {
        // Define an event for each node and add them to the map and automaton.
        for (ActivityNode node: activity.getNodes()) {
            validateName(node.getName());
            Event autEvent = newEvent();
            autEvent.setName(node.getName());
            dataStore.addEvent(autEvent.getName(), autEvent);
            aut.getDeclarations().add(autEvent);
        }
    }

    public static EnumDecl transformEnumeration(Enumeration enumeration, DataStore dataStore) {
        validateName(enumeration.getName());
        EnumDecl cifEnumDecl = newEnumDecl(null, enumeration.getName(), null);
        dataStore.addEnumeration(enumeration.getName(), cifEnumDecl);
        for (EnumerationLiteral umlEnumLiteral: enumeration.getOwnedLiterals()) {
            validateName(umlEnumLiteral.getName());
            EnumLiteral cifEnumLiteral = newEnumLiteral(umlEnumLiteral.getName(), null);
            cifEnumDecl.getLiterals().add(cifEnumLiteral);
            dataStore.addEnumerationLiteral(umlEnumLiteral.getName(), cifEnumLiteral, cifEnumDecl);
        }
        return cifEnumDecl;
    }

    public static VariableValue createBoolValue(boolean boolValue) {
        BoolExpression boolExpress = newBoolExpression(null, newBoolType(), boolValue);
        VariableValue value = newVariableValue();
        value.getValues().add(boolExpress);
        return value;
    }

    public static VariableValue extractEnumLiteral(Property variable, EnumDecl enumDecl) {
        EnumType enumType = newEnumType(enumDecl, null);
        EnumLiteral enumLiteral = enumDecl.getLiterals().stream()
                .filter(l -> (l.getName().equals(variable.getDefault()))).findFirst().get();
        EnumLiteralExpression enumExpress = newEnumLiteralExpression(enumLiteral, null, enumType);
        VariableValue value = newVariableValue();
        value.getValues().add(enumExpress);
        return value;
    }

    public static void validateName(String name) {
        Verify.verify(CifValidationUtils.isValidIdentifier(name), String.format("%s is not a valid CIF name.", name));
    }
}
