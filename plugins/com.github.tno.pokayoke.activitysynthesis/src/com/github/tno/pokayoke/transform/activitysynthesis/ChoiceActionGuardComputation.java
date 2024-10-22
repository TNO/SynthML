
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.javabdd.BDD;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Compute the guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private final Map<Event, BDD> uncontrolledSystemGuards;

    private final Map<Event, BDD> controlledSystemGuards;

    private final Map<Place, BDD> stateInfo;

    public ChoiceActionGuardComputation(Map<Event, BDD> uncontrolledSystemGuards,
            Map<Event, BDD> controlledSystemGuards, Map<Place, BDD> stateInfo)
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
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected the Petri Net to have exactly one page.");
        return computeChoiceGuards(petriNet.getPages().get(0));
    }

    /**
     * Computes choice guards for all choice arcs in the given page, i.e., arcs that go out of choice places.
     *
     * @param page The input page.
     * @return A mapping from all choice arcs to their choice guards, as BDDs.
     */
    private Map<Arc, BDD> computeChoiceGuards(Page page) {
        Map<Arc, BDD> result = new LinkedHashMap<>();

        // Collect all choice places, which are places that have multiple outgoing arcs.
        List<Place> choicePlaces = page.getObjects().stream()
                .filter(o -> o instanceof Place p && p.getOutArcs().size() > 1).map(Place.class::cast).toList();

        // Iterate over all choice places and their outgoing arcs, and compute choice guards for all these arcs.
        for (Place choicePlace: choicePlaces) {
            for (Arc outgoingArc: choicePlace.getOutArcs()) {
                result.put(outgoingArc, computeChoiceGuard(outgoingArc));
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

        // Compute an initial choice guard for the target transition of the arc.
        BDD choiceGuard = getExtraTransitionGuard(transition);

        // Further simplify the choice guard by the state information of all incoming places of the transition.
        BDD statePredicate = transition.getInArcs().stream().map(a -> stateInfo.get(a.getSource()).id())
                .reduce(BDD::andWith).get();
        BDD simplifiedChoiceGuard = choiceGuard.simplify(statePredicate);

        // Free all intermediate BDDs.
        choiceGuard.free();
        statePredicate.free();

        return simplifiedChoiceGuard;
    }

    /**
     * Gives the extra synthesized condition for the specified transition, that is not yet captured by the guard of the
     * action which this transition represents.
     *
     * @param transition The transition for which to obtain the extra guard.
     * @return The extra guard as a BDD predicate.
     */
    private BDD getExtraTransitionGuard(Transition transition) {
        Function<Map<Event, BDD>, Event> findEvent = map -> map.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(transition.getName().getText())).map(Entry::getKey)
                .findFirst().orElse(null);

        // Try to process the transition as an event in the uncontrolled system.
        Event event = findEvent.apply(uncontrolledSystemGuards);
        Verify.verifyNotNull(event, "Unknown event: " + event);

        // Obtain the uncontrolled and controlled system guard for the given transition.
        BDD uncontrolledSystemGuard = uncontrolledSystemGuards.get(event);
        BDD controlledSystemGuard = controlledSystemGuards.get(event);

        // If a controlled system guard is available, simplify it with respect to the uncontrolled system guard.
        if (controlledSystemGuard != null) {
            return controlledSystemGuard.simplify(uncontrolledSystemGuard);
        } else {
            return uncontrolledSystemGuard.id();
        }
    }
}
