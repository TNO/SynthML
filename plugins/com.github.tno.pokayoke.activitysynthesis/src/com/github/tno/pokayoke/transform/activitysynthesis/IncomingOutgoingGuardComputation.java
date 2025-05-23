
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
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
import org.eclipse.uml2.uml.ControlFlow;
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
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;

//TODO what about intermediate/virtual control flows for non-atomic actions?
// -> CIF data-based synthesis takes care of that. We now take over the computed guards more-or-less directly.

// TODO explain why guards can't become 'false'.
// -> The synthesized activity should have all safe action sequences, plus perhaps some more since we got rid of data
// for PN synthesis. The goal of guard computation is to get rid of these unsafe action sequences, and keep only the
// safe ones. We do this by translating the activity to CIF, perform data-based synthesis again, and process the
// synthesized guards for activity node execution. During the translation, we make all CIF events controllable. So we
// give the second synthesis round full control over when activity nodes are executed. If some safe action sequence gets
// disabled anyway, then this sequence must also not have been possible after the first round of synthesis, since the
// activity that we work with was a result of that round of synthesis.

public class IncomingOutgoingGuardComputation extends GuardComputation {
    private final UmlToCifTranslator translator;

    public IncomingOutgoingGuardComputation(UmlToCifTranslator translator) {
        this.translator = translator;
    }

    @Override
    public UmlToCifTranslator getTranslator() {
        return translator;
    }

    @Override
    public Map<ControlFlow, BDD> computeGuards(Specification specification) {
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

        // Find all controlled system states.
        BDD controlledStates = computeControlledBehavior(cifBddSpec);

        // Obtain the set of all internal BDD variables.
        BDDVarSet internalVars = getInternalBDDVars(cifBddSpec);

        // TODO doc obtain CIF event maps.
        Map<RedefinableElement, List<Event>> startEventMap = reverse(translator.getStartEventMap());

        // TODO doc obtain activity or node mapping (incoming, outgoing)-transition pairs.
        BiMap<Pair<ActivityEdge, ActivityEdge>, Event> activityOrNodeMapping = translator.getActivityOrNodeMapping();

        // TODO helper function
        Function<RedefinableElement, Event> getSingleStartEvent = element -> {
            List<Event> startEvents = startEventMap.get(element);
            Verify.verify(startEvents.size() == 1);
            return startEvents.get(0);
        };

        // TODO helper function
        Function<Event, CifBddEdge> getCorrespondingEdge = event -> {
            List<CifBddEdge> edges = cifBddSpec.eventEdges.get(event);
            Verify.verify(edges.size() == 1);
            return edges.get(0);
        };

        System.out.println("Computing guards for: " + translator.getActivity());

        for (ActivityNode node: translator.getActivity().getNodes()) {
            System.out.println("Computing guard for: " + node);

            if (node instanceof DecisionNode decisionNode) {
                for (Pair<ActivityEdge, ActivityEdge> pair: getControlFlowPairs(decisionNode)) {
                    CifBddEdge edge = getCorrespondingEdge.apply(activityOrNodeMapping.get(pair));
                    BDD guard = computeGuard(edge, controlledStates, internalVars);
                    System.out.println("incoming guard: " + bddToString(guard, cifBddSpec));
                }
            } else if (node instanceof MergeNode mergeNode) {
                for (Pair<ActivityEdge, ActivityEdge> pair: getControlFlowPairs(mergeNode)) {
                    CifBddEdge edge = getCorrespondingEdge.apply(activityOrNodeMapping.get(pair));
                    BDD guard = computeGuard(edge, controlledStates, internalVars);
                    System.out.println("outgoing guard: " + bddToString(guard, cifBddSpec));
                }
            } else if (node instanceof ForkNode forkNode) {
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(forkNode));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                System.out.println("outgoing guard: " + bddToString(guard, cifBddSpec));
            } else if (node instanceof JoinNode joinNode) {
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(joinNode));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                System.out.println("incoming guard: " + bddToString(guard, cifBddSpec));
            } else if (node instanceof InitialNode) {
                for (ActivityEdge outgoing: node.getOutgoings()) {
                    BDD tokenConstraint = getTokenConstraint(outgoing, cifBddSpec);
                    BDD uncontrolledGuard = cifBddSpec.initialPlantInv.id().andWith(tokenConstraint);
                    BDD controlledGuard = uncontrolledGuard.and(controlledStates);
                    BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);
                    controlledGuard.free();
                    System.out.println("incoming guard: " + bddToString(guard, cifBddSpec));
                }
            } else if (node instanceof ActivityFinalNode) {
                for (ActivityEdge incoming: node.getIncomings()) {
                    BDD tokenConstraint = getTokenConstraint(incoming, cifBddSpec);
                    BDD uncontrolledGuard = controlledStates.id().andWith(tokenConstraint);
                    BDD controlledGuard = uncontrolledGuard.and(cifBddSpec.marked);
                    BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);
                    controlledGuard.free();
                    System.out.println("outgoing guard: " + bddToString(guard, cifBddSpec));
                }
            } else if (node instanceof CallBehaviorAction || node instanceof OpaqueAction) {
                CifBddEdge edge = getCorrespondingEdge.apply(getSingleStartEvent.apply(node));
                BDD guard = computeGuard(edge, controlledStates, internalVars);
                System.out.println("outgoing guard: " + bddToString(guard, cifBddSpec));
            } else {
                throw new RuntimeException("Unknown activity node: " + node);
            }

            System.out.println();
        }

        // TODO Auto-generated method stub
        return null;
    }

    // TODO doc Compute guard for the specified CIF/BDD edge.
    private BDD computeGuard(CifBddEdge edge, BDD controlledStates, BDDVarSet internalVars) {
        // We compute the guard for 'edge' inductively: assuming we execute the activity in a controlled manner up to
        // the execution of 'edge', we now have to compute the (extra) guard that ensures that the execution of 'edge'
        // ends up in a controlled state again.
        BDD uncontrolledGuard = controlledStates.and(edge.guard);
        BDD controlledGuard = applyBackward(edge, controlledStates.id(), controlledStates);
        BDD guard = computeGuard(uncontrolledGuard, controlledGuard, internalVars);
        controlledGuard.free();
        uncontrolledGuard.free();
        return guard;
    }

    // TODO doc Try compute a guard that is independent of internal variables
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

        return guard;
    }

    // TODO doc
    private BDD getTokenConstraint(ActivityEdge controlFlow, CifBddSpec cifBddSpec) {
        // Obtain the (Boolean) CIF variable that corresponds to the given control flow.
        DiscVariable variable = translator.getControlFlowMap().get(controlFlow);

        // Construct a CIF constraint expressing that the obtained CIF variable must be true.
        Expression constraint = CifConstructors.newDiscVariableExpression(null, CifConstructors.newBoolType(),
                variable);

        // Convert the CIF constraint to a BDD predicate.
        try {
            return CifToBddConverter.convertPred(constraint, false, cifBddSpec);
        } catch (UnsupportedPredicateException e) {
            throw new RuntimeException("Unsupported predicate: " + e.getMessage());
        }
    }

    // TODO javadoc
    private Set<Pair<ActivityEdge, ActivityEdge>> getControlFlowPairs(ActivityNode node) {
        Set<Pair<ActivityEdge, ActivityEdge>> pairs = new LinkedHashSet<>();

        for (ActivityEdge incoming: node.getIncomings()) {
            for (ActivityEdge outgoing: node.getOutgoings()) {
                pairs.add(Pair.pair(incoming, outgoing));
            }
        }

        return pairs;
    }

    // TODO doc
    private static <T, U> Map<U, List<T>> reverse(Map<T, U> map) {
        Map<U, List<T>> result = new LinkedHashMap<>();

        for (Entry<T, U> entry: map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), e -> new ArrayList<>()).add(entry.getKey());
        }

        return result;
    }
}
