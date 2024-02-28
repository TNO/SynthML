
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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

/** Compute the event guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private Specification cifMinimizedStateSpace;

    private Map<Event, BDD> actionGuards;

    private CifDataSynthesisResult cifSynthesisResult;

    private PetriNet petriNet;

    private CifBddSpec cifBddSpec;

    private Map<Location, List<Annotation>> compositeStateMap;

    private Map<Place, Set<String>> regionMap;

    public ChoiceActionGuardComputation(Specification cifSpec, Map<Event, BDD> actionGuards,
            CifDataSynthesisResult cifSynthesisResult, PetriNet petriNet,
            Map<Location, List<Annotation>> compositeStateMap, Map<Place, Set<String>> regionMap)
    {
        this.cifMinimizedStateSpace = cifSpec;
        this.actionGuards = actionGuards;
        this.cifSynthesisResult = cifSynthesisResult;
        this.petriNet = petriNet;
        this.cifBddSpec = cifSynthesisResult.cifBddSpec;
        this.compositeStateMap = compositeStateMap;
        this.regionMap = regionMap;
    }

    public void computeChoiceGuards() {
        // Get the map from choice places to their choice events (outgoing events).
        Map<Place, List<Event>> choicePlaceToChoiceEvents = ChoiceActionGuardComputationHelper
                .getChoiceEventsPerChoicePlace(petriNet, cifBddSpec.alphabet);

        // Compute event guards for each choice place.
        Map<Place, Map<Event, BDD>> choicePlaceToChoiceEventToGuard = new HashMap<>();
        for (Entry<Place, List<Event>> entry: choicePlaceToChoiceEvents.entrySet()) {
            Place choicePlace = entry.getKey();
            List<Event> choiceEvents = entry.getValue();
            Map<Event, BDD> choiceEventToGuard = new HashMap<>();

            // Get guards of the choice events from the CIF specification.
            Map<Event, BDD> event2BDD4SpecGuards = actionGuards.entrySet().stream()
                    .filter(x -> choiceEvents.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Get guards of the choice events from the synthesis result.
            Map<Event, BDD> event2BDD4SynthesisGuards = cifSynthesisResult.outputGuards.entrySet().stream()
                    .filter(x -> choiceEvents.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Get the locations corresponding to the choice place.
            Set<String> choiceLocations = regionMap.get(choicePlace);
            List<Location> locs = ChoiceActionGuardComputationHelper.getLocations(cifMinimizedStateSpace,
                    choiceLocations);

            // Get state annotations of these locations.
            List<Annotation> annotations = locs.stream().flatMap(loc -> compositeStateMap.get(loc).stream()).toList();

            // Get BDDs of these state annotations.
            List<BDD> bdds = new ArrayList<>();
            for (Annotation annotation: annotations) {
                Expression expression = ChoiceActionGuardComputationHelper.stateAnnoToCifPred(annotation, cifBddSpec);
                BDD bdd = null;
                try {
                    bdd = CifToBddConverter.convertPred(expression, false, cifBddSpec);
                } catch (UnsupportedPredicateException e) {
                    throw new RuntimeException("Exception when converting CIF expression into BDD.", e);
                }
                bdds.add(bdd);
            }

            // Get disjunction of these BDDs.
            BDD disjunction = bdds.get(0);
            for (int i = 1; i < bdds.size(); i++) {
                disjunction = disjunction.or(bdds.get(i));
            }

            // Perform simplification for each event of the choice.
            for (Event choiceEvent: choiceEvents) {
                BDD simplicationResult = event2BDD4SynthesisGuards.get(choiceEvent)
                        .simplify(event2BDD4SpecGuards.get(choiceEvent)).simplify(disjunction);

                choiceEventToGuard.put(choiceEvent, simplicationResult);
            }
            choicePlaceToChoiceEventToGuard.put(choicePlace, choiceEventToGuard);
        }
    }
}
