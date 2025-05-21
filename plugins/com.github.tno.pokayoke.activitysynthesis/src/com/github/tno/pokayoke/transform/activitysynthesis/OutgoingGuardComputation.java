
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.settings.BddSimplify;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.datasynth.settings.FixedPointComputationsOrder;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

public class OutgoingGuardComputation extends GuardComputation {
    private final UmlToCifTranslator translator;

    public OutgoingGuardComputation(UmlToCifTranslator translator) {
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

        // Find all uncontrolled and controlled system states.
        BDD uncontrolledStates = computeUncontrolledBehavior(cifBddSpec);
        BDD controlledStates = computeControlledBehavior(cifBddSpec);

        // Obtain the set of all internal BDD variables.
        BDDVarSet internalVars = getInternalBDDVars(cifBddSpec);

        // TODO obtain CIF event maps.
        Map<RedefinableElement, List<Event>> startEventMap = reverse(translator.getStartEventMap());
        Map<Event, List<Event>> nonDeterministicEventMap = translator.getNonDeterministicEvents();

        // TODO helper function
        Function<Event, CifBddEdge> getCorrespondingEdge = event -> {
            List<CifBddEdge> edges = cifBddSpec.eventEdges.get(event);
            Verify.verify(edges.size() == 1);
            return edges.get(0);
        };

        // TODO what about intermediate/virtual control flows for non-atomic actions?

        // TODO explain why guards can't become 'false'.

        // TODO compute outgoing guard for every control flow.
        for (ActivityEdge controlFlow: translator.getActivity().getEdges()) {
            ActivityNode target = controlFlow.getTarget();

            // TODO not sure about this. We still remove a token then....
            // We will not execute the activity final node.
            if (target instanceof ActivityFinalNode) {

                // TODO probably we must consider all controlled system states where there is a token on 'controlFlow'
                // in which the activity postcondition holds. I.e., in which you are marked. From those states you
                // can finish the activity. Same with uncontrolled states, and then simplify.
                continue;
            }

            System.out.println("Computing outgoing guard for control flow to: " + target);

            // TODO compute guard
            BDD guardUncontrolled = cifBddSpec.factory.zero();
            BDD guardControlled = cifBddSpec.factory.zero();

            // Apply 'target' backwards to find out in what states 'controlFlow' can lose its token in the controlled
            // and the uncontrolled system. In case 'target' is atomic and non-deterministic, we need to do two backward
            // steps: for the end+start event. Otherwise, we only need to so the start event.

            // TODO also callable for control nodes?
            // If 'target' is atomic and non-deterministic, then we must consider both start and end edges.
            if (PokaYokeUmlProfileUtil.isAtomic(target) && !PokaYokeUmlProfileUtil.isDeterministic(target)) {
                for (Event startEvent: startEventMap.get(target)) {
                    CifBddEdge startEdge = getCorrespondingEdge.apply(startEvent);

                    for (Event endEvent: nonDeterministicEventMap.get(startEvent)) {
                        CifBddEdge endEdge = getCorrespondingEdge.apply(endEvent);

                        guardUncontrolled = guardUncontrolled.orWith(applyBackward(startEdge,
                                applyBackward(endEdge, uncontrolledStates.id(), uncontrolledStates),
                                uncontrolledStates));
                        guardControlled = guardControlled.orWith(applyBackward(startEdge,
                                applyBackward(endEdge, controlledStates.id(), controlledStates), controlledStates));
                    }
                }
            } else {
                for (Event startEvent: startEventMap.get(target)) {
                    CifBddEdge startEdge = getCorrespondingEdge.apply(startEvent);

                    guardUncontrolled = guardUncontrolled
                            .orWith(applyBackward(startEdge, uncontrolledStates.id(), uncontrolledStates));
                    guardControlled = guardControlled
                            .orWith(applyBackward(startEdge, controlledStates.id(), controlledStates));
                }
            }

            // Try to compute an outgoing guard that is independent of internal variables.
            BDD abstractControlled = guardControlled.exist(internalVars);
            BDD abstractUncontrolled = guardUncontrolled.exist(internalVars);
            BDD outgoingGuard = abstractControlled.simplify(abstractUncontrolled);
            abstractControlled.free();
            abstractUncontrolled.free();

            // TODO two checks that the guard is correct.

            // Free intermediate BDDs.
            guardUncontrolled.free();
            guardControlled.free();

            System.out.println("Outgoing guard: " + bddToString(outgoingGuard, cifBddSpec));
            System.out.println();
        }

        // TODO Auto-generated method stub
        return null;
    }

    private static <T, U> Map<U, List<T>> reverse(Map<T, U> map) {
        Map<U, List<T>> result = new LinkedHashMap<>();

        for (Entry<T, U> entry: map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), e -> new ArrayList<>()).add(entry.getKey());
        }

        return result;
    }
}
