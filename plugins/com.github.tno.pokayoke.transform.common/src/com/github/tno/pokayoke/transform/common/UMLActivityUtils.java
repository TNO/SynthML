<<<<<<< HEAD
=======
/**
 *
 */
>>>>>>> 40d74c30a058b0ae594aad8e51d887140b76a697

package com.github.tno.pokayoke.transform.common;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;

<<<<<<< HEAD
/** Utils that process UML activity diagrams. */
=======
/** Utils that process UML activity diagrams.*/
>>>>>>> 40d74c30a058b0ae594aad8e51d887140b76a697
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
