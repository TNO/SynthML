
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, and finally to the synthesized activity UML opaque actions (before
 * model finalization).
 */
public class SynthesisUmlElementTracking {
    /** The suffix of an atomic action outcome. */
    public static final String ATOMIC_OUTCOME_SUFFIX = "__result_";

    /** The suffix of a non-atomic action outcome. */
    public static final String NONATOMIC_OUTCOME_SUFFIX = "__na_result_";

    /** The map from CIF events to their corresponding UML element info. */
    private Map<Event, UmlElementInfo> cifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF event names to their corresponding UML element info. */
    private Map<String, UmlElementInfo> cifEventNamesToUmlElementInfo = new LinkedHashMap<>();

    /** The map from Petri net transitions to their corresponding UML element info. */
    private Map<Transition, UmlElementInfo> transitionsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from UML opaque actions to their corresponding original UML element info. */
    private Map<Action, UmlElementInfo> actionsToUmlElementInfoMap = new LinkedHashMap<>();

    /**
     * Returns the map from CIF event names to the corresponding UML element info.
     *
     * @return The map from CIF event names to the corresponding UML element info.
     */
    public Map<String, UmlElementInfo> getCifEventNamesToUmlElementInfo() {
        return cifEventNamesToUmlElementInfo;
    }

    /**
     * Returns the map from Petri net transitions to the corresponding UML element info.
     *
     * @return The map from Petri net transitions to the corresponding UML element info.
     */
    public Map<Transition, UmlElementInfo> getTransitionsToUmlElementInfo() {
        return transitionsToUmlElementInfo;
    }

    /**
     * Returns the map from the new UML opaque actions to the corresponding UML element info.
     *
     * @return The map from the new UML opaque actions to the corresponding UML element info.
     */
    public Map<Action, UmlElementInfo> getActionsToUmlElementInfoMap() {
        return actionsToUmlElementInfoMap;
    }

    public static enum ActionKind {
        START_OPAQUE_BEHAVIOR, END_OPAQUE_BEHAVIOR, COMPLETE_OPAQUE_BEHAVIOR, START_SHADOW, END_SHADOW, COMPLETE_SHADOW,
        START_OPAQUE_ACTION, END_OPAQUE_ACTION, COMPLETE_OPAQUE_ACTION, CONTROL_NODE;
    }

    public SynthesisUmlElementTracking() {
        // Empty constructor.
    }

    // Add a single CIF start event.
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement) {
        // Create the UML element info and store it.
        UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
        umlElementInfo.setStartAction(true);
        umlElementInfo.setMerged(false);
        cifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
        cifEventNamesToUmlElementInfo.put(cifEvent.getName(), umlElementInfo);
    }

    public void addCifStartEvents(Map<Event, RedefinableElement> cifEventToUmlElement) {
        for (java.util.Map.Entry<Event, RedefinableElement> entry: cifEventToUmlElement.entrySet()) {
            Event cifEvent = entry.getKey();
            RedefinableElement umlElement = entry.getValue();

            // Create the UML element info and store it.
            UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
            umlElementInfo.setStartAction(true);
            umlElementInfo.setMerged(false);
            cifEventNamesToUmlElementInfo.put(cifEvent.getName(), umlElementInfo);
        }
    }

    // Add a single CIF end event.
    public void addCifEvent(Event cifEvent, Pair<RedefinableElement, Integer> umlElementAndEffectIdx) {
        RedefinableElement umlElement = umlElementAndEffectIdx.left;
        int effectNr = umlElementAndEffectIdx.right;

        // Create the UML element info and store it.
        UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
        umlElementInfo.setStartAction(false);
        umlElementInfo.setMerged(false);
        umlElementInfo.setEffectIdx(effectNr);
        cifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
        cifEventNamesToUmlElementInfo.put(cifEvent.getName(), umlElementInfo);
    }

    // Same method after erasure, so we need to give different names.
    public void addCifEndEvents(Map<Event, Pair<RedefinableElement, Integer>> map) {
        for (java.util.Map.Entry<Event, Pair<RedefinableElement, Integer>> entry: map.entrySet()) {
            Event cifEvent = entry.getKey();
            RedefinableElement umlElement = entry.getValue().left;
            int effectNr = entry.getValue().right;

            // Create the UML element info and store it.
            UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
            umlElementInfo.setStartAction(false);
            umlElementInfo.setMerged(false);
            umlElementInfo.setEffectIdx(effectNr);
            cifEventNamesToUmlElementInfo.put(cifEvent.getName(), umlElementInfo);
        }
    }

    public Map<Event, RedefinableElement> getStartEventMap() {
        return cifEventsToUmlElementInfo.isEmpty() ? new LinkedHashMap<>()
                : cifEventsToUmlElementInfo.entrySet().stream().filter(e -> e.getValue().isStartAction())
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUmlElement()));
    }

    public void updateEndAtomicNonDeterministic(List<String> removedNames) {
        // Update the UML element info: remove the object referring to the end of atomic non-deterministic COF event,
        // and set the start as merged.
        for (String removedName: removedNames) {
            String startActionName = removedName.substring(0, removedName.lastIndexOf(ATOMIC_OUTCOME_SUFFIX));
            cifEventNamesToUmlElementInfo.get(startActionName).setMerged(true);
            cifEventNamesToUmlElementInfo.remove(removedName);
        }
    }

    // Section dealing with Petri net transitions.

    public void addPetriNetTransitions(PetriNet petriNet) {
        // Creates the map from transitions to UML element info, provided that the map from CIF event names to UML
        // elements info is not empty.
        Verify.verify(!cifEventNamesToUmlElementInfo.isEmpty(),
                "The map from CIF event names to UML element infos is empty.");

        List<Transition> petriNetTransitions = petriNet.getPages().get(0).getObjects().stream()
                .filter(o -> o instanceof Transition).map(Transition.class::cast).toList();

        for (Transition t: petriNetTransitions) {
            // Create new UML element info and store it.
            UmlElementInfo currentUmlElementInfo = cifEventNamesToUmlElementInfo.get(t.getName().getText());
            if (currentUmlElementInfo == null) {
                transitionsToUmlElementInfo.put(t, null);
            } else {
                UmlElementInfo newUmlElementInfo = new UmlElementInfo(currentUmlElementInfo.getUmlElement());
                newUmlElementInfo.setStartAction(currentUmlElementInfo.isStartAction());
                newUmlElementInfo.setMerged(currentUmlElementInfo.isMerged());
                newUmlElementInfo.setEffectIdx(currentUmlElementInfo.getEffectIdx());
                transitionsToUmlElementInfo.put(t, newUmlElementInfo);
            }
        }
    }

    public void updateRewrittenPatterns(List<NonAtomicPattern> nonAtomicPatterns) {
        // For each rewritten pattern, update the start transition as merged and remove the corresponding end
        // transitions.
        for (NonAtomicPattern pattern: nonAtomicPatterns) {
            transitionsToUmlElementInfo.get(pattern.startTransition()).setMerged(true);
            pattern.endTransitions().stream().forEach(et -> transitionsToUmlElementInfo.remove(et));
        }
    }

    public void addActions(Map<Transition, Action> transitionsToActions) {
        // Update the action to UML element info map.
        for (Entry<Transition, Action> entry: transitionsToActions.entrySet()) {
            actionsToUmlElementInfoMap.put(entry.getValue(), transitionsToUmlElementInfo.get(entry.getKey()));
        }
    }

    public void removeLoopTransition() {
        // Remove the transition(s) that is used as the self-loop for the final place in the Petri net.
        // Nota: Using Cif2Petrify.LOOP_EVENT_NAME gives import cycles.
        Set<Transition> loopTransitions = transitionsToUmlElementInfo.entrySet().stream()
                .filter(e -> e.getKey().getName().getText().equals("__loop")).map(e -> e.getKey())
                .collect(Collectors.toSet());
        loopTransitions.stream().forEach(t -> transitionsToUmlElementInfo.remove(t));
    }

    public boolean isAtomicEndEvent(Event event) {
        UmlElementInfo umlElementInfo = cifEventNamesToUmlElementInfo.get(event.getName());
        return umlElementInfo.isAtomic() && !umlElementInfo.isStartAction();
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

    // Section dealing with new UML opaque actions.

    /**
     * Returns the kind of activity node the input action should translate to, based on the UML element the action
     * originates from, and whether it was merged during the synthesis chain.
     *
     * @param action The UML action create to fill the newly synthesised activity.
     * @return An enumeration defining the kind of activity node the action should translate to.
     */
    public ActionKind getActionKind(Action action) {
        UmlElementInfo umlElementInfo = actionsToUmlElementInfoMap.get(action);
        if (umlElementInfo.getUmlElement() instanceof OpaqueBehavior) {
            if (umlElementInfo.isStartAction() && (umlElementInfo.isAtomic() || umlElementInfo.isMerged())) {
                return ActionKind.COMPLETE_OPAQUE_BEHAVIOR;
            } else if (umlElementInfo.isStartAction()) {
                return ActionKind.START_OPAQUE_BEHAVIOR;
            } else {
                return ActionKind.END_OPAQUE_BEHAVIOR;
            }
        }

        return ActionKind.CONTROL_NODE;
    }

    public UmlElementInfo getUmlElementInfo(Action action) {
        return actionsToUmlElementInfoMap.get(action);
    }
}
