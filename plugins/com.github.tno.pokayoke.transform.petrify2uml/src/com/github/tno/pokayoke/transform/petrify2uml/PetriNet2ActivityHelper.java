
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.HashMap;
import java.util.List;

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
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Node;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Helper methods to translate Petri Net to Activity. */
public class PetriNet2ActivityHelper {
    private static HashMap<String, NamedElement> nameObjectMapping = new HashMap<>();

    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    private PetriNet2ActivityHelper() {
    }

    public static Activity initializeUMLActivity(Page page) {
        // Create a UML model and initialize it

        Model model = UML_FACTORY.createModel();
        model.setName(page.getId());

        // Create a UML class and add it to the model
        Class contextClass = model.createOwnedClass("Class", false);

        // Create an activity for the class
        Activity activity = UML_FACTORY.createActivity();
        activity.setName(page.getId());

        // Add activity as the owned member of the class.
        contextClass.getOwnedBehaviors().add(activity);
        return activity;
    }

    public static OpaqueAction createOpacheAction(String name, Activity activity) {
        OpaqueAction action = UML_FACTORY.createOpaqueAction();
        action.setName(name);
        action.setActivity(activity);
        nameObjectMapping.put(name, action);
        return action;
    }

    public static boolean isMarkedPlace(Place place) {
        return place.getInitialMarking() != null;
    }

    public static void transformMarkedPlace(Activity activity, Place place) {
        InitialNode initialNode = UML_FACTORY.createInitialNode();
        initialNode.setActivity(activity);
        Preconditions.checkArgument(place.getOutArcs().size() == 1,
                "Expected the initial place to have exactly one outgoing arc.");
        Arc arc = place.getOutArcs().get(0);
        String targetName = arc.getTarget().getId();
        OpaqueAction targetAction = (OpaqueAction)nameObjectMapping.get(targetName);

        createControlFlow(place.getId(), activity, initialNode, targetAction);
    }

    public static ControlFlow createControlFlow(String edgeName, Activity activity, ActivityNode source,
            ActivityNode target)
    {
        ControlFlow edge = UML_FACTORY.createControlFlow();
        edge.setName(edgeName);
        edge.setActivity(activity);
        edge.setSource(source);
        edge.setTarget(target);
        nameObjectMapping.put(edgeName, edge);
        return edge;
    }

    public static boolean isFinalPlace(Place place) {
        return place.getId().equals("FinalPlace");
    }

    public static void transformFinalPlace(Activity activity, Place place) {
        ActivityFinalNode finalNode = UML_FACTORY.createActivityFinalNode();
        finalNode.setActivity(activity);
        Preconditions.checkArgument(place.getInArcs().size() == 1,
                "Expected the final place to have exactly one incoming arc.");
        Arc arc = place.getInArcs().get(0);
        String sourceName = arc.getSource().getId();
        OpaqueAction sourceAction = (OpaqueAction)nameObjectMapping.get(sourceName);

        createControlFlow(place.getId(), activity, sourceAction, finalNode);
    }

    public static boolean isOneToOnePlace(Place place) {
        return place.getInArcs().size() == 1 && place.getOutArcs().size() == 1;
    }

    public static void transformOneToOnePlace(Place place, Activity activity) {
        Node sourceNode = place.getInArcs().get(0).getSource();
        OpaqueAction sourceAction = (OpaqueAction)nameObjectMapping.get(sourceNode.getId());
        Node targetNode = place.getOutArcs().get(0).getTarget();
        OpaqueAction targetAction = (OpaqueAction)nameObjectMapping.get(targetNode.getId());

        createControlFlow(place.getId(), activity, sourceAction, targetAction);
    }

    public static boolean isMergePattern(Place place) {
        return place.getInArcs().size() > 1 && place.getOutArcs().size() == 1;
    }

    public static void transformMergePattern(Place place, Activity activity) {
        // Obtain the actions translated from the sources of the incoming arcs.
        List<NamedElement> sourceActions = place.getInArcs().stream()
                .map(o -> nameObjectMapping.get(o.getSource().getId())).toList();

        // Obtain the action translated from the target of the outgoing arc.
        NamedElement targetAction = place.getOutArcs().stream().map(o -> nameObjectMapping.get(o.getTarget().getId()))
                .findFirst().get();

        // create a merge node.
        MergeNode merge = UML_FACTORY.createMergeNode();
        merge.setName(place.getId());
        merge.setActivity(activity);

        // Connect the merge node to the actions translated from the sources of the incoming arcs.
        for (NamedElement action: sourceActions) {
            createControlFlow(action.getName() + "__to__" + "merge", activity, (ActivityNode)action, merge);
        }

        // Connect the merge node to the action translated from the target of the outgoing arc.
        createControlFlow(merge.getName() + "__to__" + targetAction.getName(), activity, merge,
                (ActivityNode)targetAction);
    }

    public static boolean isDecisionPattern(Place place) {
        return place.getInArcs().size() == 1 && place.getOutArcs().size() > 1;
    }

    public static void transformDecisionPattern(Place place, Activity activity) {
        // Obtain the action translated from the source of the incoming arc.
        NamedElement sourceAction = place.getInArcs().stream().map(o -> nameObjectMapping.get(o.getSource().getId()))
                .findFirst().get();

        // Obtain the actions translated from the target of the outgoing arcs.
        List<NamedElement> targetActions = place.getOutArcs().stream()
                .map(o -> nameObjectMapping.get(o.getTarget().getId())).toList();

        // Create a decision node.
        DecisionNode decision = UML_FACTORY.createDecisionNode();
        decision.setName(place.getId());
        decision.setActivity(activity);

        // Connect decision node and the source action.
        createControlFlow(sourceAction.getName() + "__to__" + decision.getName(), activity, (ActivityNode)sourceAction,
                decision);

        // Connect the decision node to the action translated from the targets of the outgoing arcs and set the guard of
        // the edges to true.
        for (NamedElement action: targetActions) {
            ControlFlow controlFlow = createControlFlow(decision.getName() + "__to__" + action.getName(), activity,
                    decision, (ActivityNode)action);
            LiteralBoolean guard = UML_FACTORY.createLiteralBoolean();
            guard.setValue(true);
            controlFlow.setGuard(guard);
        }
    }

    public static boolean isMergeDecisionPattern(Place place) {
        return place.getInArcs().size() > 1 && place.getOutArcs().size() > 1;
    }

    public static void transformMergeDecisionPattern(Place place, Activity activity) {
        // Obtain the actions translated from the sources of the incoming arcs.
        List<NamedElement> sourceActions = place.getInArcs().stream()
                .map(o -> nameObjectMapping.get(o.getSource().getId())).toList();

        // Obtain the actions translated from the target of the outgoing arcs.
        List<NamedElement> targetActions = place.getOutArcs().stream()
                .map(o -> nameObjectMapping.get(o.getTarget().getId())).toList();

        // create a merge node.
        MergeNode merge = UML_FACTORY.createMergeNode();
        merge.setName(place.getId());
        merge.setActivity(activity);

        // Connect the merge node to the actions translated from the sources of the incoming arcs.
        for (NamedElement action: sourceActions) {
            createControlFlow(action.getName() + "__to__" + "merge", activity, (ActivityNode)action, merge);
        }

        // Create a decision node.
        DecisionNode decision = UML_FACTORY.createDecisionNode();
        decision.setName(place.getId());
        decision.setActivity(activity);

        // Connect merge and decision nodes.
        createControlFlow(merge.getName() + "__to__" + decision.getName(), activity, merge, decision);

        // Connect the decision node to the action translated from the targets of the outgoing arcs and set the guard of
        // the edges to true.
        for (NamedElement action: targetActions) {
            ControlFlow controlFlow = createControlFlow(decision.getName() + "__to__" + action.getName(), activity,
                    decision, (ActivityNode)action);
            LiteralBoolean guard = UML_FACTORY.createLiteralBoolean();
            guard.setValue(true);
            controlFlow.setGuard(guard);
        }
    }

    public static boolean isForkPattern(Transition transition) {
        return transition.getInArcs().size() == 1 && transition.getOutArcs().size() > 1;
    }

    public static void transformForkPattern(Transition transition, Activity activity) {
        // Obtain the action translated from the transition.
        OpaqueAction action = (OpaqueAction)nameObjectMapping.get(transition.getId());

        // Obtain the outgoing edges.
        List<NamedElement> outgoingEdges = transition.getOutArcs().stream()
                .map(outArc -> nameObjectMapping.get(outArc.getTarget().getId())).toList();

        // create a fork node.
        ForkNode fork = UML_FACTORY.createForkNode();
        fork.setActivity(activity);

        // Connect the action to the fork node.
        createControlFlow(action.getName() + "__to__" + "fork", activity, action, fork);

        // Reset the source of the outgoing edges to the fork node.
        for (NamedElement outgoingEdge: outgoingEdges) {
            ((ActivityEdge)outgoingEdge).setSource(fork);
        }
    }

    public static boolean isJoinPattern(Transition transition) {
        return transition.getInArcs().size() > 1 && transition.getOutArcs().size() == 1;
    }

    public static void transformJoinPattern(Transition transition, Activity activity) {
        // Obtain the action translated from the transition.
        OpaqueAction action = (OpaqueAction)nameObjectMapping.get(transition.getId());

        // Obtain the incoming edges of the action.
        List<NamedElement> incomingEdges = transition.getInArcs().stream()
                .map(o -> nameObjectMapping.get(o.getSource().getId())).toList();

        // Create a join node.
        JoinNode join = UML_FACTORY.createJoinNode();
        join.setActivity(activity);

        // Connect the join node and the action.
        createControlFlow("join" + "__to__" + action.getName(), activity, join, action);

        // Reset the target of the incoming edges to the join node.
        for (NamedElement incomingEdge: incomingEdges) {
            ((ActivityEdge)incomingEdge).setTarget(join);
        }
    }

    public static void renameDuplicateActions(Activity activity) {
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof OpaqueAction action) {
                String name = action.getName();
                if (name.contains("/")) {
                    action.setName(name.split("/")[0]);
                }
            }
        }
    }
}
