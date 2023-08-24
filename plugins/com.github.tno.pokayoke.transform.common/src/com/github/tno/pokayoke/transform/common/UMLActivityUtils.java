/**
 *
 */

package com.github.tno.pokayoke.transform.common;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;

/** Utils that process UML activity diagrams.*/
public class UMLActivityUtils {
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
}
