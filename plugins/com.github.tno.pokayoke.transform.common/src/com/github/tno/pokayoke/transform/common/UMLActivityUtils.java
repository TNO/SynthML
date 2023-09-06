
package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;

/** Utils that process UML activity diagrams. */
public class UMLActivityUtils {
    private static final List<String> OBJECT_TYPE = Arrays.asList("MergeNode", "ForkNode", "JoinNode", "DecisionNode",
            "CallBehaviorAction", "InitialNode", "FlowFinalNode", "ActivityFinalNode", "OpaqueAction", "ActivityEdge");

    private static final List<Activity> RENAMED_ACTIVITY = new ArrayList<>();

    private static final List<Activity> TRACED_ACTIVITY = new ArrayList<>();

    private static final Map<String, Integer> OBJECT_COUNT = OBJECT_TYPE.stream()
            .collect(Collectors.toMap(key -> key, key -> 0));

    private UMLActivityUtils() {
    }

    /**
     * Removes irrelevant and redundant information from the given activity, like edge weights or redundant edge guards.
     *
     * @param activity The activity to clean up.
     */
    public static void removeIrrelevantInformation(Activity activity) {
        // Remove any default weights from all edges.
        for (ActivityEdge edge: activity.getEdges()) {
            if (edge.getWeight() instanceof LiteralInteger literal && literal.getValue() == 0) {
                edge.setWeight(null);
            }
        }

        // Remove any default guards from all edges not coming out of decision nodes.
        for (ActivityEdge edge: activity.getEdges()) {
            if (!(edge.getSource() instanceof DecisionNode) && edge.getGuard() instanceof LiteralBoolean literal
                    && literal.isValue())
            {
                edge.setGuard(null);
            }
        }
    }

    /**
     * Checks if a double underscore is used in the name of any model elements.
     *
     * @param model The model to check.
     * @return The result of the check.
     */
    public static boolean isDoubleUnderscoreUsed(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                if (namedElement.getName() != null && namedElement.getName().contains("__")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void setNameForNodesAndEdges(Activity childBehavior, String absoluteName) {
        // If the name of node has not been added, the concatenation of absolute name and the name of the node is
        // added to the comment. Otherwise, the absolute name is appended.
        String prefix = absoluteName + "__";
        for (ActivityNode node: childBehavior.getNodes()) {
            String name = prefix;
            if (node.getName() == null || node.getName().equals("")) {
                name = name + getNameOfObject(node.eClass().getName());
            } else {
                name = name + node.getName();
            }
            node.setName(name);
        }

        // If the name of edge has not been added, the concatenation of absolute name and the name of the edge is
        // added to the comment. Otherwise, the absolute name is appended.
        for (ActivityEdge edge: new ArrayList<>(childBehavior.getEdges())) {
            String name = prefix;
            if (edge.getName() == null) {
                name = name + getNameOfObject("ActivityEdge");
            } else {
                name = name + edge.getName();
            }

            edge.setName(name);
        }

        // Mark that this activity has been visited for naming.
        RENAMED_ACTIVITY.add(childBehavior);
    }

    public static void appendCallBehaviorActionName(Activity childBehavior, String absoluteName) {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            node.setName(absoluteName + "__" + node.getName());
        }

        for (ActivityEdge edge: new ArrayList<>(childBehavior.getEdges())) {
            edge.setName(absoluteName + "__" + edge.getName());
        }
    }

    public static void setTracingCommentForNodesAndEdges(Activity childBehavior, String absoluteID) {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            // If a tracing comment has not been added, the concatenation of absolute ID and the ID of the node is added
            // to the comment. Otherwise, the absolute ID is appended to the comment.
            if (node.getOwnedComments().size() == 0) {
                Comment comment = FileHelper.FACTORY.createComment();
                comment.setBody(absoluteID + "__" + node.eResource().getURIFragment(node));
                node.getOwnedComments().add(comment);
            } else {
                for (Comment nodeComment: node.getOwnedComments()) {
                    nodeComment.setBody(absoluteID + "__" + nodeComment.getBody());
                }
            }
        }
        for (ActivityEdge edge: new ArrayList<>(childBehavior.getEdges())) {
            // If a tracing comment has not been added, the concatenation of absolute ID and the ID of the edge is added
            // to the comment. Otherwise, the absolute ID is appended to the comment.
            if (edge.getOwnedComments().size() == 0) {
                Comment comment = FileHelper.FACTORY.createComment();
                comment.setBody(absoluteID + "__" + edge.eResource().getURIFragment(edge));
                edge.getOwnedComments().add(comment);
            } else {
                for (Comment edgeComment: edge.getOwnedComments()) {
                    edgeComment.setBody(absoluteID + "__" + edgeComment.getBody());
                }
            }
        }

        // Mark that the tracing comment has been added to this activity.
        TRACED_ACTIVITY.add(childBehavior);
    }

    public static void appendCallBehaviorActionID(Activity childBehavior, String id) {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            int i = node.getOwnedComments().size();
            String idChain = id + "__" + node.getOwnedComments().get(i - 1).getBody();
            Comment comment = FileHelper.FACTORY.createComment();
            comment.setBody(idChain);
            node.getOwnedComments().set(i - 1, comment);
        }

        for (ActivityEdge edge: new ArrayList<>(childBehavior.getEdges())) {
            int i = edge.getOwnedComments().size();
            String idChain = id + "__" + edge.getOwnedComments().get(i - 1).getBody();
            Comment comment = FileHelper.FACTORY.createComment();
            comment.setBody(idChain);
            edge.getOwnedComments().set(i - 1, comment);
        }
    }

    /**
     * Set tracing comment for a newly added edge that connect the content of an nested activity to the outer activity.
     *
     * @param outerEdge The edge from the outer activity, connecting to the call behavior action. This could be the
     *     incoming and outgoing edges of the call behavior action.
     * @param innerEdge The edge inside the nested activity. This could be outgoing edges of the initial node or the
     *     incoming edges of the activity final node.
     * @param newEdge The newly created edge for connecting the content of inner activity to the outer activity.
     */
    public static void setTracingCommentForAddedEdge(ActivityEdge outerEdge, ActivityEdge innerEdge,
            ActivityEdge newEdge)
    {
        String idOuterEdge = outerEdge.eResource().getURIFragment(outerEdge);
        Comment comment1 = FileHelper.FACTORY.createComment();
        comment1.setBody(idOuterEdge);
        newEdge.getOwnedComments().add(comment1);

        int i = innerEdge.getOwnedComments().size();
        String idInnerEdge = innerEdge.getOwnedComments().get(i - 1).getBody();
        Comment comment2 = FileHelper.FACTORY.createComment();
        comment2.setBody(idInnerEdge);
        newEdge.getOwnedComments().add(comment2);
    }

    public static String getNameOfObject(String className) {
        String name = className + "_" + String.valueOf(OBJECT_COUNT.get(className) + 1);
        OBJECT_COUNT.put(className, OBJECT_COUNT.get(className) + 1);
        return name;
    }

    public static boolean isNamed(Activity activity) {
        return RENAMED_ACTIVITY.contains(activity);
    }

    public static boolean isTraced(Activity activity) {
        return TRACED_ACTIVITY.contains(activity);
    }

    public static void ensureUniquenessOfNames(Model model) {
        List<String> names = new ArrayList<>();
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                names.add(namedElement.getName());
                int count = Collections.frequency(names, namedElement.getName());
                if (count > 1) {
                    namedElement.setName(namedElement.getName() + "_" + String.valueOf(count));
                }
            }
        }
    }
}
