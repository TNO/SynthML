
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
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
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InitialNode;
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

        // Create an edge variable (boolean) for each edge in the activity.
        createEdgeVariables(activity, aut, dataStore);

        // Create an automaton event for each node in the activity.
        createEvents(activity, aut, dataStore);

        return aut;
    }

    public static void createEdgeVariables(Activity activity, Automaton aut, DataStore dataStore) {
        for (ActivityEdge edge: activity.getEdges()) {
            // Define a boolean variable and set the initial value to true if its source is an initial node, otherwise,
            // set the value to false.
            VariableValue value = newVariableValue();
            if (edge.getSource() instanceof InitialNode) {
                value.getValues().add(CifValueUtils.makeTrue());
            } else {
                value.getValues().add(CifValueUtils.makeFalse());
            }
            DiscVariable cifBoolVariable = newDiscVariable(edge.getName(), null, newBoolType(), value);

            // Add this variable to the name map and the automaton.
            dataStore.addVariable(edge.getName(), cifBoolVariable);
            aut.getDeclarations().add(cifBoolVariable);
        }
    }

    public static void createEvents(Activity activity, Automaton aut, DataStore dataStore) {
        // Define an event for each node and add them to the map and automaton.
        for (ActivityNode node: activity.getNodes()) {
            Event autEvent = newEvent();
            autEvent.setName(node.getName());
            dataStore.addEvent(autEvent.getName(), autEvent);
            aut.getDeclarations().add(autEvent);
        }
    }

    public static EnumDecl transformEnumeration(Enumeration enumeration, DataStore dataStore) {
        EnumDecl cifEnumDecl = newEnumDecl(null, enumeration.getName(), null);
        dataStore.addEnumeration(enumeration.getName(), cifEnumDecl);
        for (EnumerationLiteral umlEnumLiteral: enumeration.getOwnedLiterals()) {
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

    public static Map<String, List<ActivityEdge>> extractMapFromCallBehaviorActionToBoundaryEdge(Activity activity) {
        List<ActivityEdge> boundaryEdges = activity.getEdges().stream()
                .filter(k -> k.getName().contains("CallBehaviorAction") && k.getName().contains("Added")).toList();
        Map<String, List<ActivityEdge>> actionToEdges = new HashMap<>();
        for (ActivityEdge element: boundaryEdges) {
            String[] chunks = element.getName().split("__");
            for (String chunk: chunks) {
                if (chunk.contains("CallBehaviorAction")) {
                    if (!actionToEdges.containsKey(chunk)) {
                        actionToEdges.put(chunk, new ArrayList<>());
                    }
                    actionToEdges.get(chunk).add(element);
                }
            }
        }
        return actionToEdges;
    }

    public static List<ActivityNode> identifyTargetssOfIncomingEdges(List<ActivityEdge> edges) {
        List<ActivityEdge> incomingEdges = edges.stream().filter(e -> e.getName().contains("Incoming")).toList();
        List<ActivityNode> targetNodes = incomingEdges.stream().map(edge -> edge.getTarget()).toList();
        return targetNodes;
    }

    public static List<ActivityNode> identifySourcesOfOutgoingEdges(List<ActivityEdge> edges) {
        List<ActivityEdge> outgoingEdges = edges.stream().filter(e -> e.getName().contains("Outgoing")).toList();
        List<ActivityNode> sourceNodes = outgoingEdges.stream().map(edge -> edge.getSource()).toList();
        return sourceNodes;
    }

    public static void validateName(String name) {
        Verify.verify(CifValidationUtils.isValidIdentifier(name), String.format("%s is not a valid CIF name.", name));
    }

    public static void validateNames(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement nameElement) {
                if (nameElement.getName() != null && !nameElement.getName().equals("")) {
                    validateName(nameElement.getName());
                }
            }
        }
    }
}
