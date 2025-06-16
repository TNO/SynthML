
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.utils.CifBddReachability;
import org.eclipse.escet.cif.datasynth.CifDataSynthesis;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisTiming;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;

import com.github.javabdd.BDD;
import com.google.common.base.Verify;

/**
 * Helper methods for guard computation. These are needed because synthesis in CIF frees too many BDDs during synthesis.
 * Once that is resolved in CIF, we shouldn't need this class anymore.
 *
 * @see <a href="https://gitlab.eclipse.org/eclipse/escet/escet/-/issues/1200">ESCET issue #1200</a>
 * @see <a href="https://gitlab.eclipse.org/eclipse/escet/escet/-/issues/1201">ESCET issue #1201</a>
 */
public class GuardComputationHelper {
    private GuardComputationHelper() {
        // Static class.
    }

    /**
     * Computes the set of all forward reachable states from the given predicate, using all edges in the given CIF/BDD
     * specification. This method will ensure that the error, guard, and update predicates of the edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @param predicate The predicate from which to start exploring, which will be {@link BDD#free freed} afterwards.
     * @param restriction The predicate that indicates the upper bound on the reachable states, or {@code null} in case
     *     there is no such upper bound (which is equivalent to providing the BDD 'true' as the upper bound).
     * @param badStates Whether the given predicate represents bad states ({@code true}) or good states ({@code false}).
     * @return The set of forward reachable states, as a BDD predicate.
     */
    private static BDD reachForward(CifBddSpec cifBddSpec, BDD predicate, BDD restriction, boolean badStates) {
        int nrOfEdges = cifBddSpec.edges.size();

        // Make a copy of all error, guard, and update predicates as the reachability computation will free them.
        Map<CifBddEdge, BDD> errors = new LinkedHashMap<>(nrOfEdges);
        Map<CifBddEdge, BDD> guards = new LinkedHashMap<>(nrOfEdges);
        Map<CifBddEdge, BDD> updates = new LinkedHashMap<>(nrOfEdges);

        for (CifBddEdge edge: cifBddSpec.edges) {
            errors.put(edge, edge.error.id());
            guards.put(edge, edge.guard.id());
            updates.put(edge, edge.update.id());
        }

        // Compute all forward reachable states.
        cifBddSpec.edges.forEach(edge -> edge.initApply(true));
        CifBddReachability reach = new CifBddReachability(cifBddSpec, "forward search", "predicate",
                restriction != null ? "restriction" : null, restriction, badStates, true, true, true, false);
        BDD reachableStates = reach.performReachability(predicate);
        cifBddSpec.edges.forEach(edge -> edge.cleanupApply());

        // Restore the copy of the error, guard, and update predicates, since the originals were freed.
        for (CifBddEdge edge: cifBddSpec.edges) {
            edge.error = errors.get(edge);
            edge.guard = guards.get(edge);
            edge.update = updates.get(edge);
        }

        return reachableStates;
    }

    /**
     * Applies data-based synthesis to the given CIF/BDD specification. This method will ensure that the error, guard,
     * and update predicates of the edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The data-based synthesis result.
     */
    private static CifDataSynthesisResult synthesize(CifBddSpec cifBddSpec) {
        int nrOfEdges = cifBddSpec.edges.size();

        // Make a copy of all error, guard, and update predicates as data-based synthesis will free them.
        Map<CifBddEdge, BDD> errors = new LinkedHashMap<>(nrOfEdges);
        Map<CifBddEdge, BDD> guards = new LinkedHashMap<>(nrOfEdges);
        Map<CifBddEdge, BDD> updates = new LinkedHashMap<>(nrOfEdges);

        for (CifBddEdge edge: cifBddSpec.edges) {
            errors.put(edge, edge.error.id());
            guards.put(edge, edge.guard.id());
            updates.put(edge, edge.update.id());
        }

        // Also make a copy of the initialization and marked predicate, as data-based synthesis will free them.
        BDD initialPlantInv = cifBddSpec.initialPlantInv.id();
        BDD marked = cifBddSpec.marked.id();

        // Apply data-based synthesis.
        CifDataSynthesisSettings settings = (CifDataSynthesisSettings)cifBddSpec.settings;
        Verify.verifyNotNull(settings, "Expected CIF data-based synthesis settings, but got " + cifBddSpec.settings);
        CifDataSynthesisResult synthResult = CifDataSynthesis.synthesize(cifBddSpec, settings,
                new CifDataSynthesisTiming());

        // Restore the copy of the error, guard, and update predicates, since the originals were freed.
        for (CifBddEdge edge: cifBddSpec.edges) {
            edge.error = errors.get(edge);
            edge.guard = guards.get(edge);
            edge.update = updates.get(edge);
        }

        // Restore the initialization and marked predicate, since the original was freed.
        cifBddSpec.initialPlantInv = initialPlantInv;
        cifBddSpec.marked = marked;

        return synthResult;
    }

    /**
     * Computes the controlled behavior predicate of the given system. This computation will ensure that the error,
     * guard, and update predicates of the CIF/BDD edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The computed controlled behavior predicate.
     */
    public static BDD computeControlledBehavior(CifBddSpec cifBddSpec) {
        // Apply data-based synthesis.
        CifDataSynthesisResult synthResult = synthesize(cifBddSpec);

        // Update the guard of edges with a controllable event to consider the corresponding controlled system guard.
        // Store a copy of the uncontrolled guard, to restore later.
        Map<CifBddEdge, BDD> guards = new LinkedHashMap<>(cifBddSpec.edges.size());

        for (CifBddEdge edge: cifBddSpec.edges) {
            if (edge.event.getControllable()) {
                guards.put(edge, edge.guard.id());
                edge.guard = edge.guard.andWith(synthResult.outputGuards.get(edge.event));
            }
        }

        // Determine the initialization predicate.
        BDD initPredicate = cifBddSpec.initialPlantInv.id();
        if (synthResult.initialOutput != null) {
            initPredicate = initPredicate.andWith(synthResult.initialOutput);
        }

        // Construct the controlled behavior predicate of the system with a forward search.
        BDD controlledBehavior = reachForward(cifBddSpec, initPredicate, null, false);

        // Restore the uncontrolled system guards of the edges.
        for (CifBddEdge edge: cifBddSpec.edges) {
            if (edge.event.getControllable()) {
                edge.guard.free();
                edge.guard = guards.get(edge);
            }
        }

        return controlledBehavior;
    }

    /**
     * Applies the given CIF/BDD edge backward.
     *
     * @param edge The input CIF/BDD edge.
     * @param predicate The predicate to which to apply the edge, which will be {@link BDD#free freed} by this method.
     * @param restriction The predicate that indicates the upper bound on the reached states, or {@code null} in case
     *     there is no such upper bound (which is equivalent to providing the BDD 'true' as the upper bound).
     * @return The predicate describing the states that are backward reachable from the given edge.
     */
    public static BDD applyBackward(CifBddEdge edge, BDD predicate, BDD restriction) {
        // Make a copy of the error, guard, and update predicates of the edge, since edge application will free them.
        BDD error = edge.error.id();
        BDD guard = edge.guard.id();
        BDD update = edge.update.id();

        // Apply the given edge backward on the specified predicate.
        edge.initApply(false);
        edge.preApply(false, restriction);
        BDD result = edge.apply(predicate, false, false, restriction, true);
        edge.postApply(false);
        edge.cleanupApply();

        // Restore the copy of the error, guard, and update predicates of the edge, since the originals were freed.
        edge.error = error;
        edge.guard = guard;
        edge.update = update;

        return result;
    }
}
