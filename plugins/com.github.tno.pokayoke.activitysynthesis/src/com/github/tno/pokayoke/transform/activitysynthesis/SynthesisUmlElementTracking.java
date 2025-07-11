
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter.NonAtomicPattern;
import com.github.tno.pokayoke.transform.uml2cif.UmlElementInfo;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, and finally to the synthesized activity UML opaque actions (before
 * model finalization).
 */
public class SynthesisUmlElementTracking {
    private Map<String, UmlElementInfo> cifEventNamesToUmlElemntInfo;

    private Map<Transition, UmlElementInfo> transitionsToUmlElementInfo;

    private Map<Action, UmlElementInfo> actionsToUmlElementInfoMap;

    /**
     * Returns the map from CIF event names to the corresponding UML element info.
     *
     * @return The map from CIF event names to the corresponding UML element info.
     */
    public Map<String, UmlElementInfo> getCifEventNamesToUmlElemntInfo() {
        return cifEventNamesToUmlElemntInfo;
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
        COMPLETE_ACTION;
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
            cifEventNamesToUmlElemntInfo.put(cifEvent.getName(), umlElementInfo);
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
            cifEventNamesToUmlElemntInfo.put(cifEvent.getName(), umlElementInfo);
        }
    }

    public void updateEndAtomicNonDeterministic(List<String> removedNames) {
        // Update the UML element info: remove the object referring to the end of atomic non-deterministic COF event,
        // and set the start as merged.
        for (String removedName: removedNames) {
            String startActionName = removedName.substring(0,
                    removedName.lastIndexOf(UmlToCifTranslator.ATOMIC_OUTCOME_SUFFIX));
            cifEventNamesToUmlElemntInfo.get(startActionName).setMerged(true);
            cifEventNamesToUmlElemntInfo.remove(removedName);
        }
    }

    public void addTransitions(Set<Transition> transitions) {
        // Creates the map from transitions to UML element info, provided that the map from CIF event names to UML
        // elements info is not empty.
        Verify.verify(!cifEventNamesToUmlElemntInfo.isEmpty(), "TODO error msg");

        for (Transition t: transitions) {
            transitionsToUmlElementInfo.put(t, cifEventNamesToUmlElemntInfo.get(t.getName().getText()));
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
        for (java.util.Map.Entry<Transition, Action> entry: transitionsToActions.entrySet()) {
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

        return null;
    }

    public UmlElementInfo getUmlElementInfo(Action action) {
        return actionsToUmlElementInfoMap.get(action);
    }
}
