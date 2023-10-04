
package com.github.tno.pokayoke.transform.common;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.InitialNode;

/** Helper for adding structure information to edges. */
public class StructureInfoHelper {
    private static final String STRUCTURE_INFO_IDENTIFIER = "Original-Structure";

    private int counter = 0;

    public void addStructureInfoInActivities(Class contextClass) {
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity) {
                incrementCounter();
                for (ActivityNode node: activity.getNodes()) {
                    if (node instanceof InitialNode initialNode) {
                        for (ActivityEdge edge: initialNode.getOutgoings()) {
                            addStructureStartInfo(edge);
                        }
                    }
                    if (node instanceof ActivityFinalNode finalNode) {
                        for (ActivityEdge edge: finalNode.getIncomings()) {
                            addStructureEndInfo(edge);
                        }
                    }
                }
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
