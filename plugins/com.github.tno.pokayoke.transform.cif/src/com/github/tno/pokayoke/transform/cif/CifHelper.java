
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdge;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdgeEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumDecl;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteral;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEventExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newVariableValue;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifValidationUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.declarations.VariableValue;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
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
     * Validates if the names of the model are CIF valid.
     *
     * @param model The model to validate.
     */
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

    /**
     * Validates if the name is CIF valid.
     *
     * @param name The name to validate.
     */
    public static void validateName(String name) {
        Verify.verify(CifValidationUtils.isValidIdentifier(name), String.format("%s is not a valid CIF name.", name));
    }

    /**
     * Adds enumerations and variables of the UML model. Adds events for the nodes and variables for the edges.
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
                // https://www.omg.org/spec/FUML/1.5/PDF (page 32 and 39). A CIF value is created if a default value
                // exists for this property.
                if (property.getDefaultValue() != null) {
                    cifBoolVariable.setValue(createBoolValue(property.getDefaultValue().booleanValue()));
                }
                aut.getDeclarations().add(cifBoolVariable);
                dataStore.addVariable(cifBoolVariable.getName(), cifBoolVariable);
            } else if (dataStore.isEnumeration(dataType)) {
                EnumType enumType = newEnumType(dataStore.getEnumeration(dataType), null);
                DiscVariable cifEnum = newDiscVariable(property.getName(), null, enumType, null);

                // The default value of attributes and properties can be unspecified:
                // https://www.omg.org/spec/FUML/1.5/PDF (page 32 and 39). A CIF value is created if a default value
                // exists for this property.
                if (property.getDefaultValue() != null) {
                    EnumLiteral enumLiteral = dataStore.getEnumerationLiteral(property.getDefaultValue().stringValue());
                    EnumDecl enumeration = dataStore.getEnumeration(enumLiteral);
                    cifEnum.setValue(createEnumLiteralValue(enumLiteral, enumeration));
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

    /**
     * Transforms a UML enumeration into CIF enumeration declaration and adds it to the data store.
     *
     * @param enumeration The enumeration to transform.
     * @param dataStore Where enumeration declarations are stored.
     * @return A created enumeration declaration.
     */
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

    /**
     * Creates a CIF boolean value.
     *
     * @param boolValue The boolean value.
     * @return The created CIF boolean value.
     */
    private static VariableValue createBoolValue(boolean boolValue) {
        BoolExpression boolExpress = newBoolExpression(null, newBoolType(), boolValue);
        VariableValue value = newVariableValue();
        value.getValues().add(boolExpress);
        return value;
    }

    /**
     * Creates a CIF enumeration literal value.
     *
     * @param enumLiteral The enumeration literal.
     * @param enumDecl The corresponding enumeration declaration.
     * @return The created CIF enumeration literal value.
     */
    private static VariableValue createEnumLiteralValue(EnumLiteral enumLiteral, EnumDecl enumDecl) {
        EnumType enumType = newEnumType(enumDecl, null);
        EnumLiteralExpression enumExpress = newEnumLiteralExpression(enumLiteral, null, enumType);
        VariableValue value = newVariableValue();
        value.getValues().add(enumExpress);
        return value;
    }

    /**
     * Creates edge variables for edges in the activity, and adds them to the automaton and the data store.
     *
     * @param activity The activity that contains edges.
     * @param aut The automaton that contains the created edge variables.
     * @param dataStore Where edge variables are stored.
     */
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

    /**
     * Creates events for nodes in the activity, and adds them to the automaton and the data store.
     *
     * @param activity The activity that contains nodes.
     * @param aut The automaton that contains the created events.
     * @param dataStore Where events are stored.
     */
    public static void createEvents(Activity activity, Automaton aut, DataStore dataStore) {
        // Define an event for each node and add them to the map and automaton.
        for (ActivityNode node: activity.getNodes()) {
            Event autEvent = newEvent();
            autEvent.setName(node.getName());
            dataStore.addEvent(autEvent.getName(), autEvent);
            aut.getDeclarations().add(autEvent);
        }
    }

    /**
     * Creates CIF edge and edge event with an event, and adds the edge to the location.
     *
     * @param location The location.
     * @param event The event.
     * @return The created edge.
     */
    public static Edge createCifEdgeAndEdgeEvent(Location location, Event event) {
        // Define a CIF edge and add it to the location.
        Edge cifEdge = newEdge();
        location.getEdges().add(cifEdge);

        // Define a CIF edge event and add it to the CIF edge.
        EdgeEvent cifEdgeEvent = newEdgeEvent();
        cifEdgeEvent.setEvent(newEventExpression(event, null, newBoolType()));
        cifEdge.getEvents().add(cifEdgeEvent);

        return cifEdge;
    }

    /**
     * Adds a guard on an edge and updates the value of an incoming edge variable.
     *
     * @param edgeVariableName The name of the edge variable whose value needs to be updated.
     * @param cifEdge The edge to update.
     * @param dataStore Where edge variables are stored.
     */
    public static void addGuardAndUpdateIncomingEdgeVariable(String edgeVariableName, Edge cifEdge,
            DataStore dataStore)
    {
        // Extract the edge variable for the incoming edge.
        DiscVariable edgeVariable = dataStore.getVariable(edgeVariableName);

        // Add the guard to the edge.
        cifEdge.getGuards().add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

        // Set the incoming edge variable to false.
        cifEdge.getUpdates().add(createAssignmentForEdgeVariableUpdate(edgeVariable, false));
    }

    /**
     * Updates the value of an outgoing edge variable.
     *
     * @param edgeVariableName The name of the edge variable whose value needs to be updated.
     * @param cifEdge The edge to update.
     * @param dataStore Where edge variables are stored.
     */
    public static void updateOutgoingEdgeVariable(String edgeVariableName, Edge cifEdge, DataStore dataStore) {
        // Set the outgoing edge variable to true.
        DiscVariable outgoingEdgedVariable = dataStore.getVariable(edgeVariableName);
        cifEdge.getUpdates().add(createAssignmentForEdgeVariableUpdate(outgoingEdgedVariable, true));
    }

    /**
     * Creates an assignment to update edge variable.
     *
     * @param edgeVariable The edge variable.
     * @param value The value to assign.
     * @return The created assignment.
     */
    public static Assignment createAssignmentForEdgeVariableUpdate(DiscVariable edgeVariable, boolean value) {
        DiscVariableExpression addressableVar = newDiscVariableExpression(null, newBoolType(), edgeVariable);
        Assignment assign = newAssignment(addressableVar, null, newBoolExpression(null, newBoolType(), value));
        return assign;
    }
}
