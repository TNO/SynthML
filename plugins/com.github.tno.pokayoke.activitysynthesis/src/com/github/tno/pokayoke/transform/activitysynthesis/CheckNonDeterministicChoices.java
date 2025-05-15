
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.DecisionNode;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;

public class CheckNonDeterministicChoices {
    private CheckNonDeterministicChoices() {
    }

    /**
     * Checks the decision nodes of an activity for non-deterministic transitions, and registers a warning if found.
     *
     * @param activity The activity to check.
     * @param translator The UML to CIF translator that was used to translate the UML input model to the CIF
     *     specification.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     * @param bddSpec The CIF/BDD specification.
     */
    public static void check(Activity activity, UmlToCifTranslator translator, List<String> warnings,
            CifBddSpec bddSpec)
    {
        for (ActivityNode node: activity.getNodes()) {
            // If the current node is a decision node with multiple outgoing edges, then check for non-determinism.
            if (node instanceof DecisionNode decisionNode && decisionNode.getOutgoings().size() > 1) {
                // Check if (at least) two edges can be fired at the same time.
                check(decisionNode, translator, warnings, bddSpec);
            }
        }
    }

    /**
     * Checks a single node for non-deterministic transitions, and registers a warning if found.
     *
     * @param node The activity node to check for non-deterministic transitions.
     * @param translator The UML to CIF translator that was used to translate the UML input model to the CIF
     *     specification.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     * @param bddSpec The CIF/BDD specification.
     */
    private static void check(ActivityNode node, UmlToCifTranslator translator, List<String> warnings,
            CifBddSpec bddSpec)
    {
        Map<ActivityEdge, BDD> edgeGuardMap = new LinkedHashMap<>();

        // Check every edge against every other edge. An edge is not checked against itself. A warning is printed for
        // each pair only once, so not for commuted pairs.
        for (ActivityEdge edge: node.getOutgoings()) {
            // Get the guard of the edge as a BDD.
            Expression cifGuard = translator.getIncomingGuard(edge);
            BDD bddGuard;
            try {
                bddGuard = CifToBddConverter.convertPred(cifGuard, false, bddSpec);
            } catch (UnsupportedPredicateException e) {
                throw new RuntimeException(
                        String.format("Failed to convert CIF expression into BDD, with predicate \"%s\".",
                                CifTextUtils.exprToStr(cifGuard)),
                        e);
            }

            // Check the current edge against the previous ones, registering a warning in case of guard overlap.
            for (var entry: edgeGuardMap.entrySet()) {
                // Compute the logical conjunction of the current guard and the previously computed one.
                BDD guardOverlap = entry.getValue().and(bddGuard);

                // If the overlap between the two guards is not empty, write the warning.
                if (!guardOverlap.isZero()) {
                    // Add a warning that a non-deterministic node has been found.
                    String currentEdgeTargetName = edge.getTarget().getName();
                    String currentEdgeGuardName = edge.getName();
                    String entryTargetName = entry.getKey().getTarget().getName();
                    String entryGuardName = entry.getKey().getName();
                    String message = String.format(
                            "Non-deterministic decision node found, leading to %s (guard: %s) and to %s (guard: %s).",
                            "\'" + (currentEdgeTargetName == null ? "control node" : currentEdgeTargetName) + "\'",
                            "\'" + (currentEdgeGuardName == null ? "true" : currentEdgeGuardName) + "\'",
                            "\'" + (entryTargetName == null ? "control node" : entryTargetName) + "\'",
                            "\'" + (entryGuardName == null ? "true" : entryGuardName) + "\'");
                    warnings.add(message);
                }

                // Free the BDD representing the logical conjunction.
                guardOverlap.free();
            }

            // Add the current edge and BDD guard to the map.
            edgeGuardMap.put(edge, bddGuard);
        }

        // Free all the BDDs in the map.
        for (var entry: edgeGuardMap.entrySet()) {
            entry.getValue().free();
        }
    }
}
