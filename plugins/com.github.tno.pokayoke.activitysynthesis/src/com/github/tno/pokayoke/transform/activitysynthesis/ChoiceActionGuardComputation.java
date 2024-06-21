
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.cif.bdd.conversion.BddToCif;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.InputVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.common.java.Sets;

import com.github.javabdd.BDD;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Compute the guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private Specification cifMinimizedStateSpace;

    private Map<Event, BDD> uncontrolledSystemGuards;

    private Map<Event, BDD> auxiliarySystemGuards;

    private CifDataSynthesisResult cifSynthesisResult;

    private PetriNet petriNet;

    private Map<Location, List<Annotation>> compositeStateMap;

    private Map<Place, Set<String>> regionMap;

    public ChoiceActionGuardComputation(Specification cifMinimizedStateSpace, Map<Event, BDD> uncontrolledSystemGuards,
            Map<Event, BDD> auxiliarySystemGuards, CifDataSynthesisResult cifSynthesisResult, PetriNet petriNet,
            Map<Location, List<Annotation>> compositeStateMap, Map<Place, Set<String>> regionMap)
    {
        this.cifMinimizedStateSpace = cifMinimizedStateSpace;
        this.uncontrolledSystemGuards = uncontrolledSystemGuards;
        this.auxiliarySystemGuards = auxiliarySystemGuards;
        this.cifSynthesisResult = cifSynthesisResult;
        this.petriNet = petriNet;
        this.compositeStateMap = compositeStateMap;
        this.regionMap = regionMap;
    }

    public Map<Transition, Expression> computeChoiceGuards() {
        CifBddSpec cifBddSpec = cifSynthesisResult.cifBddSpec;

        // Get the map from choice places to their choice events (outgoing events).
        Set<Event> allEvents = Sets.union(uncontrolledSystemGuards.keySet(), auxiliarySystemGuards.keySet());
        Map<Place, List<Event>> choicePlaceToChoiceEvents = ChoiceActionGuardComputationHelper
                .getChoiceEventsPerChoicePlace(petriNet, allEvents);

        // Compute guards for each choice place.
        Map<Transition, Expression> choiceTransitionToGuard = new LinkedHashMap<>();
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
            List<BDD> choiceStatesPreds = new ArrayList<>();
            for (Annotation annotation: annotations) {
                Expression expression = ChoiceActionGuardComputationHelper.stateAnnotationToCifPred(annotation,
                        cifBddSpec);
                try {
                    BDD bdd = CifToBddConverter.convertPred(expression, false, cifBddSpec);
                    choiceStatesPreds.add(bdd);
                } catch (UnsupportedPredicateException e) {
                    throw new RuntimeException("Failed to convert CIF expression into BDD.", e);
                }
            }

            // Get disjunction of these BDDs.
            BDD choiceStatesPred = choiceStatesPreds.stream().reduce((left, right) -> left.orWith(right)).get();

            // Perform simplification to obtain choice guards.
            for (Event choiceEvent: choiceEvents) {
                BDD choiceGuardBdd;

                if (uncontrolledSystemGuards.containsKey(choiceEvent)) {
                    // The current choice event corresponds to an opaque behavior in the input UML model.

                    // Get the uncontrolled system guard from the CIF specification.
                    BDD uncontrolledSystemGuard = uncontrolledSystemGuards.get(choiceEvent);

                    // Get the controlled system guard from the synthesis result.
                    BDD controlledSystemGuard = cifSynthesisResult.outputGuards.get(choiceEvent);

                    // Perform simplification.
                    BDD supervisorExtraGuard = controlledSystemGuard.simplify(uncontrolledSystemGuard);
                    choiceGuardBdd = supervisorExtraGuard.simplify(choiceStatesPred);
                    supervisorExtraGuard.free();
                } else if (auxiliarySystemGuards.containsKey(choiceEvent)) {
                    // The current choice event is an auxiliary event introduced earlier in the synthesis chain.

                    // Perform simplification directly, as there is no synthesis result for the current choice event.
                    choiceGuardBdd = auxiliarySystemGuards.get(choiceEvent).simplify(choiceStatesPred);
                } else {
                    throw new RuntimeException("Unknown choice event " + choiceEvent);
                }

                Expression choiceGuardExpr = BddToCif.bddToCifPred(choiceGuardBdd, cifBddSpec);
                choiceGuardBdd.free();

                // Make sure the choice guard does not contain additional state.
                if (containsAdditionalState(choiceGuardExpr)) {
                    throw new RuntimeException(
                            "Expected choice guards to not contain extra state that was introduced during synthesis.");
                }

                choiceTransitionToGuard.put(
                        ChoiceActionGuardComputationHelper.getChoiceTransition(choicePlace, choiceEvent),
                        choiceGuardExpr);
            }
            choiceStatesPred.free();
        }

        return choiceTransitionToGuard;
    }

    /**
     * Determines whether the given expression contains auxiliary state that is not in the UML input model, but was
     * added during the process of activity synthesis, like for example the atomicity variable.
     *
     * @param expr The expression to check.
     * @return {@code true} in case the given expression contains additional state, {@code false} otherwise.
     */
    private boolean containsAdditionalState(Expression expr) {
        if (expr instanceof BinaryExpression binExpr) {
            return containsAdditionalState(binExpr.getLeft()) || containsAdditionalState(binExpr.getRight());
        } else if (expr instanceof BoolExpression) {
            return false;
        } else if (expr instanceof DiscVariableExpression varExpr) {
            return varExpr.getVariable().getName().startsWith("__");
        } else if (expr instanceof EnumLiteralExpression) {
            return false;
        } else if (expr instanceof InputVariableExpression) {
            return false;
        } else if (expr instanceof IntExpression) {
            return false;
        } else if (expr instanceof LocationExpression) {
            return true;
        } else if (expr instanceof UnaryExpression unExpr) {
            return containsAdditionalState(unExpr.getChild());
        }

        throw new RuntimeException("Unsupported expression: " + expr);
    }
}
