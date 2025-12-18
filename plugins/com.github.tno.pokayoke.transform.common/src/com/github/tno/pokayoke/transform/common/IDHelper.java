////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;

import com.google.common.base.Verify;

/** Helper class for adding tracing ID to model elements. */
public class IDHelper {
    private static final String TRACING_IDENTIFIER = "Original-ID-Path";

    private IDHelper() {
    }

    /**
     * Adds the ID of model elements to their comments.
     *
     * @param model The model that contains elements.
     */
    public static void addIDTracingCommentToModelElements(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                addTracingComment(namedElement, getID(namedElement));
            }
        }
    }

    /**
     * Adds a tracing comment to a model element.
     *
     * @param element The model element.
     * @param id The ID to add as a tracing comment of the model element.
     */
    public static void addTracingComment(NamedElement element, String id) {
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(TRACING_IDENTIFIER + ":" + id);
        element.getOwnedComments().add(comment);
    }

    /**
     * Gets the ID of a model element.
     *
     * @param element The element.
     * @return The ID of the element.
     */
    public static String getID(NamedElement element) {
        return EcoreUtil.getURI(element).fragment();
    }

    /**
     * Prepends the IDs of the call behavior action and activity to the comments of the nodes and edges in an activity.
     *
     * @param activity The activity that contains nodes and edges.
     * @param action The call behavior action that calls the activity.
     */
    public static void prependPrefixIDToNodesAndEdgesInActivity(Activity activity, CallBehaviorAction action) {
        List<String> actionIDs = extractIDsFromTracingComment(action);
        List<String> activityIDs = extractIDsFromTracingComment(activity);

        Verify.verify(actionIDs.size() == 1,
                String.format("Action %s should have only one tracing comment.", action.getName()));
        Verify.verify(activityIDs.size() == 1,
                String.format("Activity %s should have only one tracing comment.", activity.getName()));

        String id = actionIDs.get(0) + " " + activityIDs.get(0);

        for (ActivityNode node: activity.getNodes()) {
            prependPrefixID(node, id);
        }

        for (ActivityEdge edge: activity.getEdges()) {
            prependPrefixID(edge, id);
        }
    }

    private static void prependPrefixID(NamedElement element, String prefixID) {
        List<Comment> comments = element.getOwnedComments().stream().filter(c -> isTracingComment(c)).toList();

        for (Comment comment: comments) {
            // Split the comment body into header and ID chain.
            String[] commentBody = comment.getBody().split(":");
            comment.setBody(commentBody[0] + ":" + prefixID + " " + commentBody[1]);
        }
    }

    /**
     * Extracts the IDs from the tracing comments of the element.
     *
     * @param element The element that contains tracing comments.
     * @return The IDs in the tracing comments.
     */
    public static List<String> extractIDsFromTracingComment(NamedElement element) {
        List<String> tracingComments = new ArrayList<>();
        for (Comment comment: element.getOwnedComments()) {
            if (isTracingComment(comment)) {
                tracingComments.add(comment.getBody().split(":")[1]);
            }
        }
        return tracingComments;
    }

    private static boolean isTracingComment(Comment comment) {
        return comment.getBody().split(":")[0].equals(TRACING_IDENTIFIER);
    }
}
