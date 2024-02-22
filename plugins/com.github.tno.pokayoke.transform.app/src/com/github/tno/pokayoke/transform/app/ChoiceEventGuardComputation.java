
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

/** Compute the guards of the events from choices. */
public class ChoiceEventGuardComputation {
    private Specification cifSpec;

    private Map<Event, BDD> eventGuards;

    private CifDataSynthesisResult cifSynthesisResult;

    private PetriNet petriNet;

    private CifBddSpec cifBddSpec;

    private Map<Location, List<Annotation>> compositeStateMap;

    private Map<Place, Set<String>> regionMap;

    public ChoiceEventGuardComputation(Specification cifSpec, Map<Event, BDD> actionGuards,
            CifDataSynthesisResult cifSynthesisResult, PetriNet petriNet, CifBddSpec cifBddSpec,
            Map<Location, List<Annotation>> compositeStateMap, Map<Place, Set<String>> regionMap)
    {
        this.cifSpec = cifSpec;
        this.eventGuards = actionGuards;
        this.cifSynthesisResult = cifSynthesisResult;
        this.petriNet = petriNet;
        this.cifBddSpec = cifBddSpec;
        this.compositeStateMap = compositeStateMap;
        this.regionMap = regionMap;
    }

    public void computeChoiceGuards()
            throws SecurityException, IllegalArgumentException, UnsupportedPredicateException
    {
        // Get the map from choice places to events.
        Map<Place, List<Event>> place2Events = ChoiceEventGuardComputationHelper.getPlaceEvents(petriNet,
                cifBddSpec.alphabet);
        Map<Place, Map<Event, BDD>> place2EventBddMap = new HashMap<>();

        // Compute action/event guards for each choice place.
        for (Entry<Place, List<Event>> entry: place2Events.entrySet()) {
            Place choicePlace = entry.getKey();
            List<Event> choiceEvents = entry.getValue();
            Map<Event, BDD> event2BddMap = new HashMap<>();

            // Get guards of the choice event from the CIF specification.
            Map<Event, BDD> event2BDD4SpecGuards = eventGuards.entrySet().stream()
                    .filter(x -> choiceEvents.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Get guards of the choice event from the synthesis result.
            Map<Event, BDD> event2BDD4SynthesisGuards = cifSynthesisResult.outputGuards.entrySet().stream()
                    .filter(x -> choiceEvents.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Get the locations corresponding to the choice place.
            Set<String> choiceLocations = regionMap.get(choicePlace);
            List<Location> locs = ChoiceEventGuardComputationHelper.getLocations(cifSpec, choiceLocations);

            // Get state annotations of these locations.
            List<Annotation> annotations = locs.stream().flatMap(loc -> compositeStateMap.get(loc).stream()).toList();

            // Get BDD of these state annotations.
            List<BDD> bdds = new ArrayList<>();
            for (Annotation annotation: annotations) {
                Expression expression = ChoiceEventGuardComputationHelper.getExpression(annotation, cifBddSpec);
                BDD bdd = CifToBddConverter.convertPred(expression, false, cifBddSpec);
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

                event2BddMap.put(choiceEvent, simplicationResult);
            }
            place2EventBddMap.put(choicePlace, event2BddMap);
        }
    }
}
