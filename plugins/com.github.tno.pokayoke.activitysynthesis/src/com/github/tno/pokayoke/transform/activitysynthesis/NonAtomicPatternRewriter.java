
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PnObject;
import fr.lip6.move.pnml.ptnet.Transition;

/** Rewriter for non-atomic patterns in Petri Nets. */
public class NonAtomicPatternRewriter {
    /** The prefix of a tau transition. */
    public static final String TAU_PREFIX = "__tau_";

    /** The number of tau transitions that have been introduced by this rewriter. */
    private int tauCounter = 0;

    /**
     * Finds and rewrites all non-atomic patterns in the given Petri Net, by renaming all their end transitions to tau.
     *
     * @param petriNet The Petri Net to rewrite, which is modified in-place.
     * @return The list of non-atomic patterns that have been rewritten.
     */
    public List<NonAtomicPattern> findAndRewritePatterns(PetriNet petriNet) {
        List<NonAtomicPattern> patterns = findPatterns(petriNet);
        rewritePatterns(patterns);
        return patterns;
    }

    /**
     * Gives all non-atomic patterns in the given Petri Net.
     *
     * @param petriNet The input Petri Net.
     * @return All non-atomic patterns in the given Petri Net.
     */
    private List<NonAtomicPattern> findPatterns(PetriNet petriNet) {
        return petriNet.getPages().stream().flatMap(page -> findPatterns(page).stream()).toList();
    }

    /**
     * Gives all non-atomic patterns in the given Petri Net page.
     *
     * @param page The input Petri Net page.
     * @return All non-atomic patterns in the given Petri Net page.
     */
    private List<NonAtomicPattern> findPatterns(Page page) {
        // Obtain the list of all transitions in the Petri Net page.
        List<Transition> transitions = sorted(
                page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast));

        // Build a map from (names of) non-atomic start events to their corresponding non-atomic end events.
        Map<String, Set<String>> nonAtomicEventMap = new LinkedHashMap<>();

        for (Transition transition: transitions) {
            String[] nameParts = transition.getName().getText().split(UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX);

            Verify.verify(nameParts.length == 1 || nameParts.length == 2,
                    String.format("Expected to find either one or two name parts, but found %d.", nameParts.length));

            if (nameParts.length > 1) {
                String startEvent = nameParts[0];
                Set<String> endEvents = nonAtomicEventMap.get(startEvent);

                if (endEvents == null) {
                    endEvents = new LinkedHashSet<>();
                    nonAtomicEventMap.put(startEvent, endEvents);
                }

                endEvents.add(startEvent + UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX + nameParts[1]);
            }
        }

        // Obtain all non-atomic patterns (and by doing so, check whether they are as expected).
        return transitions.stream().filter(transition -> nonAtomicEventMap.containsKey(transition.getName().getText()))
                .map(transition -> new NonAtomicPattern(transition, nonAtomicEventMap)).toList();
    }

    /**
     * Rewrites the given list of non-atomic patterns, by renaming their end transitions to tau.
     *
     * @param patterns The non-atomic patterns to rewrite, which are modified in-place.
     */
    private void rewritePatterns(List<NonAtomicPattern> patterns) {
        for (NonAtomicPattern pattern: patterns) {
            for (Transition endTransition: pattern.endTransitions) {
                String newName = TAU_PREFIX + tauCounter;
                endTransition.getName().setText(newName);
                endTransition.setId(newName);
                tauCounter++;
            }
        }
    }

    /**
     * Updates the given state information mapping and the uncontrollable system guards mapping, based on the given list
     * of non-atomic patterns that have been rewritten.
     *
     * @param patterns The non-atomic patterns which have been rewritten.
     * @param stateInfo The input state information mapping, which is modified in-place.
     * @param uncontrollableSystemGuards The uncontrollable system guards mapping, which is modified in-place.
     */
    public void updateMappings(List<NonAtomicPattern> patterns, Map<Place, BDD> stateInfo,
            Map<String, BDD> uncontrollableSystemGuards)
    {
        // Determine the updated state information and new uncontrollable system guards for every non-atomic pattern.
        for (NonAtomicPattern pattern: patterns) {
            // First we determine the new state information predicate of the intermediate place of the current pattern.
            BDD newStateInfoPredicate = pattern.endTransitions.stream()
                    // Find all target places that are reachable by some end transition of the current pattern.
                    .flatMap(transition -> transition.getOutArcs().stream().map(arc -> (Place)arc.getTarget()))
                    // Union the state information predicates of all these target places.
                    .map(place -> stateInfo.get(place).id()).reduce(BDD::orWith).get();

            stateInfo.put(pattern.intermediatePlace, newStateInfoPredicate);

            // Secondly, we compute an uncontrollable system guard for every end/tau transition.
            for (Transition endTransition: pattern.endTransitions) {
                String transitionName = endTransition.getName().getText();
                Preconditions.checkArgument(transitionName.contains(TAU_PREFIX),
                        String.format("Expected to find a tau transition, but got '%s'.", transitionName));

                // Determine the auxiliary guard of the current tau transition, to be the union of the state predicate
                // BDDs of all its end places, and update the uncontrollable guard mapping accordingly.
                BDD newGuard = endTransition.getOutArcs().stream()
                        // Find all target places that are reachable from the current end transition.
                        .map(arc -> (Place)arc.getTarget())
                        // Union the state information predicates of all these target places.
                        .map(place -> stateInfo.get(place).id()).reduce(BDD::orWith).get();

                uncontrollableSystemGuards.put(transitionName, newGuard);
            }
        }
    }

    private static <T extends PnObject> List<T> sorted(Stream<T> stream) {
        return stream.sorted(Comparator.comparing(PnObject::getId)).toList();
    }

    /** A non-atomic Petri Net pattern to rewrite. */
    public class NonAtomicPattern {
        /** The transition that starts the non-atomic action. */
        final Transition startTransition;

        /** The intermediate place that contains a token whenever the non-atomic action is executing. */
        final Place intermediatePlace;

        /** All transitions that end the execution of the non-atomic action. */
        final List<Transition> endTransitions;

        /**
         * Constructs a new non-atomic Petri Net pattern.
         *
         * @param startTransition The transition that starts the non-atomic action.
         * @param nonAtomicEventMap The mapping from (names of) non-atomic start events to their corresponding
         *     non-atomic end events.
         */
        NonAtomicPattern(Transition startTransition, Map<String, Set<String>> nonAtomicEventMap) {
            // Check whether the start transition conforms to the non-atomic pattern.
            this.startTransition = startTransition;

            Preconditions.checkArgument(startTransition.getOutArcs().size() == 1,
                    String.format("Expected non-atomic start transitions to have a single outgoing arc, but found %d.",
                            startTransition.getOutArcs().size()));

            Arc startArc = startTransition.getOutArcs().get(0);

            // Check whether the intermediate place conforms to the non-atomic pattern.
            this.intermediatePlace = (Place)startArc.getTarget();

            Preconditions.checkArgument(intermediatePlace.getInArcs().size() == 1,
                    String.format(
                            "Expected non-atomic intermediate places to have a single incoming arc, but found %d.",
                            intermediatePlace.getInArcs().size()));

            // Check whether the end transitions conforms to the non-atomic pattern.
            this.endTransitions = sorted(intermediatePlace.getOutArcs().stream().map(a -> (Transition)a.getTarget()));

            Set<String> expectedEndEvents = nonAtomicEventMap.get(startTransition.getName().getText());
            Set<String> actualEndEvents = endTransitions.stream().map(a -> a.getName().getText())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Preconditions.checkArgument(actualEndEvents.equals(expectedEndEvents),
                    String.format("Expected to find non-atomic end events '%s', but found '%s'.", expectedEndEvents,
                            actualEndEvents));

            for (Transition endTransition: endTransitions) {
                Preconditions.checkArgument(endTransition.getInArcs().size() == 1,
                        String.format(
                                "Expected non-atomic end transitions to have a single incoming arc, but found %d.",
                                endTransition.getInArcs().size()));
            }
        }
    }
}
