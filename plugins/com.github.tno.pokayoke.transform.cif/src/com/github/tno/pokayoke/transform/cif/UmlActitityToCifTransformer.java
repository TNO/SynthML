
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
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
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
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
import org.eclipse.uml2.uml.FlowFinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;

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
        // Check if all elements in model have a CIF valid name.
        CifHelper.validateNames(model);

        // Extract the context class.
        Class contextClass = (Class)model.getMember("Context");

        // Transform the main activity in the contextClass.
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity && activity.getName().equals("main")) {
                Automaton aut = CifHelper.initializeAutomaton(model, activity, dataStore);
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
        for (ActivityNode node: new LinkedHashSet<>(activity.getNodes())) {
            if (node instanceof ForkNode) {
                // Check if there is only one incoming edge.
                Preconditions.checkArgument(node.getIncomings().size() == 1,
                        "Expected that fork node has only one incoming edge.");

                // Get the node event.
                Event nodeEvent = dataStore.getEvent(node.getName());

                Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                // Set guard for the incoming edge variable and update its value to false.
                ActivityEdge incomingEdge = node.getIncomings().get(0);
                CifHelper.setGuardAndUpdateForIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                // Set the outgoing edge variables to true.
                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    // Set the outgoing edge variable to true.
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
                    Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                    // Set guard for the incoming edge variable and update its value to false.
                    CifHelper.setGuardAndUpdateForIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                    // Set the outgoing edge variables to true.
                    ActivityEdge outgoingEdge = node.getOutgoings().get(0);
                    CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                }
            } else if (node instanceof JoinNode) {
                // Check if the join node only has one outgoing edge.
                Preconditions.checkArgument(node.getOutgoings().size() == 1,
                        "Expected that JoinNode has only one outgoing edge.");

                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());

                Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                // Set guards for the incoming edge variables and update their value to false.
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    CifHelper.setGuardAndUpdateForIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);
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

                    // Create CIF edge and edge event.
                    Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                    // Set guard for the incoming edge variable and update its value to false.
                    CifHelper.setGuardAndUpdateForIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);

                    // Extract the guard of the action from CIF text in the body of the action.
                    // If the action has at least one body, then parse the first body, which is assumed to be its guard.
                    List<Expression> guards = action.getBodies().stream().limit(1)
                            .map(b -> expressionParser.parseString(b, modelPath))
                            .map(b -> translator.translateExpression(b)).collect(Collectors.toList());

                    // Update the guard.
                    cifEdge.getGuards().addAll(guards);

                    // Extract the effect of the action from CIF text in the body of the action.
                    // Parse all bodies except the first one, all of which should be updates.
                    List<Assignment> effects = action.getBodies().stream().skip(1)
                            .map(b -> updateParser.parseString(b, modelPath)).map(b -> translator.translateUpdate(b))
                            .collect(Collectors.toList());

                    // Set the value of the variables.
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
                    Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                    // Add the edge variable for the incoming edge to the guard.
                    DiscVariable incomingEdgedVariable = dataStore.getVariable(node.getIncomings().get(0).getName());
                    cifEdge.getGuards().add(newDiscVariableExpression(null,
                            EcoreUtil.copy(incomingEdgedVariable.getType()), incomingEdgedVariable));

                    // Extract guard from the outgoing edge and add it to the automaton edge.
                    String guard = outgoingEdge.getGuard().getName();
                    Expression guardExpress = translator
                            .translateExpression(expressionParser.parseString(guard, modelPath));
                    cifEdge.getGuards().add(guardExpress);

                    // Set the incoming edge variable to false.
                    cifEdge.getUpdates().add(CifHelper.createAssignmentForEdgeVariable(incomingEdgedVariable, false));

                    // Set the outgoing edge variable to true.
                    CifHelper.updateOutgoingEdgeVariable(outgoingEdge.getName(), cifEdge, dataStore);
                }
            } else if (node instanceof ActivityFinalNode) {
                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                    // Extract the edge variable for the incoming edge.
                    DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());

                    // Add the guard to the edge.
                    cifEdge.getGuards()
                            .add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

                    // Set all edge variable to false. According to UML specification, the execution of activity is
                    // terminated when the first activity final node is fired: https://www.omg.org/spec/UML/2.5.1/PDF
                    // (page 430).
                    for (ActivityEdge edge: activity.getEdges()) {
                        // Set the incoming edge variable to false.
                        cifEdge.getUpdates().add(CifHelper
                                .createAssignmentForEdgeVariable(dataStore.getVariable(edge.getName()), false));
                    }
                }
            } else if (node instanceof FlowFinalNode) {
                // Get node event.
                Event nodeEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    Edge cifEdge = CifHelper.createCifEdgeAndEdgeEvent(location, nodeEvent);

                    // Set guard for the incoming edge variable and update its value to false. Only the execution of
                    // this control flow is terminated. Other flows in the activity can still continue.
                    CifHelper.setGuardAndUpdateForIncomingEdgeVariable(incomingEdge.getName(), cifEdge, dataStore);
                }
            }
        }
    }
}
