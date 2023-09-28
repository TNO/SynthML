
package com.github.tno.pokayoke.transform.common;

import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.ControlFlow;

/**
 * Helper for adding structure info.
 */
public class StructureInfoHelper {
    private static final String STRUCTURE_INFO_IDENTIFIER = "Original-Structure";

    private int callBehaviorCounter = 0;

    public StructureInfoHelper() {
    }

    public void updateCounter() {
        callBehaviorCounter = callBehaviorCounter + 1;
    }

    public int getCount() {
        return callBehaviorCounter;
    }

    public void addStructureInfo(ControlFlow newEdge, String postfix) {
        String structureInfo = String.valueOf(callBehaviorCounter) + " " + postfix;
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(STRUCTURE_INFO_IDENTIFIER + ":" + structureInfo);
        newEdge.getOwnedComments().add(comment);
    }
}
