
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.framework.utils.exception.ImportException;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;
import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Node;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PnObject;
import fr.lip6.move.pnml.ptnet.Transition;

/** Translates Petri Nets in PNML to activities in UML. */
public class PNML2UMLTranslator {
    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    /** The abstract UML activity to translate the Petri Net to. */
    private final Activity activity;

    /** The mapping from Petri Net arcs to corresponding translated UML control flows. */
    private final Map<Arc, ControlFlow> arcMapping = new LinkedHashMap<>();

    /** The mapping from Petri Net places to corresponding translated UML control flows. */
    private final Map<Place, ControlFlow> placeMapping = new LinkedHashMap<>();

    /** The mapping from Petri Net transitions to corresponding translated UML activity nodes. */
    private final Map<Transition, ActivityNode> transitionMapping = new LinkedHashMap<>();

    /** The mapping from UML activity nodes to corresponding Petri Net nodes. */
    private final Map<ActivityNode, Node> nodeMapping = new LinkedHashMap<>();

    /** The mapping from UML activity control flows to corresponding Petri Net objects. */
    private final Map<ControlFlow, PnObject> controlFlowMapping = new LinkedHashMap<>();

    /**
     * The mapping from an extra decision/merge node introduced as the Petri net translation of a place to a pair
     * containing: the called concrete decision/merge node and the duplicate decision/merge nodes that derive from it.
     */
    Map<ActivityNode, Pair<ActivityNode, List<ActivityNode>>> extraNodeToPair = new LinkedHashMap<>();

    public PNML2UMLTranslator() {
        this(createEmptyUMLModelWithActivity());
    }

    public PNML2UMLTranslator(Activity activity) {
        Preconditions.checkArgument(activity.isAbstract(), "Expected an abstract activity.");
        Preconditions.checkArgument(activity.getNodes().isEmpty(), "Expected abstract activities to not have nodes.");
        Preconditions.checkArgument(activity.getEdges().isEmpty(), "Expected abstract activities to not have edges.");

        this.activity = activity;
    }

    private static Activity createEmptyUMLModelWithActivity() {
        // Create a UML model and initialize it.
        Model model = UML_FACTORY.createModel();
        model.setName("Model");

        // Create a UML class and add it to the model.
        Class clazz = model.createOwnedClass("Class", false);

        // Create an activity for the class.
        Activity activity = UML_FACTORY.createActivity();
        activity.setIsAbstract(true);
        activity.setName("Activity");

        // Add the activity as the owned member of the class.
        clazz.getOwnedBehaviors().add(activity);
        clazz.setClassifierBehavior(activity);

        return activity;
    }

    public Map<Arc, ControlFlow> getArcMapping() {
        return Collections.unmodifiableMap(arcMapping);
    }

    public Map<Place, ControlFlow> getPlaceMapping() {
        return Collections.unmodifiableMap(placeMapping);
    }

    public Map<Transition, ActivityNode> getTransitionMapping() {
        return Collections.unmodifiableMap(transitionMapping);
    }

    public Map<ActivityNode, Node> getNodeMapping() {
        return Collections.unmodifiableMap(nodeMapping);
    }

    public Map<ControlFlow, PnObject> getControlFlowMapping() {
        return Collections.unmodifiableMap(controlFlowMapping);
    }

    public void translateFile(Path inputPath, Path outputPath) throws ImportException, InvalidIDException, IOException {
        // Translate the input Petri Net to a UML activity.
        PetriNet petriNet = PNMLUMLFileHelper.readPetriNet(inputPath.toString());
        translate(petriNet, null);

        // Find the internal actions, and remove them.
        List<ActivityNode> internalNodes = activity.getNodes().stream().filter(node -> node.getName().contains("__"))
                .toList();
        Set<OpaqueAction> actionsToRemove = internalNodes.stream().filter(OpaqueAction.class::isInstance)
                .map(OpaqueAction.class::cast).collect(Collectors.toCollection(LinkedHashSet::new));
        PostProcessActivity.removeOpaqueActions(activity, actionsToRemove);

        // Write the UML activity to the output file.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path umlOutputFilePath = outputPath.resolve(filePrefix + ".uml");
        FileHelper.storeModel(activity.getModel(), umlOutputFilePath.toString());
    }

    public void translate(PetriNet petriNet, SynthesisChainTracking tracker) {
        // According to PNML documents, each Petri Net needs to contain at least one page. Users can add multiple pages
        // to structure their Petri Net in various ways. In our transformation, we add only one page that is mandatory.
        // See more info in: https://dev.lip6.fr/trac/research/ISOIEC15909/wiki/English/User/Structure.
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected the Petri Net to have exactly one page.");

        translate(petriNet.getPages().get(0), tracker);
    }

    private void translate(Page page, SynthesisChainTracking tracker) {
        // Transform all Petri Net transitions.
        List<Transition> transitions = sorted(
                page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast));
        transitions.forEach(t -> translate(t, tracker));

        // Transform all Petri Net places and the arcs connected to them.
        List<Place> places = sorted(page.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast));
        places.forEach(p -> translate(p, tracker));

        // Repair the duplicate decision (and merge) nodes that originate from decision (and merge) nodes of called
        // concrete activities.
        repairDuplicateNodes();

        // Post-process the UML activity to introduce forks and joins where needed.
        introduceForksAndJoins();

        // Rename any duplication markers by means of action renaming.
        transitionMapping.values().forEach(act -> act.setName(getNameWithoutDuplicationPostfix(act.getName())));

        // Indicate that the activity is no longer abstract.
        activity.setIsAbstract(false);
    }

    /**
     * Translate the given Petri net transition into a UML activity node based on its tracking information. The
     * synthesis tracker can be {@code null} for regression testing purposes: in that case, translate every transition
     * as an opaque action.
     *
     * @param transition The Petri net transition.
     * @param tracker The synthesis chain tracker, or {@code null} for testing.
     */
    private void translate(Transition transition, SynthesisChainTracking tracker) {
        Preconditions.checkArgument(!transitionMapping.containsKey(transition),
                "Expected the given transition to have not yet been translated.");

        RedefinableElement umlElement = (tracker == null) ? null : tracker.getUmlElement(transition);
        ActivityNode node = switch (umlElement) {
            case ForkNode f -> UML_FACTORY.createForkNode();
            case JoinNode j -> UML_FACTORY.createJoinNode();
            case DecisionNode d -> UML_FACTORY.createDecisionNode();
            case MergeNode m -> UML_FACTORY.createMergeNode();
            case InitialNode i -> UML_FACTORY.createDecisionNode();
            case ActivityFinalNode f -> UML_FACTORY.createMergeNode();
            case OpaqueBehavior o -> UML_FACTORY.createOpaqueAction();
            case CallBehaviorAction c -> UML_FACTORY.createOpaqueAction();
            case OpaqueAction o -> UML_FACTORY.createOpaqueAction();
            case null -> UML_FACTORY.createOpaqueAction();
            default -> throw new AssertionError(
                    "Unexpected UML element class: " + umlElement.getClass().getSimpleName());
        };

        node.setActivity(activity);
        node.setName(transition.getId());

        nodeMapping.put(node, transition);
        transitionMapping.put(transition, node);

        // Add the newly generated activity node and its corresponding transition to the tracker.
        if (tracker != null) {
            tracker.addActivityNode(node, transition);
        }
    }

    private void translate(Place place, SynthesisChainTracking tracker) {
        Preconditions.checkArgument(!placeMapping.containsKey(place),
                "Expected the given place to have not yet been translated.");

        // Any Petri Net place translates to a control flow in UML. We first find or create the source and target nodes
        // of this control flow, before creating the control flow itself. A new source/target control node may be
        // created in case the corresponding place has no or multiple incoming/outgoing arcs.

        // Find or create the source node of the control flow.
        ActivityNode sourceNode;

        if (place.getInArcs().isEmpty()) {
            Preconditions.checkNotNull(place.getInitialMarking(), "Expected initial tokens but found none.");
            sourceNode = UML_FACTORY.createInitialNode();
            sourceNode.setActivity(activity);
            sourceNode.setName("InitialNode__" + place.getId());
            nodeMapping.put(sourceNode, place);
        } else if (place.getInArcs().size() == 1) {
            sourceNode = transitionMapping.get(place.getInArcs().get(0).getSource());
        } else {
            sourceNode = UML_FACTORY.createMergeNode();
            sourceNode.setActivity(activity);
            sourceNode.setName("Merge__" + place.getId());
            nodeMapping.put(sourceNode, place);

            // Store the concrete merge node and its corresponding duplicate merge nodes, if relevant, for later repair.
            ActivityNode concreteMergeNode = null;
            List<ActivityNode> duplicateMergeNodes = new ArrayList<>();

            for (Arc arc: sorted(place.getInArcs())) {
                Transition sourceTransition = (Transition)arc.getSource();
                ControlFlow flow = createControlFlow(activity, transitionMapping.get(sourceTransition), sourceNode);
                arcMapping.put(arc, flow);
                controlFlowMapping.put(flow, arc);

                // Restore the guards of the concrete control flow connecting the source and target nodes. Avoid calling
                // when the tracker is 'null', e.g. for some regression tests. Further, if the source transition can be
                // tracked to a concrete merge node, store it together with the corresponding duplicate merge nodes that
                // derive from it.
                if (tracker != null) {
                    addConcreteControlFlowGuards(tracker, transitionMapping.get(sourceTransition), sourceNode, flow);

                    if (tracker.getUmlElement(sourceTransition) instanceof MergeNode originalMergeNode) {
                        concreteMergeNode = (concreteMergeNode == null) ? originalMergeNode : concreteMergeNode;
                        duplicateMergeNodes.add(transitionMapping.get(sourceTransition));
                    }
                }
            }

            // Put the concrete ndoe and duplicate nodes into the global map for later repair.
            if (!duplicateMergeNodes.isEmpty()) {
                extraNodeToPair.put(sourceNode, new Pair<>(concreteMergeNode, duplicateMergeNodes));
            }
        }

        // Find or create the target node of the control flow.
        ActivityNode targetNode;

        if (place.getOutArcs().isEmpty()) {
            targetNode = UML_FACTORY.createActivityFinalNode();
            targetNode.setActivity(activity);
            targetNode.setName("FinalNode__" + place.getId());
            nodeMapping.put(targetNode, place);
        } else if (place.getOutArcs().size() == 1) {
            targetNode = transitionMapping.get(place.getOutArcs().get(0).getTarget());
        } else {
            targetNode = UML_FACTORY.createDecisionNode();
            targetNode.setActivity(activity);
            targetNode.setName("Decision__" + place.getId());
            nodeMapping.put(targetNode, place);

            // Store the concrete decision node and its corresponding duplicate decision nodes, if relevant, for later
            // repair.
            ActivityNode concreteDecisionNode = null;
            List<ActivityNode> duplicateDecisionNodes = new ArrayList<>();

            for (Arc arc: sorted(place.getOutArcs())) {
                Transition targetTransition = (Transition)arc.getTarget();
                ControlFlow flow = createControlFlow(activity, targetNode, transitionMapping.get(targetTransition));
                arcMapping.put(arc, flow);
                controlFlowMapping.put(flow, arc);

                // Restore the guards of the concrete control flow connecting the source and target nodes. Avoid calling
                // when the tracker is 'null', e.g. for some regression tests. Further, if the source transition can be
                // tracked to a concrete decision node, store it together with the corresponding duplicate decision
                // nodes that derive from it.
                if (tracker != null) {
                    addConcreteControlFlowGuards(tracker, targetNode, transitionMapping.get(targetTransition), flow);

                    if (tracker.getUmlElement(targetTransition) instanceof DecisionNode originalDecisionNode) {
                        concreteDecisionNode = (concreteDecisionNode == null) ? originalDecisionNode
                                : concreteDecisionNode;
                        duplicateDecisionNodes.add(transitionMapping.get(targetTransition));
                    }
                }
            }

            // Put the concrete and duplicate decision node into the global map for later repair.
            if (!duplicateDecisionNodes.isEmpty()) {
                extraNodeToPair.put(targetNode, new Pair<>(concreteDecisionNode, duplicateDecisionNodes));
            }
        }

        // Create the control flow that connects the source node to the target node.
        ControlFlow controlFlow = createControlFlow(activity, sourceNode, targetNode);
        controlFlowMapping.put(controlFlow, place);
        placeMapping.put(place, controlFlow);

        if (place.getInArcs().size() == 1) {
            arcMapping.put(place.getInArcs().get(0), controlFlow);
        }
        if (place.getOutArcs().size() == 1) {
            arcMapping.put(place.getOutArcs().get(0), controlFlow);
        }

        // Restore the guards of the concrete control flow connecting the source and target nodes, if relevant.
        // Avoid calling when the tracker is 'null', e.g. for some regression tests.
        if (tracker != null) {
            addConcreteControlFlowGuards(tracker, sourceNode, targetNode, controlFlow);
        }
    }

    /**
     * Adds the source node's incoming guard of the outgoing edge and the target node's outgoing guard of the incoming
     * edge to the given control flow.
     *
     * @param tracker The synthesis tracker.
     * @param sourceNode The source node.
     * @param targetNode The target node.
     * @param controlFlow The control flow where to add the guards.
     */
    private void addConcreteControlFlowGuards(SynthesisChainTracking tracker, ActivityNode sourceNode,
            ActivityNode targetNode, ControlFlow controlFlow)
    {
        // Track the incoming guard to the outgoing edge of the source node. If the node does not derive form a concrete
        // activity node, use 'null' guard.
        RedefinableElement originalSourceElement = tracker.getOriginalUmlElement(sourceNode);
        String incomingGuard;
        if (originalSourceElement == null || tracker.belongsToSynthesizedActivity(originalSourceElement)) {
            incomingGuard = null;
        } else {
            incomingGuard = tracker.getOutgoingEdgeIncomingGuard(sourceNode);
        }

        // Track the outgoing guard to the incoming edge of the target node. If the node does not derive form a concrete
        // activity node, use 'null' guard.
        RedefinableElement originalTargetElement = tracker.getOriginalUmlElement(targetNode);
        String outgoingGuard;
        if (originalTargetElement == null || tracker.belongsToSynthesizedActivity(originalTargetElement)) {
            outgoingGuard = null;
        } else {
            outgoingGuard = tracker.getIncomingEdgeOutgoingGuard(targetNode);
        }

        // Set the guards on the given control flow.
        PokaYokeUmlProfileUtil.setIncomingGuard(controlFlow, incomingGuard);
        PokaYokeUmlProfileUtil.setOutgoingGuard(controlFlow, outgoingGuard);
    }

    /**
     * Repair the duplicate decision (and merge) nodes that are created as a result of the translation a decision (and
     * merge) node in a called concrete activity.
     */
    private void repairDuplicateNodes() {
        for (Entry<ActivityNode, Pair<ActivityNode, List<ActivityNode>>> extraNodeToMap: extraNodeToPair.entrySet()) {
            ActivityNode extraNode = extraNodeToMap.getKey();
            Pair<ActivityNode, List<ActivityNode>> concreteToDuplicateMap = extraNodeToMap.getValue();

            if (extraNode instanceof DecisionNode extraDecisionNode) {
                repairDuplicateDecisionNodes(extraDecisionNode, concreteToDuplicateMap.left,
                        concreteToDuplicateMap.right);
            } else if (extraNode instanceof MergeNode extraMergeNode) {
                repairDuplicateMergeNodes(extraMergeNode, concreteToDuplicateMap.left, concreteToDuplicateMap.right);
            } else {
                throw new RuntimeException(String.format("Activity node '%s' is neither a decision nor a merge node.",
                        extraNode.getName()));
            }
        }
    }

    /**
     * Repair duplicate decision nodes when: 1) All nodes can be traced back to the same decision node in a called
     * concrete activity; 2) Each node has exactly one incoming edge; 3) That edge comes from the extra decision node
     * created as a translation of a Petri net place with multiple outgoing arcs.
     *
     * In practice, keep the first duplicate node, redirect outgoing edges of the other nodes to start from it, then
     * delete the remaining nodes and their incoming edges.
     *
     * @param extraDecisionNode The decision node introduced as translation of a Petri net place.
     * @param concreteDecisionNode The decision node belonging to a called concrete activity.
     * @param duplicateNodes The list of duplicate decision nodes.
     */
    private void repairDuplicateDecisionNodes(DecisionNode extraDecisionNode, ActivityNode concreteDecisionNode,
            List<ActivityNode> duplicateNodes)
    {
        // If all duplicate decision nodes have only one incoming edge and the edge is coming from the newly created
        // decision node, we can unify them.
        boolean unifyable = duplicateNodes.stream().allMatch(
                m -> m.getIncomings().size() == 1 && m.getIncomings().get(0).getSource().equals(extraDecisionNode));

        if (!unifyable) {
            return;
        }

        // Sanity check: all the duplicate decision nodes have the outgoing guard of the incoming edge equal to the
        // outgoing guard of the incoming edge of the concrete decision node (from which they are derived).
        String concreteOutgoingGuard = PokaYokeUmlProfileUtil
                .getOutgoingGuard(concreteDecisionNode.getIncomings().get(0));
        boolean equalOutgoingGuard = duplicateNodes.stream().allMatch(m -> java.util.Objects.equals(
                // Correctly handle 'null' values.
                PokaYokeUmlProfileUtil.getOutgoingGuard(m.getIncomings().get(0)), concreteOutgoingGuard));
        Verify.verify(equalOutgoingGuard, String.format(
                "The derived decision nodes from node '%s' have a different outgoing guard on their incoming edges.",
                concreteDecisionNode.getName()));

        // Sanity check: the number of duplicate decision nodes must be equal to the number of outgoing edges of the
        // concrete decision node.
        Verify.verify(concreteDecisionNode.getOutgoings().size() == duplicateNodes.size(), String.format(
                "The concrete decision node '%s' has %s outgoing edges, but found %s corresponding duplicate decision nodes.",
                concreteDecisionNode.getName(), String.valueOf(concreteDecisionNode.getOutgoings().size()),
                String.valueOf(duplicateNodes.size())));

        // Redirect all outgoing control flows to start from the first duplicate decision node in the list. Mark the
        // other duplicate decision nodes and their incoming edges to be deleted.
        List<ActivityNode> nodesToDelete = duplicateNodes.subList(1, duplicateNodes.size());
        List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getIncomings().get(0)).toList();
        for (ActivityNode node: new ArrayList<>(nodesToDelete)) { // Avoid concurrent duplication error.
            for (ActivityEdge edge: new ArrayList<>(node.getOutgoings())) {
                edge.setSource(duplicateNodes.get(0));
            }
        }

        // Update the internal maps, and finally destroy the duplicate nodes and edges.
        List<Arc> arcsToRemap = controlFlowMapping.entrySet().stream().filter(e -> edgesToDelete.contains(e.getKey()))
                .map(e -> e.getValue()).map(Arc.class::cast).toList();
        controlFlowMapping.keySet().removeAll(edgesToDelete);
        arcsToRemap.stream()
                // Now multiple arcs refer to the same incoming control flow of the first duplicate decision node.
                .forEach(a -> arcMapping.put(a, (ControlFlow)duplicateNodes.get(0).getIncomings().get(0)));

        List<Transition> transitionsToRemap = nodeMapping.entrySet().stream()
                .filter(e -> nodesToDelete.contains(e.getKey())).map(e -> e.getValue()).map(Transition.class::cast)
                .toList();
        nodeMapping.keySet().removeAll(nodesToDelete);
        transitionsToRemap.stream()
                // Now multiple transitions refer to the same node, i.e. the first duplicate merge node.
                .forEach(t -> transitionMapping.put(t, duplicateNodes.get(0)));

        for (ActivityEdge edge: new ArrayList<>(edgesToDelete)) {
            edge.destroy();
        }

        for (ActivityNode node: new ArrayList<>(nodesToDelete)) {
            node.destroy();
        }
    }

    /**
     * Repair duplicate merge nodes when: 1) All nodes can be traced back to the same merge node in a called concrete
     * activity; 2) Each node has exactly one outgoing edge; 3) That edge points to the extra merge node created as a
     * translation of a Petri net place with multiple incoming arcs.
     *
     * In practice, keep the first duplicate node, redirect incoming edges from other nodes to it, then delete the
     * remaining nodes and their outgoing edges.
     *
     * @param extraMergeNode The merge node introduced as translation of a Petri net place.
     * @param concreteMergeNode The merge node belonging to a called concrete activity.
     * @param duplicateNodes The list of duplicate merge nodes.
     */
    private void repairDuplicateMergeNodes(MergeNode extraMergeNode, ActivityNode concreteMergeNode,
            List<ActivityNode> duplicateNodes)
    {
        // If all duplicate merge nodes have only one outgoing edge and the edge is towards the newly created merge
        // node, we can unify them.
        boolean unifyable = duplicateNodes.stream().allMatch(
                m -> m.getOutgoings().size() == 1 && m.getOutgoings().get(0).getTarget().equals(extraMergeNode));

        if (!unifyable) {
            return;
        }

        // Sanity check: all the duplicate merge nodes have the incoming guard of the outgoing edge equal to the
        // incoming guard of the outgoing edge of the concrete merge node (from which they are derived).
        String concreteIncomingGuard = PokaYokeUmlProfileUtil.getIncomingGuard(concreteMergeNode.getOutgoings().get(0));
        boolean equalIncomingGuard = duplicateNodes.stream().allMatch(m -> java.util.Objects.equals(
                // Correctly handle 'null' values.
                PokaYokeUmlProfileUtil.getIncomingGuard(m.getOutgoings().get(0)), concreteIncomingGuard));
        Verify.verify(equalIncomingGuard, String.format(
                "The derived merge nodes from node '%s' have a different incoming guard on their outgoing edges.",
                concreteMergeNode.getName()));

        // Sanity check: the number of duplicate merge nodes must be equal to the number of incoming edges of the
        // concrete merge node.
        Verify.verify(concreteMergeNode.getIncomings().size() == duplicateNodes.size(), String.format(
                "The concrete merge node '%s' has %s incoming edges, but found %s corresponding duplicate merge nodes.",
                concreteMergeNode.getName(), String.valueOf(concreteMergeNode.getIncomings().size()),
                String.valueOf(duplicateNodes.size())));

        // Redirect all incoming control flows towards the first duplicate merge node in the list. Mark the other
        // duplicate merge nodes and their outgoing edges to be deleted.
        List<ActivityNode> nodesToDelete = duplicateNodes.subList(1, duplicateNodes.size());
        List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getOutgoings().get(0)).toList();
        for (ActivityNode node: new ArrayList<>(nodesToDelete)) { // Avoid concurrent duplication error.
            for (ActivityEdge edge: new ArrayList<>(node.getIncomings())) {
                edge.setTarget(duplicateNodes.get(0));
            }
        }

        // Update the internal maps, and finally destroy the duplicate nodes and edges.
        List<Arc> arcsToRemap = controlFlowMapping.entrySet().stream().filter(e -> edgesToDelete.contains(e.getKey()))
                .map(e -> e.getValue()).map(Arc.class::cast).toList();
        controlFlowMapping.keySet().removeAll(edgesToDelete);
        arcsToRemap.stream()
                // Now multiple arcs refer to the same outgoing control flow of the first duplicate merge node.
                .forEach(a -> arcMapping.put(a, (ControlFlow)duplicateNodes.get(0).getOutgoings().get(0)));

        List<Transition> transitionsToRemap = nodeMapping.entrySet().stream()
                .filter(e -> nodesToDelete.contains(e.getKey())).map(e -> e.getValue()).map(Transition.class::cast)
                .toList();
        nodeMapping.keySet().removeAll(nodesToDelete);
        transitionsToRemap.stream()
                // Now multiple transitions refer to the same node, i.e. the first duplicate merge node.
                .forEach(t -> transitionMapping.put(t, duplicateNodes.get(0)));

        for (ActivityEdge edge: new ArrayList<>(edgesToDelete)) {
            edge.destroy();
        }

        for (ActivityNode node: new ArrayList<>(nodesToDelete)) {
            node.destroy();
        }
    }

    private void introduceForksAndJoins() {
        // Collect all action nodes in the given UML activity.
        List<Action> actions = activity.getNodes().stream().filter(Action.class::isInstance).map(Action.class::cast)
                .toList();

        // Transform any fork or join pattern in any action node.
        for (Action action: actions) {
            Preconditions.checkArgument(!action.getIncomings().isEmpty(), "Expected at least one incoming edge.");
            Preconditions.checkArgument(!action.getOutgoings().isEmpty(), "Expected at least one outgoing edge.");

            // Introduce a join node in case there are multiple incoming control flows.
            if (action.getIncomings().size() > 1) {
                JoinNode join = UML_FACTORY.createJoinNode();
                join.setActivity(activity);
                join.setName("Join__" + action.getName());
                nodeMapping.put(join, nodeMapping.get(action));

                for (ActivityEdge controlFlow: new ArrayList<>(action.getIncomings())) {
                    controlFlow.setName(concatenateNamesOf(controlFlow.getSource(), join));
                    controlFlow.setTarget(join);
                }

                ControlFlow controlFlow = createControlFlow(activity, join, action);
                controlFlowMapping.put(controlFlow, nodeMapping.get(action));
            }

            // Introduce a fork node in case there are multiple outgoing control flows.
            if (action.getOutgoings().size() > 1) {
                ForkNode fork = UML_FACTORY.createForkNode();
                fork.setActivity(activity);
                fork.setName("Fork__" + action.getName());
                nodeMapping.put(fork, nodeMapping.get(action));

                for (ActivityEdge controlFlow: new ArrayList<>(action.getOutgoings())) {
                    controlFlow.setName(concatenateNamesOf(fork, controlFlow.getTarget()));
                    controlFlow.setSource(fork);
                }

                ControlFlow controlFlow = createControlFlow(activity, action, fork);
                controlFlowMapping.put(controlFlow, nodeMapping.get(action));
            }
        }
    }

    static ControlFlow createControlFlow(Activity activity, ActivityNode source, ActivityNode target) {
        ControlFlow controlFlow = UML_FACTORY.createControlFlow();
        controlFlow.setActivity(activity);
        controlFlow.setName(concatenateNamesOf(source, target));
        controlFlow.setSource(source);
        controlFlow.setTarget(target);
        return controlFlow;
    }

    private <T extends PnObject> List<T> sorted(Collection<T> objects) {
        return sorted(objects.stream());
    }

    private <T extends PnObject> List<T> sorted(Stream<T> stream) {
        return stream.sorted(Comparator.comparing(PnObject::getId)).toList();
    }

    private static String concatenateNamesOf(ActivityNode left, ActivityNode right) {
        return left.getName() + "__to__" + right.getName();
    }

    private static String getNameWithoutDuplicationPostfix(String name) {
        return name.contains("/") ? name.split("/")[0] : name;
    }
}
