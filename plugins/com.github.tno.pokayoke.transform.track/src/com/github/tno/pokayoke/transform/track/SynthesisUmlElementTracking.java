
package com.github.tno.pokayoke.transform.track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.google.common.base.Verify;
import com.google.common.collect.Sets;

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

    /**
     * The map from CIF events generated for the synthesis to their corresponding UML element info. Does not get updated
     * as the synthesis chain progresses. Needed for the language equivalence check.
     */
    private Map<Event, UmlElementInfo> unalteredCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for the synthesis to their corresponding UML element info. Gets updated as the
     * synthesis chain rewrites events.
     */
    private Map<Event, UmlElementInfo> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF event names generated for the synthesis to their corresponding UML element info. */
    private Map<String, Event> namesToCifEvents = new LinkedHashMap<>();

    /** The map from CIF events generated for guard computation to their corresponding UML element info. */
    private Map<Event, UmlElementInfo> guardComputationCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF events generated for language equivalence to their corresponding UML element info. */
    private Map<Event, UmlElementInfo> languageEquivalenceCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from Petri net transitions to their corresponding UML element info. */
    private Map<Transition, UmlElementInfo> transitionsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from UML opaque actions to their corresponding original UML element info. */
    private Map<Action, UmlElementInfo> actionsToUmlElementInfoMap = new LinkedHashMap<>();

    /** The map from finalized UML elements to their corresponding original UML element info. */
    private Map<RedefinableElement, UmlElementInfo> finalizedUmlElementsToUmlElementInfoMap = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for guard computation to their corresponding synthesized model UML element
     * info.
     */
    private Map<Event, UmlElementInfo> guardComputationCifEventsToFinalizedUmlElementInfo = new LinkedHashMap<>();

    /** The set of internal CIF events (e.g. corresponding to control nodes) generated for synthesis. */
    private Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    /** The set of internal CIF events (e.g. corresponding to control nodes) generated for language equivalence. */
    private Set<Event> internalLanguageEquivalenceEvents = new LinkedHashSet<>();

    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

    public static enum ActionKind {
        START_OPAQUE_BEHAVIOR, END_OPAQUE_BEHAVIOR, COMPLETE_OPAQUE_BEHAVIOR, START_SHADOW, END_SHADOW, COMPLETE_SHADOW,
        START_OPAQUE_ACTION, END_OPAQUE_ACTION, COMPLETE_OPAQUE_ACTION, CONTROL_NODE;
    }

    public SynthesisUmlElementTracking() {
        // Empty constructor.
    }

    // Section dealing with CIF events.

    // Add a single CIF start event.
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, TranslationPurpose purpose) {
        // Create the UML element info and store it.
        UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
        umlElementInfo.setStartAction(true);
        umlElementInfo.setMerged(false);

        if (purpose == TranslationPurpose.SYNTHESIS) {
            synthesisCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
            namesToCifEvents.put(cifEvent.getName(), cifEvent);
            unalteredCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo.copy());
        } else if (purpose == TranslationPurpose.GUARD_COMPUTATION) {
            // In guard computation, the UML element represents the finalized UML element, and the CIF event is the
            // event stemming from it. We need to link this CIF event to the finalized event, and also to the original
            // UML element of the input model. The CIF event to finalized UML element defines a start, non-merged action
            // (similarly to the synthesis case). The CIF event to the original UML element inherits the action
            // attributes from the previous step in the synthesis chain, i.e. the finalized UML element info.

            // Store the CIF event in relation to the finalized UML element info.
            guardComputationCifEventsToFinalizedUmlElementInfo.put(cifEvent, umlElementInfo);

            // Store the CIF event in relation to the original UML element corresponding to the finalized UML element.
            UmlElementInfo originalUmlElementInfo;
            UmlElementInfo finalizedUmlElementInfo = finalizedUmlElementsToUmlElementInfoMap.get(umlElement);
            if (finalizedUmlElementInfo == null) {
                // If the current CIF element corresponds to a control node (e.g. decision node), there is no original
                // UML element to refer to. Create an empty UML element info.
                originalUmlElementInfo = new UmlElementInfo(null);
            } else {
                // Create a new UML element info object, that refers to the original UML element, and inherits its
                // attributes from the finalized UML element info.
                originalUmlElementInfo = new UmlElementInfo(finalizedUmlElementInfo.getUmlElement());
                originalUmlElementInfo.setStartAction(finalizedUmlElementInfo.isStartAction());
                originalUmlElementInfo.setMerged(finalizedUmlElementInfo.isMerged());
                originalUmlElementInfo.setEffectIdx(finalizedUmlElementInfo.getEffectIdx());
            }
            guardComputationCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);
        } else if (purpose == TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            // Store the CIF event in relation to the original UML element corresponding to the finalized UML element.
            UmlElementInfo originalUmlElementInfo;
            UmlElementInfo finalizedUmlElementInfo = finalizedUmlElementsToUmlElementInfoMap.get(umlElement);
            if (finalizedUmlElementInfo == null) {
                // If the current CIF element corresponds to a control node (e.g. decision node), there is no original
                // UML element to refer to. Create an empty UML element info.
                originalUmlElementInfo = new UmlElementInfo(null);
            } else {
                // Create a new UML element info object, that refers to the original UML element.
                originalUmlElementInfo = new UmlElementInfo(finalizedUmlElementInfo.getUmlElement());
                originalUmlElementInfo.setStartAction(finalizedUmlElementInfo.isStartAction());
                originalUmlElementInfo.setMerged(finalizedUmlElementInfo.isMerged());
                originalUmlElementInfo.setEffectIdx(finalizedUmlElementInfo.getEffectIdx());
            }

            languageEquivalenceCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);
        } else {
            throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    // Add a single CIF end event.
    public void addCifEvent(Event cifEvent, Pair<RedefinableElement, Integer> umlElementAndEffectIdx,
            TranslationPurpose purpose)
    {
        RedefinableElement umlElement = umlElementAndEffectIdx.left;
        int effectNr = umlElementAndEffectIdx.right;

        // Create the UML element info and store it.
        UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
        umlElementInfo.setStartAction(false);
        umlElementInfo.setMerged(false);
        umlElementInfo.setEffectIdx(effectNr);

        if (purpose == TranslationPurpose.SYNTHESIS) {
            synthesisCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
            namesToCifEvents.put(cifEvent.getName(), cifEvent);
            unalteredCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo.copy());
        } else if (purpose == TranslationPurpose.GUARD_COMPUTATION) {
            // In guard computation, the UML element represents the finalized UML element, and the CIF event is the
            // event stemming from it. We need to link this CIF event to the finalized event, and also to the original
            // UML element of the input model. The CIF event to finalized UML element defines a start, non-merged action
            // (similarly to the synthesis case). The CIF event to the original UML element inherits the action
            // attributes from the previous step in the synthesis chain, i.e. the finalized UML element info.

            // Store the CIF event in relation to the finalized UML element info.
            guardComputationCifEventsToFinalizedUmlElementInfo.put(cifEvent, umlElementInfo);

            // Store the CIF event in relation to the original UML element corresponding to the finalized UML element.
            UmlElementInfo originalUmlElementInfo;
            if (finalizedUmlElementsToUmlElementInfoMap.get(umlElement) == null) {
                // If the current CIF element corresponds to a control node (e.g. decision node), there is no original
                // UML element to refer to. Create an empty UML element info.
                originalUmlElementInfo = new UmlElementInfo(null);
            } else {
                // Create a new UML element info object, that refers to the original UML element, and set its attributes
                // as an end, non-merged action and the corresponding index.
                originalUmlElementInfo = new UmlElementInfo(
                        finalizedUmlElementsToUmlElementInfoMap.get(umlElement).getUmlElement());
                originalUmlElementInfo.setStartAction(false);
                originalUmlElementInfo.setMerged(false);
                originalUmlElementInfo.setEffectIdx(effectNr);
            }
            guardComputationCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);
        } else if (purpose == TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            // Store the CIF event in relation to the original UML element corresponding to the finalized UML element.
            UmlElementInfo originalUmlElementInfo;
            if (finalizedUmlElementsToUmlElementInfoMap.get(umlElement) == null) {
                // If the current CIF element corresponds to a control node (e.g. decision node), there is no original
                // UML element to refer to. Create an empty UML element info.
                originalUmlElementInfo = new UmlElementInfo(null);
            } else {
                // Create a new UML element info object, that refers to the original UML element, and set its attributes
                // as an end, non-merged action and the corresponding index.
                originalUmlElementInfo = new UmlElementInfo(
                        finalizedUmlElementsToUmlElementInfoMap.get(umlElement).getUmlElement());
                originalUmlElementInfo.setStartAction(false);
                originalUmlElementInfo.setMerged(false);
                originalUmlElementInfo.setEffectIdx(effectNr);
            }

            languageEquivalenceCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);
        } else {
            throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public Map<Event, RedefinableElement> getStartEventMap(TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.isEmpty() ? new LinkedHashMap<>()
                        : synthesisCifEventsToUmlElementInfo.entrySet().stream()
                                .filter(e -> e.getValue().isStartAction() && e.getValue().getUmlElement() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUmlElement()));
            }
            case GUARD_COMPUTATION: {
                // Return the map from CIF events generated for the guard computation step, to the finalized UML
                // elements info, *not* the original UML elements.
                return guardComputationCifEventsToFinalizedUmlElementInfo.isEmpty() ? new LinkedHashMap<>()
                        : guardComputationCifEventsToFinalizedUmlElementInfo.entrySet().stream()
                                .filter(e -> e.getValue().isStartAction() && e.getValue().getUmlElement() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUmlElement()));
            }
            case LANGUAGE_EQUIVALENCE: {
                return languageEquivalenceCifEventsToUmlElementInfo.isEmpty() ? new LinkedHashMap<>()
                        : languageEquivalenceCifEventsToUmlElementInfo.entrySet().stream()
                                .filter(e -> e.getValue().isStartAction() && e.getValue().getUmlElement() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUmlElement()));
            }
            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public UmlElementInfo getUmlElementInfo(Event event) {
        return synthesisCifEventsToUmlElementInfo.get(event);
    }

    public boolean isStartEvent(Event cifEvent) {
        return synthesisCifEventsToUmlElementInfo.get(cifEvent).isStartAction();
    }

    public void updateEndAtomicNonDeterministic(List<String> removedEventNames) {
        // Update the UML element info: remove the object referring to the end of atomic non-deterministic CIF event,
        // and set the start as merged.
        removedEventNames.stream().forEach(r -> synthesisCifEventsToUmlElementInfo.remove(namesToCifEvents.get(r)));

        // Find the start of atomic non-deterministic CIF events, and set the UML element info to 'merged'.
        List<Event> startAtomicNonDeterministicEvents = synthesisCifEventsToUmlElementInfo.keySet().stream()
                .filter(event -> event.getControllable() && isAtomicNonDeterministicStartEventName(event.getName()))
                .toList();
        startAtomicNonDeterministicEvents.stream()
                .forEach(e -> synthesisCifEventsToUmlElementInfo.get(e).setMerged(true));
    }

    public List<Event> getNodeEvents(Set<ActivityNode> nodes, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().getUmlElement())).map(Entry::getKey).toList();
            }
            case GUARD_COMPUTATION: {
                return guardComputationCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().getUmlElement())).map(Entry::getKey).toList();
            }
            case LANGUAGE_EQUIVALENCE: {
                return languageEquivalenceCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().getUmlElement())).map(Entry::getKey).toList();
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public List<Event> getStartEvents(RedefinableElement umlElement, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> entry.getValue().isStartAction()
                                && entry.getValue().getUmlElement().equals(umlElement))
                        .map(Entry::getKey).toList();
            }
            case GUARD_COMPUTATION: {
                return guardComputationCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> entry.getValue().isStartAction()
                                && entry.getValue().getUmlElement().equals(umlElement))
                        .map(Entry::getKey).toList();
            }
            case LANGUAGE_EQUIVALENCE: {
                return languageEquivalenceCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> entry.getValue().isStartAction()
                                && entry.getValue().getUmlElement().equals(umlElement))
                        .map(Entry::getKey).toList();
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public boolean isStartCallBehavior(Event cifEvent, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                UmlElementInfo umlElementInfo = synthesisCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueBehavior;
            }
            case GUARD_COMPUTATION: {
                // CIF events in relation to the original UML elements.
                UmlElementInfo umlElementInfo = guardComputationCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueBehavior;
            }
            case LANGUAGE_EQUIVALENCE: {
                UmlElementInfo umlElementInfo = languageEquivalenceCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueBehavior;
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public boolean isStartOpaqueAction(Event cifEvent, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                UmlElementInfo umlElementInfo = synthesisCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueAction;
            }
            case GUARD_COMPUTATION: {
                // CIF events in relation to the original UML elements.
                UmlElementInfo umlElementInfo = guardComputationCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueAction;
            }
            case LANGUAGE_EQUIVALENCE: {
                UmlElementInfo umlElementInfo = languageEquivalenceCifEventsToUmlElementInfo.get(cifEvent);
                return umlElementInfo.isStartAction() && umlElementInfo.getUmlElement() instanceof OpaqueAction;
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public boolean isEndAction(Event cifEvent, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                UmlElementInfo umlElementInfo = synthesisCifEventsToUmlElementInfo.get(cifEvent);
                return !umlElementInfo.isStartAction();
            }
            case GUARD_COMPUTATION: {
                // CIF events in relation to the original UML elements.
                UmlElementInfo umlElementInfo = guardComputationCifEventsToUmlElementInfo.get(cifEvent);
                return !umlElementInfo.isStartAction();
            }
            case LANGUAGE_EQUIVALENCE: {
                UmlElementInfo umlElementInfo = languageEquivalenceCifEventsToUmlElementInfo.get(cifEvent);
                return !umlElementInfo.isStartAction();
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    public Set<Pair<List<Event>, List<Event>>> getPrePostSynthesisChainEventsPaired(Activity activity) {
        // Pair the CIF events generated during the synthesis phase and during the language equivalence phase if they
        // refer to the same original UML element. Only for opaque behaviors, actions, and call behaviors.

        // Initialize map of paired events.
        Set<Pair<List<Event>, List<Event>>> pairedEvents = new LinkedHashSet<>();

        // Divide internal and external events for the synthesis map.
        Map<Event, UmlElementInfo> externalSynthesisEventsMap = new LinkedHashMap<>();
        for (Entry<Event, UmlElementInfo> entrySynth: unalteredCifEventsToUmlElementInfo.entrySet()) {
            // Only for opaque behaviors, opaque actions, and call behaviors.
            UmlElementInfo synthesisUmlElementInfo = entrySynth.getValue();

            if (synthesisUmlElementInfo.isInternal(activity)) {
                internalSynthesisEvents.add(entrySynth.getKey());
            } else {
                externalSynthesisEventsMap.put(entrySynth.getKey(), synthesisUmlElementInfo);
            }
        }

        // Divide internal and external events for the language equivalence map.
        Map<Event, UmlElementInfo> externalLanguageEqEventsMap = new LinkedHashMap<>();
        for (Entry<Event, UmlElementInfo> entrySynth: languageEquivalenceCifEventsToUmlElementInfo.entrySet()) {
            // Only for opaque behaviors, opaque actions, and call behaviors.
            UmlElementInfo languageEqUmlElementInfo = entrySynth.getValue();

            if (languageEqUmlElementInfo.isInternal(activity)) {
                internalLanguageEquivalenceEvents.add(entrySynth.getKey());
            } else {
                externalLanguageEqEventsMap.put(entrySynth.getKey(), languageEqUmlElementInfo);
            }
        }

        // Store the paired events of the post-synthesis model. If some events are not paired, they do not have a
        // corresponding UML element at the beginning of the synthesis: throw an error to flag this. Note that the
        // converse might not hold: there might be redundant behaviors (and thus events) in the original UML model, and
        // these should not have any paired events in the post-synthesis UMl model.
        Set<Event> usedPostSynthEvents = new LinkedHashSet<>();

        for (Entry<Event, UmlElementInfo> entrySynth: externalSynthesisEventsMap.entrySet()) {
            // Only for opaque behaviors, opaque actions, and call behaviors.
            UmlElementInfo synthesisUmlElementInfo = entrySynth.getValue();

            if (synthesisUmlElementInfo.isInternal(activity)) {
                continue;
            }

            List<Event> equivalentEvents = new ArrayList<>();

            for (Entry<Event, UmlElementInfo> entryLanguage: externalLanguageEqEventsMap.entrySet()) {
                UmlElementInfo languageUmlElementInfo = entryLanguage.getValue();

                // Store the equivalent events.
                if (synthesisUmlElementInfo.isEquivalent(languageUmlElementInfo)) {
                    equivalentEvents.add(entryLanguage.getKey());
                    usedPostSynthEvents.add(entryLanguage.getKey());
                }
            }

            if (!equivalentEvents.isEmpty()) {
                pairedEvents.add(new Pair<>(List.of(entrySynth.getKey()), equivalentEvents));
            }
        }

        // Sanity check: all external CIF events in the post-synthesis UML model should be paired with an event from the
        // synthesis phase.
        Verify.verify(usedPostSynthEvents.equals(externalLanguageEqEventsMap.keySet()),
                String.format("Found unused events %s in the post-synthesis UML model.",
                        Sets.difference(externalLanguageEqEventsMap.keySet(), usedPostSynthEvents).stream()
                                .map(e -> e.getName()).toList()));

        return pairedEvents;
    }

    public Set<Event> getInternalSynthesisEvents() {
        return internalSynthesisEvents;
    }

    public Set<Event> getInternalLanguageEqEvents() {
        return internalLanguageEquivalenceEvents;
    }

    // Section dealing with Petri net transitions.

    public void addPetriNetTransitions(PetriNet petriNet) {
        // Creates the map from transitions to UML element info, provided that the map from CIF event names to UML
        // elements info is not empty. It is more convenient to use the Petri net after it has been synthesised, instead
        // of storing each transition at the time of creation: in case a transition appears multiple times in a Petri
        // Net, Petrify distinguishes each duplicate by adding a postfix to the name of the transition (e.g.,
        // 'Transition_A/1' is a duplicate of 'Transition_A'), and these duplicates are not specified in the transition
        // declarations, but only appear in the specification, and are handled separately. Directly using the final
        // Petri net allows to just loop over the transitions, and store them in the synthesis tracker.
        Verify.verify(!synthesisCifEventsToUmlElementInfo.isEmpty(),
                "The map from CIF event names to UML element infos is empty.");

        List<Transition> petriNetTransitions = petriNet.getPages().get(0).getObjects().stream()
                .filter(o -> o instanceof Transition).map(Transition.class::cast).toList();

        for (Transition t: petriNetTransitions) {
            // Create new UML element info and store it.
            UmlElementInfo currentUmlElementInfo = synthesisCifEventsToUmlElementInfo
                    .get(namesToCifEvents.get(t.getName().getText()));
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

    public void removeLoopTransition() {
        // Remove the transition(s) that is used as the self-loop for the final place in the Petri net.
        // Nota: Using Cif2Petrify.LOOP_EVENT_NAME gives import cycles.
        Set<Transition> loopTransitions = transitionsToUmlElementInfo.entrySet().stream()
                .filter(e -> e.getKey().getName().getText().equals("__loop")).map(e -> e.getKey())
                .collect(Collectors.toSet());
        loopTransitions.stream().forEach(t -> transitionsToUmlElementInfo.remove(t));
    }

    public boolean isAtomicNonDeterministicStartEventName(String eventName) {
        UmlElementInfo umlElementInfo = synthesisCifEventsToUmlElementInfo.get(namesToCifEvents.get(eventName));
        return umlElementInfo.isAtomic() && !umlElementInfo.isDeterministic() && umlElementInfo.isStartAction();
    }

    public boolean isAtomicNonDeterministicEndEventName(String eventName) {
        UmlElementInfo umlElementInfo = synthesisCifEventsToUmlElementInfo.get(namesToCifEvents.get(eventName));
        return umlElementInfo.isAtomic() && !umlElementInfo.isDeterministic() && !umlElementInfo.isStartAction();
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

    public void addActions(Map<Transition, Action> transitionsToActions) {
        // Update the action to UML element info map.
        for (Entry<Transition, Action> entry: transitionsToActions.entrySet()) {
            actionsToUmlElementInfoMap.put(entry.getValue(), transitionsToUmlElementInfo.get(entry.getKey()));
        }
    }

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

    // Section dealing with finalized UML elements.

    public void addFinalizedUmlElement(RedefinableElement finalizedUmlElement,
            RedefinableElement incompleteUmlElement)
    {
        finalizedUmlElementsToUmlElementInfoMap.put(finalizedUmlElement,
                actionsToUmlElementInfoMap.get(incompleteUmlElement));
    }

    /**
     * Helper function, to reverse the given mapping.
     *
     * @param <T> The domain of map to reverse.
     * @param <U> The codomain the the map to reverse.
     * @param map The map to reverse.
     * @return The reversed map.
     */
    public static <T, U> Map<U, List<T>> reverse(Map<T, U> map) {
        Map<U, List<T>> result = new LinkedHashMap<>();

        for (Entry<T, U> entry: map.entrySet()) {
            result.computeIfAbsent(entry.getValue(), e -> new ArrayList<>()).add(entry.getKey());
        }

        return result;
    }
}
