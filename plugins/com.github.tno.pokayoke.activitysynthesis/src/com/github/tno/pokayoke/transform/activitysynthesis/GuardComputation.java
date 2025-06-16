
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.bdd.conversion.BddToCif;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.utils.CifBddReachability;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.CifDataSynthesis;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisTiming;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDVarSet;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;

public abstract class GuardComputation {
    public abstract UmlToCifTranslator getTranslator();

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
    protected BDD reachForward(CifBddSpec cifBddSpec, BDD predicate, BDD restriction, boolean badStates) {
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
     * Computes the set of all backward reachable states from the given predicate, using all edges in the given CIF/BDD
     * specification. This method will ensure that the error, guard, and update predicates of the edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @param predicate The predicate from which to start exploring, which will be {@link BDD#free freed} afterwards.
     * @param restriction The predicate that indicates the upper bound on the reachable states, or {@code null} in case
     *     there is no such upper bound (which is equivalent to providing the BDD 'true' as the upper bound).
     * @param badStates Whether the given predicate represents bad states ({@code true}) or good states ({@code false}).
     * @return The set of backward reachable states, as a BDD predicate.
     */
    protected BDD reachBackward(CifBddSpec cifBddSpec, BDD predicate, BDD restriction, boolean badStates) {
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

        // Compute all backward reachable states.
        cifBddSpec.edges.forEach(edge -> edge.initApply(false));
        CifBddReachability reach = new CifBddReachability(cifBddSpec, "backward search", "predicate",
                restriction != null ? "restriction" : null, restriction, badStates, false, true, true, false);
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
    protected CifDataSynthesisResult synthesize(CifBddSpec cifBddSpec) {
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
     * Computes the uncontrolled behavior predicate of the given system. This computation will ensure that the error,
     * guard, and update predicates of the CIF/BDD edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The computed uncontrolled behavior predicate.
     */
    protected BDD computeUncontrolledBehavior(CifBddSpec cifBddSpec) {
        return reachForward(cifBddSpec, cifBddSpec.initialPlantInv.id(), null, false);
    }

    /**
     * Computes the controlled behavior predicate of the given system. This computation will ensure that the error,
     * guard, and update predicates of the CIF/BDD edges are not freed.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The computed controlled behavior predicate.
     */
    protected BDD computeControlledBehavior(CifBddSpec cifBddSpec) {
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
     * Applies the given CIF/BDD edge forward.
     *
     * @param edge The input CIF/BDD edge.
     * @param predicate The predicate to which to apply the edge, which will be {@link BDD#free freed} by this method.
     * @param restriction The predicate that indicates the upper bound on the reached states, or {@code null} in case
     *     there is no such upper bound (which is equivalent to providing the BDD 'true' as the upper bound).
     * @return The predicate describing the states that are forward reachable from the given edge.
     */
    protected BDD applyForward(CifBddEdge edge, BDD predicate, BDD restriction) {
        // Make a copy of the error, guard, and update predicates of the edge, since edge application will free them.
        BDD error = edge.error.id();
        BDD guard = edge.guard.id();
        BDD update = edge.update.id();

        // Apply the given edge forward on the specified predicate.
        edge.initApply(true);
        edge.preApply(true, restriction);
        BDD result = edge.apply(predicate, false, true, restriction, true);
        edge.postApply(true);
        edge.cleanupApply();

        // Restore the copy of the error, guard, and update predicates of the edge, since the originals were freed.
        edge.error = error;
        edge.guard = guard;
        edge.update = update;

        return result;
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
    protected BDD applyBackward(CifBddEdge edge, BDD predicate, BDD restriction) {
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

    /**
     * Gives the set of all BDD variables that are internal, i.e., not created for user-defined properties in UML.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The set of all BDD variables that are internal, i.e., not created for user-defined properties in UML.
     */
    protected final BDDVarSet getInternalBDDVars(CifBddSpec cifBddSpec) {
        // Obtain the (Java) sets of all BDD variables, and of all internal BDD variables.
        Set<Integer> allVars = Arrays.stream(cifBddSpec.varSetOld.toArray()).boxed()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> externalVars = Arrays.stream(getExternalBDDVars(cifBddSpec).toArray()).boxed()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Determine the set of all internal BDD variables (i.e., all variables except the external ones).
        Set<Integer> internalVars = Sets.difference(allVars, externalVars);

        // Convert this set of internal variables to a 'BDDVarSet', and return it.
        return cifBddSpec.factory.makeSet(internalVars.stream().mapToInt(var -> var).toArray());
    }

    /**
     * Gives the set of all BDD variables that are created for user-defined properties in UML.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The set of all BDD variables that are created for user-defined properties in UML.
     */
    protected final BDDVarSet getExternalBDDVars(CifBddSpec cifBddSpec) {
        return getVarSetOf(getTranslator().getPropertyMap().values(), cifBddSpec);
    }

    /**
     * Gives the set of BDD variables representing the values of the given collection of CIF variables.
     *
     * @param variables The input CIF variables.
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The set of BDD variables representing the values of the given collection of CIF variables.
     */
    private BDDVarSet getVarSetOf(Collection<DiscVariable> variables, CifBddSpec cifBddSpec) {
        return variables.stream().map(variable -> getVarSetOf(variable, cifBddSpec)).reduce(BDDVarSet::unionWith)
                .orElse(cifBddSpec.factory.emptySet());
    }

    /**
     * Gives the set of BDD variables representing the values of the given CIF variable.
     *
     * @param variable The input CIF variable.
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The set of BDD variables representing the values of the given CIF variable.
     */
    private BDDVarSet getVarSetOf(DiscVariable variable, CifBddSpec cifBddSpec) {
        int index = CifToBddConverter.getDiscVarIdx(cifBddSpec.variables, variable);
        Verify.verify(0 <= index, "Expected a non-negative variable index.");
        return cifBddSpec.variables[index].domain.set();
    }

    /**
     * Computes a SynthML-compatible guard for the given BDD.
     *
     * @param bdd The BDD to convert to a SynthML-compatible guard.
     * @param cifBddSpec The CIF/BDD specification.
     * @return The SynthML-compatible guard.
     */
    protected String toUmlGuard(BDD bdd, CifBddSpec cifBddSpec) {
        // Convert BDD to a textual representation closely resembling CIF ASCII syntax.
        String text = CifTextUtils.exprToStr(BddToCif.bddToCifPred(bdd, cifBddSpec));

        // Turn the textual representation into a SynthML-compatible expression.
        // XXX a string replacement is not very robust. would be better to translate the CIF expression tree ourselves.
        String plantPrefix = getTranslator().getPlantName() + ".";
        return text.replaceAll(plantPrefix, "");
    }
}
