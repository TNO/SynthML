
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.FlowFinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Preconditions;

/** Transforms flattened UML model into CIF model. */
public class UmlActitityToCifTransformer {
    private final Model model;

    private final CifUpdateParser updateParser = new CifUpdateParser();

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final Specification spec = newSpecification();

    private final String modelPath;

    private final DataStore dataStore;

    private final CifToCifTranslator translator;

    public UmlActitityToCifTransformer(Model model, String modelPath) {
        this.model = model;
        this.modelPath = modelPath;
        this.dataStore = new DataStore();
        this.translator = new CifToCifTranslator(this.dataStore);
    }

    public static void transformFile(String sourcePath, String targetPath) {
        Model model = FileHelper.loadModel(sourcePath);
        Specification specification = new UmlActitityToCifTransformer(model, sourcePath).transformModel();
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(specification, targetPath, Paths.get(targetPath).getParent().toString());
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    public Specification transformModel() {
        // Check if all elements in the model have a CIF valid name.
        CifHelper.validateNames(model);

        // Extract the context class.
        Class contextClass = (Class)model.getMember("Context");

        // Transform the main activity in the contextClass.
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity && activity.getName().equals("main")) {
                Automaton aut = createAutomaton(model, activity, dataStore);
                CifHelper.validateName(model.getName());
                aut.setName(model.getName());
                spec.getComponents().add(aut);
                transformActivity(activity, aut);
            }
        }
        return spec;
    }

    private void transformActivity(Activity activity, Automaton aut) {
        // Create the only location and add it to the automaton.
        Location location = newLocation();
        location.getInitials().add(CifValueUtils.makeTrue());
        location.getMarkeds().add(CifValueUtils.makeTrue());
        aut.getLocations().add(location);

        // Transform all nodes and their connected edges.
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof ForkNode) {
                // Check if there is only one incoming edge.
                Preconditions.checkArgument(node.getIncomings().size() == 1,
                        "Expected that fork node has only one incoming edge.");

                // Get the node event.
                Event nodeEvent = dataStore.getEvent(node.getName());

                // Create an automaton edge and its edge event.
                Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                // Add a guard to the automaton edge with the incoming edge variable and update the value of the edge
                // variable to
                // false.
                ActivityEdge incomingEdge = node.getIncomings().get(0);
                CifHelper.addGuardAndUpdateIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                // Set the value of the outgoing edge variables to true.
                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                }
            } else if (node instanceof MergeNode) {
                // Check if the merge node only has one outgoing edge.
                Preconditions.checkArgument(node.getOutgoings().size() == 1,
                        "Expected that merge node has only one outgoing edge.");

                // Get the node event.
                Event nodeEvent = dataStore.getEvent(node.getName());

                // For each incoming edge, an automaton edge and its info (i.e., guard and update) is transformed.
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Create an automaton edge and its edge event.
                    Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                    // Add a guard to the automaton edge with the incoming edge variable and update the value of the
                    // variable to
                    // false.
                    CifHelper.addGuardAndUpdateIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                    // Set the value of the outgoing edge variables to true.
                    ActivityEdge outgoingEdge = node.getOutgoings().get(0);
                    CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                }
            } else if (node instanceof JoinNode) {
                // Check if the join node only has only one outgoing edge.
                Preconditions.checkArgument(node.getOutgoings().size() == 1,
                        "Expected that JoinNode has only one outgoing edge.");

                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());

                // Create an automaton edge and its edge event.
                Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                // Add guards to the edge with the incoming edge variables and update the values of the edge variables
                // to false.
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    CifHelper.addGuardAndUpdateIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);
                }

                // Set the outgoing edge variables to true.
                ActivityEdge outgoingEdge = node.getOutgoings().get(0);
                CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
            } else if (node instanceof OpaqueAction action) {
                // fUML requires that an action with input pins has an offer on at least one of the pins before it
                // fires: from https://www.omg.org/spec/FUML/1.5/PDF (page 175).
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Get node event.
                    Event nodeEvent = dataStore.getEvent(node.getName());

                    // Create an automaton edge and its edge event.
                    Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                    // Add a guard to the automaton edge with the incoming edge variable and update the value of the
                    // edge variable
                    // to false.
                    CifHelper.addGuardAndUpdateIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                    // Extract the guard of the action from CIF text in the body of the action.
                    // If the action has at least one body, then parse the first body, which is assumed to be its guard.
                    List<Expression> guards = action.getBodies().stream().limit(1)
                            .map(b -> expressionParser.parseString(b, modelPath))
                            .map(b -> translator.translateExpression(b)).collect(Collectors.toList());

                    // Add the extracted guards to the automaton edge.
                    cifEdge.getGuards().addAll(guards);

                    // Extract the effect of the action from CIF text in the body of the action.
                    // Parse all bodies except the first one, all of which should be updates.
                    List<Assignment> effects = action.getBodies().stream().skip(1)
                            .map(b -> updateParser.parseString(b, modelPath)).map(b -> translator.translateUpdate(b))
                            .collect(Collectors.toList());

                    // Add the assignments to the update of the automaton edge.
                    cifEdge.getUpdates().addAll(effects);

                    // Set the edge variables for the outgoing edges to true. If there are multiple outgoing edges from
                    // an action, a thread split and each outgoing thread executes concurrently, i.e., implicit fork:
                    // https://www.omg.org/spec/FUML/1.5/PDF (pages 175 and 225).
                    for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                        CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                    }
                }
            } else if (node instanceof DecisionNode) {
                // Check if the decision node only has one incoming edge.
                Preconditions.checkArgument(node.getIncomings().size() == 1,
                        "Expected that decision node has only one outgoing edge.");

                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    // Create an automaton edge and its edge event.
                    Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                    // Add a guard to the automaton edge with the incoming edge variable.
                    DiscVariable incomingEdgedVariable = dataStore.getVariable(node.getIncomings().get(0).getName());
                    cifEdge.getGuards().add(newDiscVariableExpression(null,
                            EcoreUtil.copy(incomingEdgedVariable.getType()), incomingEdgedVariable));

                    // Extract the guard of the outgoing edge from the decision node and add it to the edge.
                    String guard = outgoingEdge.getGuard().getName();
                    Expression guardExpress = translator
                            .translateExpression(expressionParser.parseString(guard, modelPath));
                    cifEdge.getGuards().add(guardExpress);

                    // Set the value of the incoming edge variable to false.
                    cifEdge.getUpdates()
                            .add(CifHelper.createAssignmentForEdgeVariableUpdate(incomingEdgedVariable, false));

                    // Set the value of the outgoing edge variable to true.
                    CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                }
            } else if (node instanceof ActivityFinalNode) {
                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Create an automaton edge and its edge event.
                    Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                    // Extract the automaton edge variable for the incoming edge.
                    DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());

                    // Add a guard to the automaton edge with the edge variable.
                    cifEdge.getGuards()
                            .add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

                    // Set all edge variables to false. According to UML specification, the execution of activity is
                    // terminated when the first activity final node is fired: https://www.omg.org/spec/UML/2.5.1/PDF
                    // (page 430).
                    for (ActivityEdge edge: activity.getEdges()) {
                        // Set the value of the incoming edge variable to false.
                        cifEdge.getUpdates().add(CifHelper
                                .createAssignmentForEdgeVariableUpdate(dataStore.getVariable(edge.getName()), false));
                    }
                }
            } else if (node instanceof FlowFinalNode) {
                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Create an automaton edge and its edge event.
                    Edge cifEdge = CifHelper.createEdgeAndEdgeEvent(location, nodeEvent);

                    // Add a guard to the automaton edge with the incoming edge variable and update its value to false.
                    // Only
                    // the execution of this control flow is terminated. Other flows in the activity can still continue.
                    CifHelper.addGuardAndUpdateIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);
                }
            }
        }
    }

    /**
     * Creates a new CIF automaton for a UML model. Adds enumerations and variables of the UML model to the CIF
     * automaton. Also adds CIF events for the UML nodes, and CIF variables for the UML edges.
     *
     * @param model The UML model to transform.
     * @param activity The main activity in the model to transform.
     * @param dataStore Storage for keeping track of the elements of the CIF model.
     * @return The new CIF automaton.
     */
    public static Automaton createAutomaton(Model model, Activity activity, DataStore dataStore) {
        Automaton aut = newAutomaton();

        // Extract and transform enumerations.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration umlEnumeration) {
                EnumDecl cifEnum = CifHelper.transformEnumeration(umlEnumeration, dataStore);
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
                    cifBoolVariable.setValue(CifHelper.createBoolValue(property.getDefaultValue().booleanValue()));
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
                    cifEnum.setValue(CifHelper.createEnumLiteralValue(enumLiteral, enumeration));
                }
                aut.getDeclarations().add(cifEnum);
                dataStore.addVariable(cifEnum.getName(), cifEnum);
            }
        }

        // Create an edge variable (boolean) for each edge in the activity.
        CifHelper.createEdgeVariables(activity, aut, dataStore);

        // Create an automaton event for each node in the activity.
        CifHelper.createEvents(activity, aut, dataStore);

        return aut;
    }
}
