
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.Action;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PnObject;
import fr.lip6.move.pnml.ptnet.Transition;

/** Rewriter for non-atomic patterns in Petri Nets. */
public class NonAtomicPatternRewriter {
    /** A mapping from names of non-atomic start events to the names of their corresponding end events. */
    private final Map<String, List<String>> nonAtomicEventMap;

    /**
     * Constructs a new non-atomic pattern rewriter.
     *
     * @param nonAtomicEventMap The mapping from non-atomic start events to their corresponding end events.
     */
    public NonAtomicPatternRewriter(Map<Event, List<Event>> nonAtomicEventMap) {
        this.nonAtomicEventMap = convertToNameMapping(nonAtomicEventMap);
    }

    /**
     * Converts a given non-atomic event map to a mapping where the names of the events are used instead.
     *
     * @param nonAtomicEventMap The mapping from non-atomic start events to their corresponding end events.
     * @return The converted non-atomic event map.
     */
    private static Map<String, List<String>> convertToNameMapping(Map<Event, List<Event>> nonAtomicEventMap) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Entry<Event, List<Event>> entry: nonAtomicEventMap.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue().stream().map(Event::getName).toList());
        }

        return result;
    }

    /**
     * Finds all non-atomic patterns in the given Petri Net that can be rewritten, and rewrites them.
     *
     * @param petriNet The Petri Net to rewrite, which is modified in-place.
     * @return The list of non-atomic patterns that have been rewritten.
     */
    public List<NonAtomicPattern> findAndRewritePatterns(PetriNet petriNet) {
        List<NonAtomicPattern> patterns = findPatterns(petriNet);

        // Deepcopy because the patterns are actually deleted from the Petri net.
        List<NonAtomicPattern> originalPatterns = new ArrayList<>(EcoreUtil.copyAll(findPatterns(petriNet)));
        rewritePatterns(patterns);
        return originalPatterns;
    }

    /**
     * Gives all rewritable non-atomic patterns in the given Petri Net.
     *
     * @param petriNet The input Petri Net.
     * @return All non-atomic patterns in the given Petri Net.
     */
    private List<NonAtomicPattern> findPatterns(PetriNet petriNet) {
        return petriNet.getPages().stream().flatMap(page -> findPatterns(page).stream()).toList();
    }

    /**
     * Gives all rewritable non-atomic patterns in the given Petri Net page.
     *
     * @param page The input Petri Net page.
     * @return All non-atomic patterns in the given Petri Net page.
     */
    private List<NonAtomicPattern> findPatterns(Page page) {
        // Obtain the list of all transitions in the Petri Net page.
        List<Transition> transitions = sorted(
                page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast));

        // Obtain all non-atomic patterns.
        return transitions.stream().flatMap(t -> findPattern(t).stream()).toList();
    }

    /**
     * Tries finding a rewritable non-atomic pattern that starts from the given transition.
     *
     * @param transition The input transition.
     * @return Some rewritable non-atomic pattern in case one was found, or an empty result otherwise.
     */
    private Optional<NonAtomicPattern> findPattern(Transition transition) {
        String transitionName = transition.getName().getText();

        // Check whether the given transition is the start of a non-atomic action.
        if (!nonAtomicEventMap.containsKey(transitionName)) {
            return Optional.empty();
        }
        if (transition.getOutArcs().size() != 1) {
            return Optional.empty();
        }

        // Check whether the intermediate place does not have any incoming transitions other than the given transition.
        Place intermediatePlace = (Place)transition.getOutArcs().get(0).getTarget();

        if (intermediatePlace.getInArcs().size() != 1) {
            return Optional.empty();
        }

        // Check whether all outgoing transitions of the intermediate place are for ending the non-atomic action, and
        // there are exactly as many as expected, without duplicates.
        List<Transition> endTransitions = sorted(
                intermediatePlace.getOutArcs().stream().map(a -> (Transition)a.getTarget()));

        List<String> expectedEndEvents = nonAtomicEventMap.get(transitionName).stream().sorted().toList();
        List<String> actualEndEvents = endTransitions.stream().map(t -> t.getName().getText()).sorted().toList();

        if (!actualEndEvents.equals(expectedEndEvents)) {
            return Optional.empty();
        }

        // Check whether none of the end transitions have a join pattern. This is because any such join pattern would
        // make ending the non-atomic action dependent on some condition that is non-local for the non-atomic pattern.
        for (Transition endTransition: endTransitions) {
            if (endTransition.getInArcs().size() != 1) {
                return Optional.empty();
            }
        }

        // Check whether none of the end transitions start a fork pattern. This prevents combinations of fork and
        // choice after the end transitions, which would be difficult to merge back.
        for (Transition endTransition: endTransitions) {
            if (endTransition.getOutArcs().size() != 1) {
                return Optional.empty();
            }
        }

        // Get the end places.
        List<Place> endPlaces = new LinkedList<>();
        for (Transition endTransition: endTransitions) {
            Place endPlace = (Place)endTransition.getOutArcs().get(0).getTarget();
            endPlaces.add(endPlace);
        }

        // Ensure that the end places have no incoming arcs starting from other transitions.
        for (Place endPlace: endPlaces) {
            long otherIncomingTransitions = endPlace.getInArcs().stream().map(a -> a.getSource())
                    .filter(t -> !endTransitions.contains(t)).count();

            if (otherIncomingTransitions > 0) {
                return Optional.empty();
            }
        }

        // Return the information about the non-atomic pattern that can be rewritten.
        return Optional.of(new NonAtomicPattern(transition, intermediatePlace, endTransitions, endPlaces));
    }

    /**
     * Rewrites the given list of non-atomic patterns, by connecting the intermediate place to the transitions after the
     * end places, skipping the end transitions and related arcs.
     *
     * @param patterns The non-atomic patterns to rewrite, which are modified in-place.
     */
    private void rewritePatterns(List<NonAtomicPattern> patterns) {
        for (NonAtomicPattern pattern: patterns) {
            // First, remove the intermediate place's outgoing arcs.
            EcoreUtil.deleteAll(pattern.intermediatePlace.getOutArcs(), true);
            pattern.intermediatePlace.getOutArcs().clear();

            // Remove all the end transitions and their outgoing arcs.
            pattern.endTransitions.stream().forEach(et -> EcoreUtil.deleteAll(et.getOutArcs(), true));
            EcoreUtil.deleteAll(pattern.endTransitions, true);

            // Connect the intermediate place with the outgoing arcs from the end places.
            for (Place endPlace: pattern.endPlaces) {
                for (Arc outArc: new LinkedList<>(endPlace.getOutArcs())) {
                    outArc.setSource(pattern.intermediatePlace);
                }
            }

            // Remove end places.
            EcoreUtil.deleteAll(pattern.endPlaces, true);
        }
    }

    /**
     * Finds all activity actions corresponding to start or end transitions in the given list of rewritten patterns.
     *
     * @param patterns The rewritten non-atomic patterns on Petri Net level.
     * @param transitionMap The mapping from Petri Net transitions to corresponding UML actions.
     * @return All activity actions corresponding to start or end transitions in the given list of rewritten patterns.
     */
    public static Set<Action> getRewrittenActions(List<NonAtomicPattern> patterns,
            Map<Transition, Action> transitionMap)
    {
        Set<Action> rewrittenActions = new LinkedHashSet<>();

        for (NonAtomicPattern pattern: patterns) {
            rewrittenActions.add(transitionMap.get(pattern.startTransition()));

            for (Transition endTransition: pattern.endTransitions()) {
                rewrittenActions.add(transitionMap.get(endTransition));
            }
        }

        return rewrittenActions;
    }

    private static <T extends PnObject> List<T> sorted(Stream<T> stream) {
        return stream.sorted(Comparator.comparing(PnObject::getId)).toList();
    }

    /**
     * A rewritable non-atomic Petri Net pattern.
     *
     * @param startTransition The transition that starts the non-atomic action.
     * @param intermediatePlace The intermediate place that contains a token whenever the non-atomic action is
     *     executing.
     * @param endTransitions All transitions that end the execution of the non-atomic action.
     * @param endPlaces The places after the end transitions.
     */
    public record NonAtomicPattern(Transition startTransition, Place intermediatePlace, List<Transition> endTransitions,
            List<Place> endPlaces)
    {
    }
}
