
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

import fr.lip6.move.pnml.ptnet.Node;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PnObject;
import fr.lip6.move.pnml.ptnet.Transition;

/** Helper methods to translate Petri Net to Activity. */
public class PetriNet2ActivityHelper {
    private static Map<String, OpaqueAction> nameActionMap = new HashMap<>();

    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    private PetriNet2ActivityHelper() {
    }

    public static Activity initializeUMLActivity(Page page) {
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

    public static void transformTransitions(Page page, Activity activity) {
        page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast)
                .forEach(transition -> transformTransition(transition.getId(), activity));
    }

    private static OpaqueAction transformTransition(String name, Activity activity) {
        OpaqueAction action = UML_FACTORY.createOpaqueAction();
        action.setName(name);
        action.setActivity(activity);
        nameActionMap.put(name, action);
        return action;
    }

    public static void transformMarkedAndFinalPlaces(Page page, Activity activity) {
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.isMarkedPlace(place)) {
                    PetriNet2ActivityHelper.transformMarkedPlace(activity, place);
                } else if (PetriNet2ActivityHelper.isFinalPlace(place)) {
                    PetriNet2ActivityHelper.transformFinalPlace(activity, place);
                }
            }
        }
    }

    /**
     * Checks if the place is marked as the initial. The mark was added when transforming petrify output to PNML.
     *
     * @param place The place to check.
     * @return {@code true} if the place is marked, otherwise {@code false}.
     */
    private static boolean isMarkedPlace(Place place) {
        if (place.getInitialMarking() != null) {
            int numInArcs = place.getInArcs().size();
            int numOutArcs = place.getOutArcs().size();
            Preconditions.checkArgument(numInArcs == 0,
                    "The marked place has %s imcoming arcs. It is expected the marked place to have no incoming arc.",
                    numInArcs);
            Preconditions.checkArgument(numOutArcs == 1,
                    "The marked place has %s outgoing arcs. It is expected the marked place to have exactly one outgoing arc.",
                    numOutArcs);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms a marked place to an initial node. This method should only be called after
     * {@link #isMarkedPlace(Place)} is called and returns true.
     *
     * @param activity The activity that the initial node belongs to.
     * @param place The marked place.
     */
    private static void transformMarkedPlace(Activity activity, Place place) {
        InitialNode initialNode = UML_FACTORY.createInitialNode();
        initialNode.setActivity(activity);

        Node targetNode = place.getOutArcs().get(0).getTarget();
        OpaqueAction targetAction = nameActionMap.get(targetNode.getId());

        createControlFlow(place.getId(), activity, initialNode, targetAction);
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

    /**
     * Checks if the place is a final place. The final place was defined when transforming petrify output to PNML.
     *
     * @param place The place to check.
     * @return {@code true} if the place is marked, otherwise {@code false}.
     */
    private static boolean isFinalPlace(Place place) {
        if (place.getId().equals("FinalPlace")) {
            int numInArcs = place.getInArcs().size();
            int numOutArcs = place.getOutArcs().size();
            Preconditions.checkArgument(numInArcs == 1,
                    "The final place has %s imcoming arcs. It is expected the final place to have exactly one incoming arc.",
                    numInArcs);
            Preconditions.checkArgument(numOutArcs == 0,
                    "The final place has %s outgoing arcs. It is expected the final place to have no outgoing arc.",
                    numOutArcs);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Transforms a final place to a final node. This method should only be called after {@link #isFinalPlace(Place)} is
     * called and returns true.
     *
     * @param activity The activity that the final node belongs to.
     * @param place The final place.
     */
    private static void transformFinalPlace(Activity activity, Place place) {
        ActivityFinalNode finalNode = UML_FACTORY.createActivityFinalNode();
        finalNode.setActivity(activity);

        Node sourceNode = place.getInArcs().get(0).getSource();
        OpaqueAction sourceAction = nameActionMap.get(sourceNode.getId());

        createControlFlow(place.getId(), activity, sourceAction, finalNode);
    }

    public static void transformPlaceBasedPatterns(Page page, Activity activity) {
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.hasSingleInArc(place)) {
                    if (PetriNet2ActivityHelper.hasSingleOutArc(place)) {
                        PetriNet2ActivityHelper.transformOneToOnePattern(place, activity);
                    } else if (PetriNet2ActivityHelper.hasMultiOutArcs(place)) {
                        PetriNet2ActivityHelper.transformDecisionPattern(place, activity);
                    }
                } else if (PetriNet2ActivityHelper.hasMultiInArcs(place)) {
                    if (PetriNet2ActivityHelper.hasSingleOutArc(place)) {
                        PetriNet2ActivityHelper.transformMergePattern(place, activity);
                    } else if (PetriNet2ActivityHelper.hasMultiOutArcs(place)) {
                        PetriNet2ActivityHelper.transformMergeDecisionPattern(place, activity);
                    }
                }
            }
        }
    }

    private static boolean hasSingleInArc(Place place) {
        return place.getInArcs().size() == 1;
    }

    private static boolean hasSingleOutArc(Place place) {
        return place.getOutArcs().size() == 1;
    }

    private static boolean hasMultiInArcs(Place place) {
        return place.getInArcs().size() > 1;
    }

    private static boolean hasMultiOutArcs(Place place) {
        return place.getOutArcs().size() > 1;
    }

    private static void transformOneToOnePattern(Place place, Activity activity) {
        Node sourceNode = place.getInArcs().get(0).getSource();
        OpaqueAction sourceAction = nameActionMap.get(sourceNode.getId());
        Node targetNode = place.getOutArcs().get(0).getTarget();
        OpaqueAction targetAction = nameActionMap.get(targetNode.getId());

        createControlFlow(place.getId(), activity, sourceAction, targetAction);
    }

    private static void transformMergePattern(Place place, Activity activity) {
        // Obtain the actions translated from the sources of the incoming arcs.
        List<OpaqueAction> sourceActions = place.getInArcs().stream().map(o -> nameActionMap.get(o.getSource().getId()))
                .toList();

        // Obtain the action translated from the target of the outgoing arc.
        Node targetNode = place.getOutArcs().get(0).getTarget();
        OpaqueAction targetAction = nameActionMap.get(targetNode.getId());

        // Create a merge node.
        MergeNode merge = UML_FACTORY.createMergeNode();
        merge.setName(place.getId());
        merge.setActivity(activity);

        // Connect the merge node to the actions translated from the sources of the incoming arcs.
        sourceActions.stream()
                .forEach(sourceAction -> createControlFlow(sourceAction.getName() + "__to__" + merge.getName(),
                        activity, sourceAction, merge));

        // Connect the merge node to the action translated from the target of the outgoing arc.
        createControlFlow(merge.getName() + "__to__" + targetAction.getName(), activity, merge, targetAction);
    }

    private static void transformDecisionPattern(Place place, Activity activity) {
        // Obtain the action translated from the source of the incoming arc.
        Node sourceNode = place.getInArcs().get(0).getSource();
        OpaqueAction sourceAction = nameActionMap.get(sourceNode.getId());

        // Obtain the actions translated from the target of the outgoing arcs.
        List<OpaqueAction> targetActions = place.getOutArcs().stream()
                .map(o -> nameActionMap.get(o.getTarget().getId())).toList();

        // Create a decision node.
        DecisionNode decision = UML_FACTORY.createDecisionNode();
        decision.setName(place.getId());
        decision.setActivity(activity);

        // Connect decision node and the source action.
        createControlFlow(sourceAction.getName() + "__to__" + decision.getName(), activity, sourceAction, decision);

        // Connect the decision node to the actions translated from the targets of the outgoing arcs and set the guard
        // of the edges to true.
        for (OpaqueAction action: targetActions) {
            ControlFlow controlFlow = createControlFlow(decision.getName() + "__to__" + action.getName(), activity,
                    decision, action);
            LiteralBoolean guard = UML_FACTORY.createLiteralBoolean();
            guard.setValue(true);
            controlFlow.setGuard(guard);
        }
    }

    private static void transformMergeDecisionPattern(Place place, Activity activity) {
        // Obtain the actions translated from the sources of the incoming arcs.
        List<OpaqueAction> sourceActions = place.getInArcs().stream().map(o -> nameActionMap.get(o.getSource().getId()))
                .toList();

        // Obtain the actions translated from the target of the outgoing arcs.
        List<OpaqueAction> targetActions = place.getOutArcs().stream()
                .map(o -> nameActionMap.get(o.getTarget().getId())).toList();

        // Create a merge node.
        MergeNode merge = UML_FACTORY.createMergeNode();
        merge.setName(place.getId());
        merge.setActivity(activity);

        // Connect the merge node to the actions translated from the sources of the incoming arcs.
        sourceActions.stream()
                .forEach(sourceAction -> createControlFlow(sourceAction.getName() + "__to__" + merge.getName(),
                        activity, sourceAction, merge));

        // Create a decision node.
        DecisionNode decision = UML_FACTORY.createDecisionNode();
        decision.setName(place.getId());
        decision.setActivity(activity);

        // Connect merge and decision nodes.
        createControlFlow(merge.getName() + "__to__" + decision.getName(), activity, merge, decision);

        // Connect the decision node to the actions translated from the targets of the outgoing arcs and set the guard
        // of the edges to true.
        for (OpaqueAction action: targetActions) {
            ControlFlow controlFlow = createControlFlow(decision.getName() + "__to__" + action.getName(), activity,
                    decision, action);
            LiteralBoolean guard = UML_FACTORY.createLiteralBoolean();
            guard.setValue(true);
            controlFlow.setGuard(guard);
        }
    }

    public static void transformTransitionBasedPatterns(Page page, Activity activity) {
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Transition transition) {
                if (PetriNet2ActivityHelper.hasMultiOutArcs(transition)) {
                    PetriNet2ActivityHelper.transformForkPattern(transition, activity);
                }
                if (PetriNet2ActivityHelper.hasMultiInArcs(transition)) {
                    PetriNet2ActivityHelper.transformJoinPattern(transition, activity);
                }
            }
        }
    }

    private static boolean hasMultiInArcs(Transition transition) {
        return transition.getInArcs().size() > 1;
    }

    private static boolean hasMultiOutArcs(Transition transition) {
        return transition.getOutArcs().size() > 1;
    }

    private static void transformForkPattern(Transition transition, Activity activity) {
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
            outgoingEdge.setName(fork.getName() + "__to__" + outgoingEdge.getTarget().getName());
        }

        // Connect the action to the fork node.
        createControlFlow(action.getName() + "__to__" + fork.getName(), activity, action, fork);
    }

    private static void transformJoinPattern(Transition transition, Activity activity) {
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
            incomingEdge.setName(incomingEdge.getSource().getName() + "__to__" + join.getName());
        }

        // Connect the join node and the action.
        createControlFlow(join.getName() + "__to__" + action.getName(), activity, join, action);
    }

    public static void renameDuplicateActions() {
        nameActionMap.values().stream().filter(action -> action.getName().contains("/"))
                .forEach(action -> action.setName(action.getName().split("/")[0]));
    }
}
