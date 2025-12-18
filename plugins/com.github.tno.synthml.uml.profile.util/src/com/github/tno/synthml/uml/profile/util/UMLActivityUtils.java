////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.util;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;

/** Utils that process UML activities. */
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
        // According to the document "Semantics of a Foundational Subset for Executable UML Models", version 1.5, page
        // 40: "A guard is only allowed if the source of the edge is a DecisionNode" in fUML.
        for (ActivityEdge edge: activity.getEdges()) {
            if (!(edge.getSource() instanceof DecisionNode) && edge.getGuard() instanceof LiteralBoolean literal
                    && literal.isValue())
            {
                edge.setGuard(null);
            }
        }
    }
}
