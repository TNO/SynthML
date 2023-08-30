
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdge;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdgeEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEventExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;

import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Preconditions;

/** Transforms flattened UML model into CIF model. */
public class CIFTransformer {
    private final Model model;

    private final CifUpdateParser updateParser = new CifUpdateParser();

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final Specification spec;

    private final String modelPath;

    private final DataStore dataStore;

    private final CifToCifTranslator translator;

    public CIFTransformer(Model model, Specification spec, String modelPath) {
        this.model = model;
        this.spec = spec;
        this.modelPath = modelPath;
        this.dataStore = new DataStore();
        this.translator = new CifToCifTranslator(this.dataStore);
    }

    public static void transformFile(String sourcePath, String targetPath) {
        Model model = FileHelper.loadModel(sourcePath);
        Specification spec = newSpecification();
        new CIFTransformer(model, spec, sourcePath).transformModel();
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(spec, targetPath, Paths.get(targetPath).getParent().toString());
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    public void transformModel() {
        // Extract activities.
        Class contextClass = (Class)model.getMember("Context");
        // Transform the main activity diagram in the contextClass.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity && activity.getName().equals("main")) {
                Automaton aut = CIFHelper.initializeAutomaton(model, activity, dataStore);
                aut.setName(model.getName());
                spec.getComponents().add(aut);
                transformActivityDiagram(activity, aut);
            }
        }
    }

    public void transformActivityDiagram(Activity activity, Automaton aut) {
        // Create the only location (i.e., the flower state) and add it to the automaton.
        Location location = newLocation();
        location.getInitials().add(CifValueUtils.makeTrue());
        location.getMarkeds().add(CifValueUtils.makeTrue());
        aut.getLocations().add(location);

        // Iterate through all the activity node for transformation.
        for (ActivityNode node: new LinkedHashSet<>(activity.getNodes())) {
            if (node instanceof InitialNode) {
                // Get the automaton event.
                Event autEvent = dataStore.getEvent(node.getName());

                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    // Get the edge variable.
                    DiscVariable edgeVariable = dataStore.getVariable(outgoingEdge.getName());

                    // Define a new edge and add it to the location.
                    Edge autEdge = newEdge();
                    location.getEdges().add(autEdge);

                    // Define a new edge event.
                    EdgeEvent edgeEvent = newEdgeEvent();
                    edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                    autEdge.getEvents().add(edgeEvent);

                    // Set the edge variable for the outgoing edge to true.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgeVariable, true));
                }
            }

            if (node instanceof ForkNode) {
                // Check if there is only one incoming edge.
                Preconditions.checkArgument(node.getIncomings().size() == 1,
                        "Expected that fork node has only one incoming edge.");
                // Get the automaton event.
                Event autEvent = dataStore.getEvent(node.getName());

                // Define a new edge and add it to the location.
                Edge autEdge = newEdge();
                location.getEdges().add(autEdge);

                // Define a new edge event add it to the edge.
                EdgeEvent edgeEvent = newEdgeEvent();
                edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                autEdge.getEvents().add(edgeEvent);

                // Extract the edge variable for the incoming edge.
                ActivityEdge incomingEdge = node.getIncomings().get(0);
                DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());

                // Add the evaluation of the edge variable as the guard of the edge.
                autEdge.getGuards()
                        .add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

                // Set the incoming edge variable to false.
                autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgeVariable, false));

                // Set the outgoing edge variables to true.
                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    DiscVariable variable = dataStore.getVariable(outgoingEdge.getName());
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(variable, true));
                }
            }

            if (node instanceof MergeNode) {
                // Check if the merge node only has one outgoing edge.
                Preconditions.checkArgument(node.getOutgoings().size() == 1,
                        "Expected that merge node has only one outgoing edge.");
                // Get the automaton event.
                Event autEvent = dataStore.getEvent(node.getName());

                // For each incoming edge, an automaton edge and its info (i.e., guard and update) is transformed.
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Define a new edge and add it to the location.
                    Edge autEdge = newEdge();
                    location.getEdges().add(autEdge);

                    // Define a new edge event and add it to the edge.
                    EdgeEvent edgeEvent = newEdgeEvent();
                    edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                    autEdge.getEvents().add(edgeEvent);

                    // Extract the edge variable for the incoming edge.
                    DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());

                    // Add the evaluation of the edge variable as the guard of the edge.
                    autEdge.getGuards()
                            .add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

                    // Set the incoming edge variable to false.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgeVariable, false));

                    // Set the outgoing edge variables to true.
                    ActivityEdge outgoingEdge = node.getOutgoings().get(0);
                    DiscVariable variable = dataStore.getVariable(outgoingEdge.getName());
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(variable, true));
                }
            }

            if (node instanceof JoinNode) {
                // Check if the join node only has one outgoing edge.
                Preconditions.checkArgument(node.getOutgoings().size() == 1,
                        "Expected that JoinNode has only one outgoing edge.");

                // Get automaton event.
                Event autEvent = dataStore.getEvent(node.getName());

                // Define a new edge and add it to the location.
                Edge autEdge = newEdge();
                location.getEdges().add(autEdge);

                // Define a new edge event.
                EdgeEvent edgeEvent = newEdgeEvent();
                edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                autEdge.getEvents().add(edgeEvent);

                // Add the evaluation of all the edge variables of the incoming edges to guard and set the value of them to false.
                List<Expression> guards = new ArrayList<>();
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Extract the guard.
                    DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());
                    guards.add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));
                    // Set the value of the incoming edge variable to false.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgeVariable, false));
                }
                // Set the guards with conjunction operator.
                autEdge.getGuards().add(CifValueUtils.createConjunction(guards));

                // Set the outgoing edge variables to true
                ActivityEdge outgoingEdge = node.getOutgoings().get(0);
                DiscVariable variable = dataStore.getVariable(outgoingEdge.getName());
                autEdge.getUpdates().add(createAssignmentForEdgeVariable(variable, true));
            }

            if (node instanceof OpaqueAction action) {
                // fUML requires that an action with input pins have an offer on at least one of the pins before it
                // fires: from https://www.omg.org/spec/FUML/1.5/PDF (page 175).
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Get automaton event.
                    Event autEvent = dataStore.getEvent(node.getName());

                    // Define a new edge and add it to the location.
                    Edge autEdge = newEdge();
                    location.getEdges().add(autEdge);

                    // Define a new edge event.
                    EdgeEvent edgeEvent = newEdgeEvent();
                    edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                    autEdge.getEvents().add(edgeEvent);

                    // Extract the guard of the action from CIF text in the body of the action.
                    // If the action has at least one body, then parse the first body, which is assumed to be its guard.
                    List<Expression> guards = action.getBodies().stream().limit(1).map(b -> parseExpression(b))
                            .map(b -> translator.translateExpression(b)).collect(Collectors.toList());
                    autEdge.getGuards().addAll(guards);

                    // Extract the effect of the action from CIF text in the body of the action.
                    // Parse all bodies except the first one, all of which should be updates.
                    List<Assignment> effects = action.getBodies().stream().skip(1).map(b -> parseUpdate(b))
                            .map(b -> translator.translateUpdate(b)).collect(Collectors.toList());
                    autEdge.getUpdates().addAll(effects);

                    // Add edge variables (that correspond to the incoming edges) to the guard and set the guard.
                    DiscVariable edgedVariable = dataStore.getVariable(incomingEdge.getName());
                    autEdge.getGuards().add(
                            newDiscVariableExpression(null, EcoreUtil.copy(edgedVariable.getType()), edgedVariable));

                    // Set the incoming edge variable to false.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgedVariable, false));

                    // Set the edge variables for the outgoing edges to true. If there are multiple outgoing edges from
                    // a action, a thread split and each outgoing thread executes concurrently, i.e., implicit fork:
                    // https://www.omg.org/spec/FUML/1.5/PDF (pages 175 and 225).
                    for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                        DiscVariable variable = dataStore.getVariable(outgoingEdge.getName());
                        autEdge.getUpdates().add(createAssignmentForEdgeVariable(variable, true));
                    }
                }
            }

            if (node instanceof DecisionNode) {
                // Check if the decision node only has one incoming edge.
                Preconditions.checkArgument(node.getIncomings().size() == 1,
                        "Expected that decision node has only one outgoing edge.");

                // Get automaton event.
                Event autEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                    // Define a new edge and add it to the location.
                    Edge autEdge = newEdge();
                    location.getEdges().add(autEdge);

                    // Define a new edge event.
                    EdgeEvent edgeEvent = newEdgeEvent();
                    edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                    autEdge.getEvents().add(edgeEvent);

                    // Add the edge variable for the incoming edge to the guard.
                    DiscVariable incomingEdgedVariable = dataStore.getVariable(node.getIncomings().get(0).getName());
                    autEdge.getGuards().add(newDiscVariableExpression(null,
                            EcoreUtil.copy(incomingEdgedVariable.getType()), incomingEdgedVariable));

                    // Extract guard from the outgoing edge and add it to the automaton edge.
                    String guard = outgoingEdge.getGuard().getName();
                    Expression guardExpress = translator.translateExpression(parseExpression(guard));
                    autEdge.getGuards().add(guardExpress);

                    // Set the incoming edge variable to false.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(incomingEdgedVariable, false));

                    // Set the outgoing edge variable to true.
                    DiscVariable outgoingEdgedVariable = dataStore.getVariable(outgoingEdge.getName());
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(outgoingEdgedVariable, true));
                }
            }
            if (node instanceof ActivityFinalNode) {
                // Get automaton event.
                Event autEvent = dataStore.getEvent(node.getName());
                for (ActivityEdge incomingEdge: node.getIncomings()) {
                    // Define a new edge and add it to the location.
                    Edge autEdge = newEdge();
                    location.getEdges().add(autEdge);

                    // Define a new edge event.
                    EdgeEvent edgeEvent = newEdgeEvent();
                    edgeEvent.setEvent(newEventExpression(autEvent, null, null));
                    autEdge.getEvents().add(edgeEvent);

                    // Extract the edge variable for the incoming edge.
                    DiscVariable edgeVariable = dataStore.getVariable(incomingEdge.getName());

                    // Add the guard to the edge.
                    autEdge.getGuards()
                            .add(newDiscVariableExpression(null, EcoreUtil.copy(edgeVariable.getType()), edgeVariable));

                    // Set the incoming edge variable to false.
                    autEdge.getUpdates().add(createAssignmentForEdgeVariable(edgeVariable, false));
                }
            }
        }
    }

    public Assignment createAssignmentForEdgeVariable(DiscVariable variable, Boolean value) {
        DiscVariableExpression addressableVar = newDiscVariableExpression(null, newBoolType(), variable);
        Assignment assign = newAssignment(addressableVar, null, newBoolExpression(null, newBoolType(), value));
        return assign;
    }

    private AExpression parseExpression(String expression) {
        return expressionParser.parseString(expression, modelPath);
    }

    private AUpdate parseUpdate(String update) {
        return updateParser.parseString(update, modelPath);
    }
}
