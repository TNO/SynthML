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

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;

/** Helper for adding structure information to edges. */
public class StructureInfoHelper {
    private static final String STRUCTURE_INFO_IDENTIFIER = "Original-Structure";

    private int counter = 0;

    public void addStructureInfoInActivities(Namespace namespace) {
        for (NamedElement member: namespace.getOwnedMembers()) {
            if (member instanceof Activity activityMember) {
                addStructureInfoInActivity(activityMember);
            } else if (member instanceof Class classMember) {
                addStructureInfoInActivities(classMember);
            } else if (member instanceof Model modelMember) {
                addStructureInfoInActivities(modelMember);
            }
        }
    }

    public void addStructureInfoInActivity(Activity activity) {
        incrementCounter();

        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof InitialNode initialNode) {
                addStructureStartInfo(initialNode.getOutgoings().get(0));
            }
            if (node instanceof FinalNode finalNode) {
                addStructureEndInfo(finalNode.getIncomings().get(0));
            }
        }
    }

    public void incrementCounter() {
        counter = counter + 1;
    }

    public void addStructureStartInfo(ActivityEdge edge) {
        addStructureInfo(edge, "Start");
    }

    public void addStructureEndInfo(ActivityEdge edge) {
        addStructureInfo(edge, "End");
    }

    private void addStructureInfo(ActivityEdge edge, String postfix) {
        String structureInfo = String.valueOf(counter) + " " + postfix;
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(STRUCTURE_INFO_IDENTIFIER + ":" + structureInfo);
        edge.getOwnedComments().add(comment);
    }
}
