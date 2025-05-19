
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.settings.BddSimplify;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.datasynth.settings.FixedPointComputationsOrder;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDVarSet;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;

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
        CifToBddConverter converter = new CifToBddConverter("Outgoing guard computation");
        CifBddSpec cifBddSpec = converter.convert(specification, settings, factory);

        // Find all uncontrolled and controlled system states.
        BDD uncontrolledStates = computeUncontrolledBehavior(cifBddSpec);
        BDD controlledStates = computeControlledBehavior(cifBddSpec);

        // Obtain the set of all internal BDD variables.
        BDDVarSet internalVars = getInternalBDDVars(cifBddSpec);

        // TODO obtain CIF start event map.
        Map<Event, RedefinableElement> startEventMap = translator.getStartEventMap();

        // TODO compute outgoing guard for every control flow.
        for (ActivityEdge edge: translator.getActivity().getEdges()) {
            if (edge instanceof ControlFlow controlFlow) {
                ActivityNode target = controlFlow.getTarget();

                System.out.println("Computing outgoing guard for control flow to: " + target);

                // TODO compute guard
                BDD guardUncontrolled = cifBddSpec.factory.zero();
                BDD guardControlled = cifBddSpec.factory.zero();

                // TODO note that 'target' may have multiple CIF start events. We should consider them all.
                for (CifBddEdge cifEdge: cifBddSpec.edges) {
                    // TODO properly handle end events of atomic non-deteterministic actions.

                    // What can 'cifEdge' be:
                    // - The start+end event of an atomic deterministic action -> take edge (case 1)
                    // - The start event of an atomic non-deterministic action -> don't take edge
                    // - The start event of a non-atomic action -> take edge (case 1)
                    // - The end event of an atomic non-deterministic action -> take start + end event  (case 2)
                    // - The end event of a non-atomic action -> don't take edge

                    // So:
                    // - if 'cifEdge' is the start event of an atomic deterministic or non-atomic action, take the edge
                    // - if 'cifEdge' is the end event of an atomic non-deterministic action, take both start+end edge
                    // - otherwise, don't take the edge

                    if (cifEdge.event.getControllable() && startEventMap.get(cifEdge.event).equals(target)) {
                        guardUncontrolled = guardUncontrolled
                                .orWith(applyBackward(cifEdge, uncontrolledStates.id(), uncontrolledStates));
                        guardControlled = guardControlled
                                .orWith(applyBackward(cifEdge, controlledStates.id(), controlledStates));
                    }
                }

                // Try to compute an outgoing guard that is independent of internal variables.
                BDD abstractControlled = guardControlled.exist(internalVars);
                BDD abstractUncontrolled = guardUncontrolled.exist(internalVars);
                BDD outgoingGuard = abstractControlled.simplify(abstractUncontrolled);
                abstractControlled.free();
                abstractUncontrolled.free();

                System.out.println("Outgoing guard: " + bddToString(outgoingGuard, cifBddSpec));
                System.out.println();
            }
        }

        // TODO Auto-generated method stub
        return null;
    }
}
