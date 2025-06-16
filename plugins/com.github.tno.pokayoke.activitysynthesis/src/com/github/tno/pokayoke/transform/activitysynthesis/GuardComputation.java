
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.bdd.conversion.BddToCif;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.settings.BddSimplify;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.datasynth.settings.FixedPointComputationsOrder;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;

/** Computes incoming and outgoing guards for synthesized UML activities. */
public class GuardComputation {
    /** The UML-to-CIF translator to use for guard computation. */
    private final UmlToCifTranslator translator;

    /**
     * Constructs a new {@link GuardComputation}.
     *
     * @param translator The UML-to-CIF translator to use for guard computation.
     */
    public GuardComputation(UmlToCifTranslator translator) {
        this.translator = translator;
    }

    public void computeGuards(Specification specification) {
        // Obtain the mapping from UML (activity) elements to all the CIF start events created for them.
        Map<RedefinableElement, List<Event>> startEventMap = reverse(translator.getStartEventMap());

        // Helper function for obtaining the single CIF start event of a given UML element.
        Function<RedefinableElement, Event> getSingleStartEvent = element -> {
            List<Event> startEvents = startEventMap.get(element);
            int nrOfStartEvents = startEvents.size();
            Verify.verify(nrOfStartEvents == 1, "Expected a single start event, but got " + nrOfStartEvents + ".");
            return startEvents.get(0);
        };

        // Obtain the mapping from all incoming/outgoing control flow pairs of translated OR-type activity nodes,
        // to the CIF start events created for these control flow pairs.
        BiMap<Pair<ActivityEdge, ActivityEdge>, Event> activityOrNodeMapping = translator.getActivityOrNodeMapping();

        // Define the configuration for performing data-based synthesis and symbolic reachability searches.
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();
        settings.setDoForwardReach(true);
        settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.REACH_NONBLOCK_CTRL);
        settings.setBddSimplifications(EnumSet.noneOf(BddSimplify.class));

        // Convert the CIF specification to a CIF/BDD specification.
        CifToBddConverter.preprocess(specification, settings.getWarnOutput(), settings.getDoPlantsRefReqsWarn());
        BDDFactory factory = CifToBddConverter.createFactory(settings, new ArrayList<>(), new ArrayList<>());
        CifToBddConverter converter = new CifToBddConverter("Guard computation");
        CifBddSpec cifBddSpec = converter.convert(specification, settings, factory);

        // Helper function for obtaining the corresponding single CIF/BDD edge of a CIF event.
        Function<Event, CifBddEdge> getCorrespondingEdge = event -> {
            List<CifBddEdge> edges = cifBddSpec.eventEdges.get(event);
            int nrOfEdges = edges.size();
            Verify.verify(nrOfEdges == 1, "Expected a single CIF/BDD edge, but got " + nrOfEdges + ".");
            return edges.get(0);
        };

        // Find all controlled system states.
        BDD controlledStates = GuardComputationHelper.computeControlledBehavior(cifBddSpec);

        // Obtain the set of all internal BDD variables.
        BDDVarSet internalVars = getInternalBDDVars(cifBddSpec);

        // Compute guards for every activity node, and put these on the appropriate control flows in the activity.
        for (ActivityNode node: translator.getActivity().getNodes()) {
            // Do a case distinction on the type of activity node, and compute guards according to this node type.
            if (node instanceof DecisionNode) {
                // Compute an incoming guard for every outgoing control flow of the decision node.
                for (Pair<ActivityEdge, ActivityEdge> pair: getControlFlowPairs(node)) {
                    CifBddEdge edge = getCorrespondingEdge.apply(activityOrNodeMapping.get(pair));
                    BDD guard = computeGuard(edge, controlledStates, internalVars);
                    PokaYokeUmlProfileUtil.setIncomingGuard(pair.right, toUmlGuard(guard, cifBddSpec));
                }
            } else if (node instanceof MergeNode) {
                // Compute an outgoing guard for every incoming control flow of the merge node.
                for (Pair<ActivityEdge, ActivityEdge> pair: getControlFlowPairs(node)) {
                    CifBddEdge edge = getCorrespondingEdge.apply(activityOrNodeMapping.get(pair));
                    BDD guard = computeGuard(edge, controlledStates, internalVars);
                    PokaYokeUmlProfileUtil.setOutgoingGuard(pair.left, toUmlGuard(guard, cifBddSpec));
                }
            } else if (node instanceof ForkNode) {
                // Compute an outgoing guard for the (single) incoming control flow of the fork node.
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(node));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                PokaYokeUmlProfileUtil.setOutgoingGuard(node.getIncomings().get(0), toUmlGuard(guard, cifBddSpec));
            } else if (node instanceof JoinNode) {
                // Compute an incoming guard for the (single) outgoing control flow of the join node.
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(node));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                PokaYokeUmlProfileUtil.setIncomingGuard(node.getOutgoings().get(0), toUmlGuard(guard, cifBddSpec));
            } else if (node instanceof InitialNode) {
                // Compute an incoming guard for every outgoing control flow of the initial node.
                for (ActivityEdge outgoing: node.getOutgoings()) {
                    BDD tokenConstraint = getTokenConstraint(outgoing, cifBddSpec);
                    BDD uncontrolledGuard = cifBddSpec.initialPlantInv.id().andWith(tokenConstraint);
                    BDD controlledGuard = uncontrolledGuard.and(controlledStates);
                    BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);
                    controlledGuard.free();
                    PokaYokeUmlProfileUtil.setIncomingGuard(node.getOutgoings().get(0), toUmlGuard(guard, cifBddSpec));
                }
            } else if (node instanceof ActivityFinalNode) {
                // Compute an outgoing guard for every incoming control flow of the final node.
                for (ActivityEdge incoming: node.getIncomings()) {
                    BDD tokenConstraint = getTokenConstraint(incoming, cifBddSpec);
                    BDD uncontrolledGuard = controlledStates.id().andWith(tokenConstraint);
                    BDD controlledGuard = uncontrolledGuard.and(cifBddSpec.marked);
                    BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);
                    controlledGuard.free();
                    PokaYokeUmlProfileUtil.setOutgoingGuard(node.getIncomings().get(0), toUmlGuard(guard, cifBddSpec));
                }
            } else if (node instanceof CallBehaviorAction || node instanceof OpaqueAction) {
                // Compute an outgoing guard for the (single) incoming control flow of the action node.
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(node));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                PokaYokeUmlProfileUtil.setOutgoingGuard(node.getIncomings().get(0), toUmlGuard(guard, cifBddSpec));
            } else {
                throw new RuntimeException("Unknown activity node: " + node);
            }
        }
    }

    /**
     * Computes a guard for the given CIF/BDD edge.
     *
     * @param edge The CIF/BDD edge to compute the guard for.
     * @param controlledStates The set of all controlled system states.
     * @param internalVars The set of BDD variables from which the computed guard should be independent.
     * @return The computed guard.
     */
    private BDD computeGuard(CifBddEdge edge, BDD controlledStates, BDDVarSet internalVars) {
        // We compute guards inductively: assuming we execute the activity in a controlled manner up to the execution of
        // 'edge', we now have to compute the (extra) guard that ensures that the execution of 'edge' ends up in a
        // controlled system state again. If we do this consistently, then every activity execution will be safe (under
        // the guards that we compute for the activity) by induction on the length of the execution trace.

        // Thus, let us consider all controlled system states where the uncontrolled system guard of 'edge' holds. Of
        // these system states, we must only keep the ones from which the application of 'edge' ends up in a controlled
        // system state again. We can compute the guard of 'edge' as described above, from these two sets of states.
        BDD uncontrolledGuard = controlledStates.and(edge.guard);
        BDD controlledGuard = GuardComputationHelper.applyBackward(edge, controlledStates.id(), controlledStates);
        BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);

        // Free intermediate BDDs.
        controlledGuard.free();
        uncontrolledGuard.free();

        return guard;
    }

    /**
     * Tries to compute a guard as a BDD predicate that is equivalent to
     * {@code controlledGuard.simplify(uncontrolledGuard)}, but does not depend on any variables in
     * {@code internalVars}. If such a predicate does not exist, a runtime exception will be thrown instead.
     *
     * @param uncontrolledGuard The uncontrolled system guard to simplify against.
     * @param controlledGuard The controlled system guard to simplify.
     * @param internalVars The set of BDD variables from which the computed predicate should be independent.
     * @return The computed guard.
     */
    private BDD computeGuard(BDD uncontrolledGuard, BDD controlledGuard, BDDVarSet internalVars) {
        // Try to compute a guard that is independent of internal variables.
        BDD abstractControlled = controlledGuard.exist(internalVars);
        BDD abstractUncontrolled = uncontrolledGuard.exist(internalVars);
        BDD guard = abstractControlled.simplify(abstractUncontrolled);
        abstractControlled.free();
        abstractUncontrolled.free();

        // Sanity check: the computed guard indeed does not depend on internal variables.
        BDD abstractGuard = guard.exist(internalVars);
        Verify.verify(abstractGuard.equals(guard), "Expected the computed guard to not depend on internal variables.");
        abstractGuard.free();

        // Make sure that the computed guard is still correct.
        BDD guardCheck = uncontrolledGuard.and(guard);

        if (!guardCheck.equals(controlledGuard)) {
            throw new RuntimeException(
                    "Expected the computed guard to be correct, without depending on internal variables.");
        }

        guardCheck.free();

        // Make sure the computed guard is satisfiable. Note that it should never be possible to compute a 'false'
        // guard. Otherwise, there must be a safe action sequence that is now disabled with respect to the synthesized
        // supervisor that was used earlier, to synthesize the activity structure. However, since we've now made all CIF
        // events controllable for guard computation, we gave synthesis full control over the activity execution. Hence,
        // there must then be something in the structure of the synthesized activity that disables the action sequence.
        // But the activity structure follows from the state space of the earlier synthesized supervisor. Contradiction.
        if (guard.isZero()) {
            throw new RuntimeException("Expected the computed guard to be satisfiable, but got 'false'.");
        }

        return guard;
    }

    /**
     * Computes a BDD predicate expressing that the given UML control flow holds a token.
     *
     * @param controlFlow The input UML control flow.
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The computed BDD predicate.
     */
    private BDD getTokenConstraint(ActivityEdge controlFlow, CifBddSpec cifBddSpec) {
        // Obtain the (Boolean) CIF variable that corresponds to the given UML control flow.
        DiscVariable variable = translator.getControlFlowMap().get(controlFlow);

        // Construct a CIF constraint expressing that the obtained CIF variable must be true.
        Expression constraint = CifConstructors.newDiscVariableExpression(null, CifConstructors.newBoolType(),
                variable);

        // Convert this CIF constraint to a BDD predicate.
        try {
            return CifToBddConverter.convertPred(constraint, false, cifBddSpec);
        } catch (UnsupportedPredicateException e) {
            throw new RuntimeException("Unsupported predicate: " + e.getMessage());
        }
    }

    /**
     * Computes all combinations of incoming and outgoing control flows of the given UML activity node.
     *
     * @param node The input UML node.
     * @return The set of all incoming/outgoing control flow pairs of the given UML activity node.
     */
    private Set<Pair<ActivityEdge, ActivityEdge>> getControlFlowPairs(ActivityNode node) {
        Set<Pair<ActivityEdge, ActivityEdge>> pairs = new LinkedHashSet<>();

        for (ActivityEdge incoming: node.getIncomings()) {
            for (ActivityEdge outgoing: node.getOutgoings()) {
                pairs.add(Pair.pair(incoming, outgoing));
            }
        }

        return pairs;
    }

    /**
     * Gives the set of all BDD variables that are internal, i.e., not created for user-defined properties in UML.
     *
     * @param cifBddSpec The input CIF/BDD specification.
     * @return The set of all BDD variables that are internal, i.e., not created for user-defined properties in UML.
     */
    private BDDVarSet getInternalBDDVars(CifBddSpec cifBddSpec) {
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
    private BDDVarSet getExternalBDDVars(CifBddSpec cifBddSpec) {
        return getVarSetOf(translator.getPropertyMap().values(), cifBddSpec);
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
    private String toUmlGuard(BDD bdd, CifBddSpec cifBddSpec) {
        // Convert BDD to a textual representation closely resembling CIF ASCII syntax.
        String text = CifTextUtils.exprToStr(BddToCif.bddToCifPred(bdd, cifBddSpec));

        // Turn the textual representation into a SynthML-compatible expression.
        // XXX a string replacement is not very robust. would be better to translate the CIF expression tree ourselves.
        String plantPrefix = translator.getPlantName() + ".";
        return text.replaceAll(plantPrefix, "");
    }

    /**
     * Helper function, to reverse the given mapping.
     *
     * @param <T> The domain of map to reverse.
     * @param <U> The codomain the the map to reverse.
     * @param map The map to reverse.
     * @return The reversed map.
     */
    private static <T, U> Map<U, List<T>> reverse(Map<T, U> map) {
        Map<U, List<T>> result = new LinkedHashMap<>();

        for (Entry<T, U> entry: map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), e -> new ArrayList<>()).add(entry.getKey());
        }

        return result;
    }
}
