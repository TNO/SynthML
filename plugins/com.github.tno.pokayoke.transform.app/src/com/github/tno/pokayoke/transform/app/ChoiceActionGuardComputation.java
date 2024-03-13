
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;

import com.github.javabdd.BDD;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Compute the guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private Specification cifMinimizedStateSpace;

    private Map<Event, BDD> uncontrolledSystemGuards;

    private CifDataSynthesisResult cifSynthesisResult;

    private PetriNet petriNet;

    private Map<Location, List<Annotation>> compositeStateMap;

    private Map<Place, Set<String>> regionMap;

    public ChoiceActionGuardComputation(Specification cifMinimizedStateSpace, Map<Event, BDD> uncontrolledSystemGuards,
            CifDataSynthesisResult cifSynthesisResult, PetriNet petriNet,
            Map<Location, List<Annotation>> compositeStateMap, Map<Place, Set<String>> regionMap)
    {
        this.cifMinimizedStateSpace = cifMinimizedStateSpace;
        this.uncontrolledSystemGuards = uncontrolledSystemGuards;
        this.cifSynthesisResult = cifSynthesisResult;
        this.petriNet = petriNet;
        this.compositeStateMap = compositeStateMap;
        this.regionMap = regionMap;
    }

    public Map<Transition, BDD> computeChoiceGuards() {
        CifBddSpec cifBddSpec = cifSynthesisResult.cifBddSpec;
        // Get the map from choice places to their choice events (outgoing events).
        Map<Place, List<Event>> choicePlaceToChoiceEvents = ChoiceActionGuardComputationHelper
                .getChoiceEventsPerChoicePlace(petriNet, cifBddSpec.alphabet);

        // Compute guards for each choice place.
        Map<Transition, BDD> choiceTransitionToGuard = new LinkedHashMap<>();
        for (Entry<Place, List<Event>> entry: choicePlaceToChoiceEvents.entrySet()) {
            Place choicePlace = entry.getKey();
            List<Event> choiceEvents = entry.getValue();

            // Get the locations corresponding to the choice place.
            Set<String> choiceLocations = regionMap.get(choicePlace);
            List<Location> locations = ChoiceActionGuardComputationHelper.getLocations(cifMinimizedStateSpace,
                    choiceLocations);

            // Get state annotations of these locations.
            List<Annotation> annotations = locations.stream()
                    .flatMap(location -> compositeStateMap.get(location).stream()).toList();

            // Get BDDs of these state annotations.
            List<BDD> choiceStatePreds = new ArrayList<>();
            for (Annotation annotation: annotations) {
                Expression expression = ChoiceActionGuardComputationHelper.stateAnnotationToCifPred(annotation,
                        cifBddSpec);
                try {
                    BDD bdd = CifToBddConverter.convertPred(expression, false, cifBddSpec);
                    choiceStatePreds.add(bdd);
                } catch (UnsupportedPredicateException e) {
                    throw new RuntimeException("Failed to convert CIF expression into BDD.", e);
                }
            }

            // Get disjunction of these BDDs.
            BDD disjunctionOfChoiceStatePreds = choiceStatePreds.stream().reduce((left, right) -> left.orWith(right))
                    .get();

            // Perform simplification to obtain choice guards.
            for (Event choiceEvent: choiceEvents) {
                // Get the uncontrolled system guards from the CIF specification.
                BDD uncontrolledSystemGuard = uncontrolledSystemGuards.get(choiceEvent);

                // Get the controlled system guards from the synthesis result.
                BDD controlledSystemGuard = cifSynthesisResult.outputGuards.get(choiceEvent);

                // Perform simplification.
                BDD simplicationResult = controlledSystemGuard.simplify(uncontrolledSystemGuard)
                        .simplify(disjunctionOfChoiceStatePreds);

                choiceTransitionToGuard.put(
                        ChoiceActionGuardComputationHelper.getChoiceTransition(choicePlace, choiceEvent),
                        simplicationResult);
            }
        }

        return choiceTransitionToGuard;
    }
}
