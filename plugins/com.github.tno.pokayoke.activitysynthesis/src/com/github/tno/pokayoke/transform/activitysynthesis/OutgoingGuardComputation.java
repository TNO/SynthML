
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
                System.out.println("Computing outgoing guard for control flow to: " + controlFlow.getTarget());

                // TODO
                BDD uncontrolledConstraint = getTokenRemovalConstraint(uncontrolledStates, controlFlow, cifBddSpec);
                BDD controlledConstraint = getTokenRemovalConstraint(controlledStates, controlFlow, cifBddSpec);

                // Try to compute an outgoing guard that is independent of internal variables.
                BDD abstractControlled = controlledConstraint.exist(internalVars);
                BDD abstractUncontrolled = uncontrolledConstraint.exist(internalVars);
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

    // TODO
    private BDD getTokenRemovalConstraint(BDD states, ControlFlow controlFlow, CifBddSpec cifBddSpec) {
        // Partition 'states' into two parts: one where the control flow holds a token, and one where it doesn't.
        BDD tokenConstraint = getTokenConstraint(controlFlow, cifBddSpec);
        BDD statesWithToken = states.and(tokenConstraint);
        BDD statesWithoutToken = states.id().andWith(tokenConstraint.not());
        tokenConstraint.free();

        // Find all states in 'statesWithToken' in which a transition is enabled that leads to 'statesWithoutToken'.
        BDD exitStates = cifBddSpec.factory.zero();

        for (CifBddEdge edge: cifBddSpec.edges) {
            // TODO special case: end events of atomic nondeterministic actions.
            BDD sourceStates = applyBackward(edge, statesWithoutToken.id(), states);
            exitStates = exitStates.orWith(sourceStates.andWith(statesWithToken.id()));
        }

        // Free intermediate BDDs.
        statesWithToken.free();
        statesWithoutToken.free();

        return exitStates;
    }

    private BDD getTokenConstraint(ControlFlow controlFlow, CifBddSpec cifBddSpec) {
        // Construct a CIF predicate expressing that the given control flow must hold a token.
        Expression constraint = CifConstructors.newDiscVariableExpression(null, CifConstructors.newBoolType(),
                translator.getControlFlowMap().get(controlFlow));

        // Convert the CIF constraint to a BDD predicate.
        try {
            return CifToBddConverter.convertPred(constraint, false, cifBddSpec);
        } catch (UnsupportedPredicateException e) {
            throw new RuntimeException("Unsupported predicate: " + e.getMessage());
        }
    }
}
