
package com.github.tno.pokayoke.transform.activitysynthesis;

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

import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter.NonAtomicPattern;
import com.github.tno.pokayoke.transform.uml2cif.UmlElementInfo;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, and finally to the synthesized activity UML opaque actions (before
 * model finalization).
 */
public class SynthesisUmlElementTracking {
    private Map<String, UmlElementInfo> cifEventNamesToUmlElementInfo = new LinkedHashMap<>();

    private Map<Transition, UmlElementInfo> transitionsToUmlElementInfo = new LinkedHashMap<>();

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
        START_CALL, END_CALL, COMPLETE_CALL, START_SHADOW, END_SHADOW, COMPLETE_SHADOW, START_ACTION, END_ACTION,
        COMPLETE_ACTION, CONTROL_NODE;
    }

    public SynthesisUmlElementTracking() {
        // Empty constructor.
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
            umlElementInfo.setEffectNr(effectNr);
            cifEventNamesToUmlElementInfo.put(cifEvent.getName(), umlElementInfo);
        }
    }

    public void updateEndAtomicNonDeterministic(List<String> removedNames) {
        // Update the UML element info: remove the object referring to the end of atomic non-deterministic COF event,
        // and set the start as merged.
        for (String removedName: removedNames) {
            String startActionName = removedName.substring(0,
                    removedName.lastIndexOf(UmlToCifTranslator.ATOMIC_OUTCOME_SUFFIX));
            cifEventNamesToUmlElementInfo.get(startActionName).setMerged(true);
            cifEventNamesToUmlElementInfo.remove(removedName);
        }
    }

    public void addPetriNetTransitions(PetriNet petriNet) {
        // Creates the map from transitions to UML element info, provided that the map from CIF event names to UML
        // elements info is not empty.
        Verify.verify(!cifEventNamesToUmlElementInfo.isEmpty(), "TODO error msg");

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
                newUmlElementInfo.setEffectNr(currentUmlElementInfo.getEffectNr());
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

    public ActionKind getActionKind(Action action) {
        UmlElementInfo umlElementInfo = actionsToUmlElementInfoMap.get(action);
        if (umlElementInfo.getUmlElement() instanceof OpaqueBehavior) {
            if (umlElementInfo.isStartAction() && (umlElementInfo.isAtomic() || umlElementInfo.isMerged())) {
                return ActionKind.COMPLETE_CALL;
            } else if (umlElementInfo.isStartAction()) {
                return ActionKind.START_CALL;
            } else {
                return ActionKind.END_CALL;
            }
        }

        return ActionKind.CONTROL_NODE;
    }

    public UmlElementInfo getUmlElementInfo(Action action) {
        return actionsToUmlElementInfoMap.get(action);
    }

    public void removeLoopTransition() {
        // Remove the transition(s) that is used as the self-loop for the final place in the Petri net.
        // Nota: Using Cif2Petrify.LOOP_EVENT_NAME gives import cycles.
        Set<Transition> loopTransitions = transitionsToUmlElementInfo.entrySet().stream()
                .filter(e -> e.getKey().getName().getText().equals("__loop")).map(e -> e.getKey())
                .collect(Collectors.toSet());
        loopTransitions.stream().forEach(t -> transitionsToUmlElementInfo.remove(t));
    }
}
