
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

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

import com.github.javabdd.BDD;
import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/** Compute the guards of the actions for choices. */
public class ChoiceActionGuardComputation {
    private final Specification cifMinimizedStateSpace;

    private final Map<Event, BDD> uncontrolledSystemGuards;

    private final Map<Event, BDD> auxiliarySystemGuards;

    private final CifDataSynthesisResult cifSynthesisResult;

    private final Map<Location, List<Annotation>> compositeStateMap;

    private final Map<Place, Set<String>> regionMap;

    public ChoiceActionGuardComputation(Specification cifMinimizedStateSpace, Map<Event, BDD> uncontrolledSystemGuards,
            Map<Event, BDD> auxiliarySystemGuards, CifDataSynthesisResult cifSynthesisResult,
            Map<Location, List<Annotation>> compositeStateMap, Map<Place, Set<String>> regionMap)
    {
        this.cifMinimizedStateSpace = cifMinimizedStateSpace;
        this.uncontrolledSystemGuards = uncontrolledSystemGuards;
        this.auxiliarySystemGuards = auxiliarySystemGuards;
        this.cifSynthesisResult = cifSynthesisResult;
        this.compositeStateMap = compositeStateMap;
        this.regionMap = regionMap;
    }

    /**
     * Computes choice guards for all choice arcs in the given Petri Net, i.e., arcs that go out of choice places.
     *
     * @param petriNet The input Petri Net.
     * @return A mapping from all choice arcs to their choice guards, as CIF expressions.
     */
    public Map<Arc, Expression> computeChoiceGuards(PetriNet petriNet) {
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected the Petri Net to have exactly one page.");
        return computeChoiceGuards(petriNet.getPages().get(0));
    }

    /**
     * Computes choice guards for all choice arcs in the given page, i.e., arcs that go out of choice places.
     *
     * @param page The input page.
     * @return A mapping from all choice arcs to their choice guards, as CIF expressions.
     */
    public Map<Arc, Expression> computeChoiceGuards(Page page) {
        Map<Arc, Expression> result = new LinkedHashMap<>();

        // Collect all choice places, which are places that have multiple outgoing arcs.
        List<Place> choicePlaces = page.getObjects().stream()
                .filter(o -> o instanceof Place p && p.getOutArcs().size() > 1).map(Place.class::cast).toList();

        // Iterate over all choice places and their outgoing arcs, and compute choice guards for all these arcs.
        for (Place choicePlace: choicePlaces) {
            for (Arc outgoingArc: choicePlace.getOutArcs()) {
                BDD choiceGuard = computeChoiceGuard(outgoingArc);
                result.put(outgoingArc, convertToExpr(choiceGuard));
                choiceGuard.free();
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
    public BDD computeChoiceGuard(Arc arc) {
        Transition transition = (Transition)arc.getTarget();

        // Compute an initial choice guard for the target transition of the arc.
        BDD choiceGuard = getExtraTransitionGuard(transition);

        // Further simplify the choice guard by the state information of all incoming places of the transition.
        BDD stateInfo = transition.getInArcs().stream().map(a -> (Place)a.getSource()).map(this::getStateInformation)
                .reduce(BDD::andWith).get();
        BDD simplifiedShoiceGuard = choiceGuard.simplify(stateInfo);

        // Free all intermediate BDDs.
        choiceGuard.free();
        stateInfo.free();

        return simplifiedShoiceGuard;
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

        if (event != null) {
            // Get the uncontrolled system guard from the CIF specification.
            BDD uncontrolledSystemGuard = uncontrolledSystemGuards.get(event);

            // Get the controlled system guard from the synthesis result.
            BDD controlledSystemGuard = cifSynthesisResult.outputGuards.get(event);

            // Perform simplification so that only the extra synthesized condition is left over.
            return controlledSystemGuard.simplify(uncontrolledSystemGuard);
        }

        // Try to process the transition as an auxiliary event that was introduced earlier in the synthesis chain.
        event = findEvent.apply(auxiliarySystemGuards);

        if (event != null) {
            // Since there no synthesis result for this auxiliary event, return it without simplification.
            return auxiliarySystemGuards.get(event).id();
        }

        throw new RuntimeException("Unknown event: " + event);
    }

    /**
     * Gives the predicate that represents all states the system can be in while there is a token on the given place.
     *
     * @param place The place for which to obtain state information.
     * @return The state information as a BDD predicate.
     */
    private BDD getStateInformation(Place place) {
        CifBddSpec cifBddSpec = cifSynthesisResult.cifBddSpec;

        // Get all CIF locations corresponding to the choice place.
        List<Location> locations = ChoiceActionGuardComputationHelper.getLocations(cifMinimizedStateSpace,
                regionMap.get(place));

        // Get all state annotations of these locations.
        List<Annotation> annotations = locations.stream().flatMap(location -> compositeStateMap.get(location).stream())
                .toList();

        // Get BDDs of these state annotations.
        List<BDD> choiceStatesPreds = new ArrayList<>();
        for (Annotation annotation: annotations) {
            Expression expression = ChoiceActionGuardComputationHelper.stateAnnotationToCifPred(annotation, cifBddSpec);
            try {
                BDD bdd = CifToBddConverter.convertPred(expression, false, cifBddSpec);
                choiceStatesPreds.add(bdd);
            } catch (UnsupportedPredicateException e) {
                throw new RuntimeException("Failed to convert CIF expression into BDD.", e);
            }
        }

        // Compute the disjunction of these BDDs.
        return choiceStatesPreds.stream().reduce(BDD::orWith).get();
    }

    /**
     * Converts a given BDD predicate to a CIF expression.
     *
     * @param pred The BDD predicate to convert.
     * @return The converted CIF expression which doesn't use auxiliary variables that were introduced during synthesis.
     */
    private Expression convertToExpr(BDD pred) {
        Expression expr = BddToCif.bddToCifPred(pred, cifSynthesisResult.cifBddSpec);

        // Make sure the choice guard does not contain additional state.
        if (containsAdditionalState(expr)) {
            throw new RuntimeException(
                    "Expected choice guards to not contain extra variables that were introduced during synthesis.");
        }

        return expr;
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
            return true;
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
