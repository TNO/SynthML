
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.Node;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Helper methods to translate Petri Net to Activity. */
public class PetriNet2ActivityHelper {
    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    private final Map<String, OpaqueAction> nameActionMap = new HashMap<>();

    public Activity initializeUMLActivity(Page page) {
        // Create a UML model and initialize it.
        Model model = UML_FACTORY.createModel();
        model.setName(page.getId());

        // Create a UML class and add it to the model.
        Class contextClass = model.createOwnedClass("Class", false);

        // Create an activity for the class.
        Activity activity = UML_FACTORY.createActivity();
        activity.setName(page.getId());

        // Add the activity as the owned member of the class.
        contextClass.getOwnedBehaviors().add(activity);

        return activity;
    }

    /**
     * Transforms all transitions into actions.
     *
     * @param page The Petri Net page that contains the transitions.
     * @param activity The activity that contains the transformed actions.
     */
    public void transformTransitions(Page page, Activity activity) {
        page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast)
                .forEach(transition -> transformTransition(transition.getId(), activity));
    }

    private OpaqueAction transformTransition(String name, Activity activity) {
        OpaqueAction action = UML_FACTORY.createOpaqueAction();
        action.setName(name);
        action.setActivity(activity);
        Verify.verify(nameActionMap.put(name, action) == null,
                "An existing action with the same name %s has already been added to the map.", name);

        return action;
    }

    /**
     * Transforms marked and final places into initial and final nodes.
     *
     * @param page The Petri Net page that contains the marked and final.
     * @param activity The activity that contains the transformed nodes.
     */
    public void transformMarkedAndFinalPlaces(Page page, Activity activity) {
        // Obtain the places.
        List<Place> places = page.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast).toList();

        places.stream().filter(place -> isMarkedPlace(place)).forEach(place -> transformMarkedPlace(place, activity));
        places.stream().filter(place -> isFinalPlace(place)).forEach(place -> transformFinalPlace(place, activity));
    }

    /**
     * Checks if the place is marked as the initial. The mark was added when transforming Petrify output to PNML.
     *
     * @param place The place to check.
     * @return {@code true} if the place is marked, otherwise {@code false}.
     */
    public static boolean isMarkedPlace(Place place) {
        if (place.getInitialMarking() != null) {
            int numInArcs = place.getInArcs().size();
            int numOutArcs = place.getOutArcs().size();
            Preconditions.checkArgument(numInArcs == 0,
                    "The place with initial marking has %s incoming arcs. It is expected this place to have no incoming arc.",
                    numInArcs);
            Preconditions.checkArgument(numOutArcs == 1,
                    "The place with initial marking has %s outgoing arcs. It is expected this place to have exactly one outgoing arc.",
                    numOutArcs);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms a marked place to an initial node. This method should only be called if {@link #isMarkedPlace(Place)}
     * returns true.
     *
     * @param place The marked place.
     * @param activity The activity that contains the transformed initial node.
     */
    private void transformMarkedPlace(Place place, Activity activity) {
        InitialNode initialNode = UML_FACTORY.createInitialNode();
        initialNode.setActivity(activity);
        initialNode.setName("InitialNode");

        Node targetNode = place.getOutArcs().get(0).getTarget();
        OpaqueAction targetAction = nameActionMap.get(targetNode.getId());

        createControlFlow(activity, initialNode, targetAction);
    }

    private static ControlFlow createControlFlow(String edgeName, Activity activity, ActivityNode source,
            ActivityNode target)
    {
        ControlFlow edge = UML_FACTORY.createControlFlow();
        edge.setName(edgeName);
        edge.setActivity(activity);
        edge.setSource(source);
        edge.setTarget(target);

        return edge;
    }

    private static ControlFlow createControlFlow(Activity activity, ActivityNode source, ActivityNode target) {
        return createControlFlow(concatenateNamesOfNodes(source, target), activity, source, target);
    }

    /**
     * Checks if the place is a final place. The final place was defined when transforming Petrify output to PNML.
     *
     * @param place The place to check.
     * @return {@code true} if the place is marked, otherwise {@code false}.
     */
    private static boolean isFinalPlace(Place place) {
        if (place.getId().equals("FinalPlace")) {
            int numInArcs = place.getInArcs().size();
            int numOutArcs = place.getOutArcs().size();
            Preconditions.checkArgument(numInArcs == 1,
                    "The place with 'FinalPlace' as ID has %s incoming arcs. It is expected this place to have exactly one incoming arc.",
                    numInArcs);
            Preconditions.checkArgument(numOutArcs == 0,
                    "The place with 'FinalPlace' as ID has %s outgoing arcs. It is expected this place to have no outgoing arc.",
                    numOutArcs);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms a final place to a final node. This method should only be called if {@link #isFinalPlace(Place)}
     * returns true.
     *
     * @param place The final place.
     * @param activity The activity that contains the transformed final node.
     */
    private void transformFinalPlace(Place place, Activity activity) {
        ActivityFinalNode finalNode = UML_FACTORY.createActivityFinalNode();
        finalNode.setActivity(activity);
        finalNode.setName("FinalNode");

        Node sourceNode = place.getInArcs().get(0).getSource();
        OpaqueAction sourceAction = nameActionMap.get(sourceNode.getId());

        createControlFlow(activity, sourceAction, finalNode);
    }

    /**
     * Transforms the place-based patterns that are detected based on the number of incoming and outgoing arcs of
     * places.
     *
     * @param page The Petri Net page that contains places.
     * @param activity The activity that contains the transformed activity objects.
     */
    public void transformPlaceBasedPatterns(Page page, Activity activity) {
        // Obtain the places that have at least one incoming and outgoing arcs (i.e., excluding the places for initial
        // and final nodes).
        List<Place> places = page.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast)
                .filter(place -> !place.getInArcs().isEmpty() && !place.getOutArcs().isEmpty()).toList();

        for (Place place: places) {
            ActivityNode source = transformMerge(place, activity);
            ActivityNode target = transformDecision(place, activity);
            createControlFlow(activity, source, target);
        }
    }

    private ActivityNode transformMerge(Place place, Activity activity) {
        // Obtain the actions translated from the sources of the incoming arcs.
        List<OpaqueAction> sourceActions = place.getInArcs().stream().map(o -> nameActionMap.get(o.getSource().getId()))
                .toList();

        if (sourceActions.size() == 1) {
            return sourceActions.get(0);
        } else {
            // Create a merge node.
            MergeNode merge = UML_FACTORY.createMergeNode();
            merge.setName("Merge__" + place.getId());
            merge.setActivity(activity);

            // Connect the merge node to the actions translated from the sources of the incoming arcs.
            sourceActions.stream().forEach(sourceAction -> createControlFlow(activity, sourceAction, merge));
            return merge;
        }
    }

    private ActivityNode transformDecision(Place place, Activity activity) {
        // Obtain the actions translated from the target of the outgoing arcs.
        List<OpaqueAction> targetActions = place.getOutArcs().stream()
                .map(o -> nameActionMap.get(o.getTarget().getId())).toList();

        if (targetActions.size() == 1) {
            return targetActions.get(0);
        } else {
            // Create a decision node.
            DecisionNode decision = UML_FACTORY.createDecisionNode();
            decision.setName("Decision__" + place.getId());
            decision.setActivity(activity);

            targetActions.stream()
                    .forEach(targetAction -> connectDecisionNode2TargetAction(decision, activity, targetAction));
            return decision;
        }
    }

    /**
     * Connect the decision node to the action translated from the target of the outgoing arcs and set the guard of the
     * edges to true.
     *
     * @param decision The decision node.
     * @param activity The activity that contains the decision node.
     * @param targetAction The target action to connect.
     */
    private static void connectDecisionNode2TargetAction(DecisionNode decision, Activity activity,
            OpaqueAction targetAction)
    {
        ControlFlow controlFlow = createControlFlow(activity, decision, targetAction);
        LiteralBoolean guard = UML_FACTORY.createLiteralBoolean();
        guard.setValue(true);
        controlFlow.setGuard(guard);
    }

    private static String concatenateNamesOfNodes(ActivityNode left, ActivityNode right) {
        return left.getName() + "__to__" + right.getName();
    }

    /**
     * Transforms the transition-based patterns that are detected based on the number of incoming and outgoing arcs of
     * transitions.
     *
     * @param page The Petri Net page that contains the transitions.
     * @param activity The activity that contains the transformed activity objects.
     */
    public void transformTransitionBasedPatterns(Page page, Activity activity) {
        // Obtain the transitions.
        List<Transition> transitions = page.getObjects().stream().filter(Transition.class::isInstance)
                .map(Transition.class::cast).toList();
        transitions.stream().filter(PetriNet2ActivityHelper::hasMultiOutArcs)
                .forEach(transition -> transformFork(transition, activity));
        transitions.stream().filter(PetriNet2ActivityHelper::hasMultiInArcs)
                .forEach(transition -> transformJoin(transition, activity));
    }

    private static boolean hasMultiInArcs(Transition transition) {
        return transition.getInArcs().size() > 1;
    }

    private static boolean hasMultiOutArcs(Transition transition) {
        return transition.getOutArcs().size() > 1;
    }

    private void transformFork(Transition transition, Activity activity) {
        // Create a fork node.
        ForkNode fork = UML_FACTORY.createForkNode();
        fork.setActivity(activity);
        fork.setName("Fork");

        // Obtain the action translated from the transition.
        OpaqueAction action = nameActionMap.get(transition.getId());

        // Obtain the outgoing edges.
        List<ActivityEdge> outgoingEdges = action.getOutgoings();

        // Reset the source of the outgoing edges to the fork node.
        for (ActivityEdge outgoingEdge: new ArrayList<>(outgoingEdges)) {
            outgoingEdge.setSource(fork);
            outgoingEdge.setName(concatenateNamesOfNodes(fork, outgoingEdge.getTarget()));
        }

        // Connect the action to the fork node.
        createControlFlow(activity, action, fork);
    }

    private void transformJoin(Transition transition, Activity activity) {
        // Create a join node.
        JoinNode join = UML_FACTORY.createJoinNode();
        join.setActivity(activity);
        join.setName("Join");

        // Obtain the action translated from the transition.
        OpaqueAction action = nameActionMap.get(transition.getId());

        // Obtain the incoming edges of the action.
        List<ActivityEdge> incomingEdges = action.getIncomings();

        // Reset the target of the incoming edges to the join node.
        for (ActivityEdge incomingEdge: new ArrayList<>(incomingEdges)) {
            incomingEdge.setTarget(join);
            incomingEdge.setName(concatenateNamesOfNodes(incomingEdge.getSource(), join));
        }

        // Connect the join node and the action.
        createControlFlow(activity, join, action);
    }

    public void renameDuplicateActions() {
        nameActionMap.values().stream().filter(action -> action.getName().contains("/"))
                .forEach(action -> action.setName(action.getName().split("/")[0]));
    }
}
