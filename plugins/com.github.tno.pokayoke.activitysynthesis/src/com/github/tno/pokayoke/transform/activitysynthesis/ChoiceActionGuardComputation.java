
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.javabdd.BDD;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Compute the guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private final Map<String, BDD> uncontrolledSystemGuards;

    private final Map<String, BDD> controlledSystemGuards;

    private final Map<Place, BDD> stateInfo;

    public ChoiceActionGuardComputation(Map<String, BDD> uncontrolledSystemGuards,
            Map<String, BDD> controlledSystemGuards, Map<Place, BDD> stateInfo)
    {
        this.uncontrolledSystemGuards = uncontrolledSystemGuards;
        this.controlledSystemGuards = controlledSystemGuards;
        this.stateInfo = stateInfo;
    }

    /**
     * Computes choice guards for all choice arcs in the given Petri Net, i.e., arcs that go out of choice places.
     *
     * @param petriNet The input Petri Net.
     * @return A mapping from all choice arcs to their choice guards, as BDDs.
     */
    public Map<Arc, BDD> computeChoiceGuards(PetriNet petriNet) {
        Map<Arc, BDD> result = new LinkedHashMap<>();

        for (Page page: petriNet.getPages()) {
            result.putAll(computeChoiceGuards(page));
        }

        return result;
    }

    /**
     * Computes choice guards for all choice arcs in the given page, i.e., arcs that go out of choice places.
     *
     * @param page The input page.
     * @return A mapping from all choice arcs to their choice guards, as BDDs.
     */
    private Map<Arc, BDD> computeChoiceGuards(Page page) {
        Map<Arc, BDD> result = new LinkedHashMap<>();

        for (Object object: page.getObjects()) {
            if (object instanceof Arc arc && arc.getSource() instanceof Place place && place.getOutArcs().size() > 1) {
                result.put(arc, computeChoiceGuard(arc));
            }
        }

        return result;
    }

    /**
     * Computes the choice guard of the given arc.
     *
     * @param arc The arc for which to compute the choice guard. The source of this arc should be a {@link Place place},
     *     and the target of this arc should be a {@link Transition transition}.
     * @return The computed choice guard as a BDD predicate.
     */
    private BDD computeChoiceGuard(Arc arc) {
        Transition transition = (Transition)arc.getTarget();

        // Compute the extra (synthesized) condition for the target transition of the arc.
        BDD extraGuard = getExtraTransitionGuard(transition);

        // Further simplify the computed extra guard by the state information of all incoming places of the transition.
        BDD statePredicate = transition.getInArcs().stream().map(a -> stateInfo.get(a.getSource()).id())
                .reduce(BDD::andWith).get();
        BDD simplifiedExtraGuard = extraGuard.simplify(statePredicate);

        // Free all intermediate BDDs.
        extraGuard.free();
        statePredicate.free();

        return simplifiedExtraGuard;
    }

    /**
     * Gives the extra synthesized condition for the specified transition, that is not yet captured by the guard of the
     * action which this transition represents.
     *
     * @param transition The transition for which to obtain the extra guard.
     * @return The extra guard as a BDD predicate.
     */
    private BDD getExtraTransitionGuard(Transition transition) {
        String transitionName = transition.getName().getText();

        // Obtain the uncontrolled and controlled system guard for the given transition.
        BDD uncontrolledSystemGuard = uncontrolledSystemGuards.get(transitionName);
        BDD controlledSystemGuard = controlledSystemGuards.get(transitionName);

        // If a controlled system guard is available, simplify it with respect to the uncontrolled system guard.
        if (controlledSystemGuard != null) {
            return controlledSystemGuard.simplify(uncontrolledSystemGuard);
        } else {
            return uncontrolledSystemGuard.id();
        }
    }
}
