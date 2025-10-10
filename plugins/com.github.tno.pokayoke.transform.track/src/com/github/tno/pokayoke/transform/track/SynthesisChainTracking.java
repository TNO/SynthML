
package com.github.tno.pokayoke.transform.track;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the activity synthesis chain transformations from the UML elements of the input model, to their translation to
 * different formalisms throughout the various steps of the synthesis chain, such as CIF event, Petri net transitions,
 * actions, etc. The UML elements of the input model and CIF events created in the first step of the synthesis chain are
 * named 'original', to distinguish them from the UML elements in the synthesized activity (named 'non-finalized' for
 * placeholder opaque actions, and 'finalized' for their finalized version, see also
 * {@link #addFinalizedUmlElement(RedefinableElement, OpaqueAction)}) and from the CIF events created for guard
 * computation or language equivalence check phases of the synthesis chain.
 * <p>
 * This tracking storage contains only 'global' tracing information from transformations in the synthesis chain that is
 * needed by other transformations. Any other 'local' tracing information which is not needed by other transformations
 * is maintained in the individual transformations.
 * </p>
 */
public class SynthesisChainTracking {
    /**
     * The map from CIF events to their tracing info. Gets updated as the activity synthesis chain rewrites, removes, or
     * add events.
     */
    private final Map<Event, EventTraceInfo> cifEventTraceInfo = new LinkedHashMap<>();

    /** The map from Petri net transitions to their corresponding tracing info. */
    private final Map<Transition, TransitionTraceInfo> transitionTraceInfo = new LinkedHashMap<>();

    /**
     * The map from new (in the body of the abstract activity being synthesized) UML opaque actions to their
     * corresponding Petri net transitions.
     */
    private final Map<OpaqueAction, Transition> actionToTransition = new LinkedHashMap<>();

    /** The map from the finalized UML elements to the non-finalized opaque actions they originate from. */
    private final Map<RedefinableElement, OpaqueAction> finalizedElementToAction = new LinkedHashMap<>();

    public static enum ActionKind {
        START_OPAQUE_BEHAVIOR, END_OPAQUE_BEHAVIOR, COMPLETE_OPAQUE_BEHAVIOR, CONTROL_NODE;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Section dealing with CIF events and the corresponding input UML elements.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Registers that the given CIF event has been created for the given UML element for the indicated translation
     * purpose.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param purpose The translation purpose.
     * @param umlElement The UML element that relates to the CIF event, or {@code null} if no such element exists.
     * @param effectIdx The effect index. It must be {@code null} for events that are both start and end events, as well
     *     as for start-only events. End-only events must have a non-negative integer effect index.
     * @param isStartEvent {@code true} if the event represents a start event, {@code false} otherwise.
     * @param isEndEvent {@code true} if the event represents an end event, {@code false} otherwise.
     */
    public void addCifEvent(Event cifEvent, UmlToCifTranslationPurpose purpose, RedefinableElement umlElement,
            Integer effectIdx, boolean isStartEvent, boolean isEndEvent)
    {
        cifEventTraceInfo.put(cifEvent, new EventTraceInfo(purpose, umlElement, effectIdx, isStartEvent, isEndEvent));
    }

    /**
     * Gives the map from CIF start events to the corresponding UML elements (or {@code null}) for the specified
     * translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding UML elements (or {@code null}) for the specified
     *     translation purpose.
     */
    public Map<Event, RedefinableElement> getStartEventMap(UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().getTranslationPurpose().equals(purpose) && e.getValue().isStartEvent())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().getUmlElement()),
                        LinkedHashMap::putAll);
    }

    /**
     * Returns the event tracing info related to the input CIF event.
     *
     * @param cifEvent The CIF event.
     * @return The CIF event tracing info.
     */
    public EventTraceInfo getEventTraceInfo(Event cifEvent) {
        // Sanity check: CIF event must be stored.
        EventTraceInfo eventInfo = cifEventTraceInfo.get(cifEvent);
        Verify.verifyNotNull(eventInfo, "CIF event '" + cifEvent.getName() + "' does not have any tracing info.");
        return eventInfo;
    }

    /**
     * Returns the events corresponding to the given set of UML elements, based on the indicated translation purpose.
     *
     * @param umlElements The set of UML elements, to find the related CIF events. Each UML element must be
     *     non-{@code null}.
     * @param purpose The translation purpose.
     * @return The list of CIF events corresponding to the UML elements.
     */
    public List<Event> getEventsOf(Set<? extends RedefinableElement> umlElements, UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream().filter(e -> e.getValue().getTranslationPurpose().equals(purpose)
                && umlElements.contains(e.getValue().getUmlElement())).map(Map.Entry::getKey).toList();
    }

    /**
     * Gives the list of CIF start events corresponding to the given UML element for the specified translation purpose.
     *
     * @param umlElement The non-{@code null} UML element.
     * @param purpose The translation purpose.
     * @return The list of CIF start events corresponding to the given UML element.
     */
    public List<Event> getStartEventsOf(RedefinableElement umlElement, UmlToCifTranslationPurpose purpose) {
        List<Event> eventsOfElement = cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().getTranslationPurpose().equals(purpose) && e.getValue().isStartEvent()
                        && e.getValue().getUmlElement() != null && e.getValue().getUmlElement().equals(umlElement))
                .map(Map.Entry::getKey).toList();

        // Before vertical scaling, there should be only one event per UML element.
        Verify.verify(eventsOfElement.size() == 1,
                "Found multiple CIF events corresponding to element '" + umlElement.getName() + "'.");

        return eventsOfElement;
    }

    /**
     * Gives the map from CIF start events to the corresponding CIF end events, for the specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding CIF end events.
     */
    private Map<Event, List<Event>> getStartEndEventMap(UmlToCifTranslationPurpose purpose) {
        Map<Event, List<Event>> result = new LinkedHashMap<>();

        // Get the map of all start events.
        Map<Event, RedefinableElement> startEventMap = getStartEventMap(purpose);

        // Get the end events for every start event.
        for (Entry<Event, RedefinableElement> entry: startEventMap.entrySet()) {
            Event startEvent = entry.getKey();
            RedefinableElement umlElement = entry.getValue();

            if (result.containsKey(startEvent)) {
                throw new RuntimeException(
                        "Expected action '" + startEvent.getName() + "' to have a single start event.");
            }

            result.put(startEvent, getEndEventsOf(umlElement, purpose));
        }

        return result;
    }

    /**
     * Gives the map from CIF start events of non-atomic actions to the corresponding CIF end events, for the specified
     * translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding CIF end events.
     */
    public Map<Event, List<Event>> getNonAtomicStartEndEventMap(UmlToCifTranslationPurpose purpose) {
        // Get the map from start events to the corresponding end events.
        Map<Event, List<Event>> startEndEventMap = getStartEndEventMap(purpose);

        return startEndEventMap.entrySet().stream()
                .filter(e -> !isAtomicAction(getEventTraceInfo(e.getKey()).getUmlElement()))
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private List<Event> getEndEventsOf(RedefinableElement umlElement, UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream().filter(e -> e.getValue().getTranslationPurpose().equals(purpose))
                .filter(e -> e.getValue().getUmlElement() != null && e.getValue().getUmlElement().equals(umlElement))
                .filter(e -> e.getValue().isEndEvent()).map(e -> e.getKey()).toList();
    }

    private boolean isAtomicAction(RedefinableElement umlElement) {
        if (umlElement == null) {
            // If the UML element is 'null', the related CIF event represents an internal event (e.g. control node),
            // which is atomic by default.
            return true;
        }

        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            return PokaYokeUmlProfileUtil.isAtomic(umlElement);
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            return isAtomicAction(cbAction.getBehavior());
        }

        // Control nodes are translated as atomic actions; otherwise, nodes are non-atomic by default.
        return umlElement instanceof ControlNode;
    }

    /**
     * Gives the map from CIF start events of non-deterministic actions to the corresponding CIF end events, for the
     * specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding CIF end events.
     */
    public Map<Event, List<Event>> getNonDeterministicStartEndEventMap(UmlToCifTranslationPurpose purpose) {
        // Get the map from start events to the corresponding end events.
        Map<Event, List<Event>> startEndEventMap = getStartEndEventMap(purpose);

        return startEndEventMap.entrySet().stream()
                .filter(e -> !isDeterministicAction(getEventTraceInfo(e.getKey()).getUmlElement()))
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private boolean isDeterministicAction(RedefinableElement umlElement) {
        if (umlElement == null) {
            // If the UML element is 'null', the related CIF event represents an internal event (e.g. control node),
            // which is deterministic by default.
            return true;
        }

        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            return PokaYokeUmlProfileUtil.isDeterministic(umlElement);
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            return isDeterministicAction(cbAction.getBehavior());
        }

        // If the element does not have the Poka Yoke profile applied nor is a call behavior, it is deterministic.
        return true;
    }

    /**
     * Returns {@code true} if the event name corresponds to the end of an atomic non-deterministic UML element.
     *
     * @param eventName The name of the CIF event.
     * @return {@code true} if the event name corresponds to the end of an atomic non-deterministic UML element.
     */
    public boolean isAtomicNonDeterministicEndEventName(String eventName) {
        // Find the unique CIF event with the input name.
        List<Event> cifEvents = cifEventTraceInfo.keySet().stream().filter(e -> e.getName().equals(eventName)).toList();
        Verify.verify(cifEvents.size() == 1, "Found more than one CIF event with name: '" + eventName + "'.");

        EventTraceInfo eventInfo = getEventTraceInfo(cifEvents.get(0));
        return isAtomicAction(eventInfo.getUmlElement()) && !(isDeterministicAction(eventInfo.getUmlElement()))
                && eventInfo.isEndEvent();
    }

    /**
     * Checks whether the given non-{@code null} UML element belongs to the elements contained in the pre-synthesis UML
     * model.
     *
     * @param umlElement The non-{@code null} UML element to check.
     * @return {@code true} if the given element belongs to the pre-synthesis UML model, {@code false} otherwise.
     */
    private boolean isOriginalUmlElement(RedefinableElement umlElement) {
        Verify.verifyNotNull(umlElement, "Element cannot be 'null'.");

        return cifEventTraceInfo.values().stream()
                .anyMatch(info -> info.getTranslationPurpose().equals(UmlToCifTranslationPurpose.SYNTHESIS)
                        && info.getUmlElement() instanceof RedefinableElement cifEventUmlElement
                        && cifEventUmlElement.equals(umlElement));
    }

    /**
     * Returns the CIF start events whose corresponding original UML element matches the given one.
     *
     * @param umlElement The UML element.
     * @param purpose The translation purpose.
     * @return The list of CIF start events whose corresponding original UML element matches the given one.
     */
    public List<Event> getStartEventsCorrespondingToOriginalUmlElement(RedefinableElement umlElement,
            UmlToCifTranslationPurpose purpose)
    {
        List<Event> cifEvents;
        if (purpose == UmlToCifTranslationPurpose.SYNTHESIS) {
            cifEvents = getStartEventMap(purpose).entrySet().stream()
                    .filter(e -> e.getValue() instanceof RedefinableElement element && element.equals(umlElement))
                    .map(Map.Entry::getKey).toList();
        } else {
            cifEvents = getStartEventMap(purpose).entrySet().stream()
                    .filter(e -> e.getValue() instanceof RedefinableElement element
                            // Check the original UML element.
                            && getOriginalUmlElement(element) != null
                            && getOriginalUmlElement(element).equals(umlElement))
                    .map(Map.Entry::getKey).toList();
        }
        return cifEvents;
    }

    /**
     * Removes from the CIF event tracing map the events contained in the given set. If an event is a start event, also
     * its corresponding end events are removed. If all end events of a start event are removed, it updates the
     * corresponding start events tracing info with {@code isStartEvent} and {@code isEndEvent} both set to
     * {@code true}.
     *
     * @param cifEventNamesToRemove The set of names of CIF end events.
     * @param purpose The translation purpose.
     */
    public void removeAndUpdateEvents(Set<String> cifEventNamesToRemove, UmlToCifTranslationPurpose purpose) {
        // Get the map from start events to the corresponding end events.
        Map<Event, List<Event>> startEndEventsMap = getStartEndEventMap(purpose);

        // Create a map from event names to CIF events, for better handling CIF event names.
        Map<String, Event> namesToCifEvents = cifEventTraceInfo.keySet().stream()
                .collect(Collectors.toMap(e -> e.getName(), e -> e));

        // If the event is a start event, add it to the set of events to be removed, together with the corresponding end
        // events. If the event is an end event, add it to the set of event to be removed and store the corresponding
        // start event for later handling.
        Set<Event> eventsToRemove = new LinkedHashSet<>();
        Set<Event> startEventsToUpdate = new LinkedHashSet<>();
        for (String eventName: cifEventNamesToRemove) {
            Event cifEvent = namesToCifEvents.get(eventName);
            Verify.verifyNotNull(cifEvent, "Could not find CIF event '" + eventName + "'.");

            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            Verify.verifyNotNull(eventInfo, "CIF event '" + eventName + "' does not have any tracing info.");
            if (eventInfo.isStartEvent()) {
                // Store the start event and corresponding end events to be removed.
                eventsToRemove.add(cifEvent);
                eventsToRemove.addAll(startEndEventsMap.get(cifEvent));
            } else if (eventInfo.isEndEvent()) {
                // Store the event to be removed, find the corresponding start event for later handling.
                eventsToRemove.add(cifEvent);
                List<Event> startToUpdate = startEndEventsMap.entrySet().stream()
                        .filter(e -> e.getValue().contains(cifEvent)).map(e -> e.getKey()).toList();
                Verify.verify(startToUpdate.size() == 1,
                        String.format("Found %d start events for end event '%s'.", startToUpdate.size(), eventName));
                startEventsToUpdate.add(startToUpdate.get(0));
            }
        }

        // Remove the CIF event trace info for the events that are to be removed.
        cifEventTraceInfo.keySet().removeAll(eventsToRemove);

        // Handle the start events to be updated: if all the corresponding end events have been removed, update its CIF
        // event trace info to also make it an end event.
        for (Event startEvent: startEventsToUpdate) {
            // If all end events have been removed, update the CIF event trace info.
            if (startEndEventsMap.get(startEvent).stream().allMatch(e -> eventsToRemove.contains(e))) {
                // Create a new 'EventTraceInfo' with 'isEndEvent' set to 'true' and overwrite the info in the map.
                EventTraceInfo oldEventTraceInfo = getEventTraceInfo(startEvent);
                EventTraceInfo newEventTraceInfo = new EventTraceInfo(oldEventTraceInfo.getTranslationPurpose(),
                        oldEventTraceInfo.getUmlElement(), oldEventTraceInfo.getEffectIdx(), true, true);
                cifEventTraceInfo.put(startEvent, newEventTraceInfo);
            }
        }
    }

    /** Tracing information related to a CIF event. */
    public class EventTraceInfo {
        /** The translation purpose of the CIF event. */
        private final UmlToCifTranslationPurpose purpose;

        /** The UML element related to the CIF event, or {@code null} if no such element exists. */
        private final RedefinableElement umlElement;

        /**
         * The effect index. It is {@code null} for events that are both start and end events, as well as for start-only
         * events. End-only events must have a non-negative integer effect index.
         */
        private final Integer effectIdx;

        /** {@code true} if the event represents a start event, {@code false} otherwise. */
        private final boolean isStartEvent;

        /** {@code true} if the event represents an end event, {@code false} otherwise. */
        private final boolean isEndEvent;

        /**
         * Constructs a new {@link EventTraceInfo}.
         *
         * @param purpose The translation purpose.
         * @param umlElement The UML element that relates to the CIF event, or {@code null} if no such element exists.
         * @param effectIdx The effect index. It is {@code null} for events that are both start and end events, as well
         *     as for start-only events. End-only events must have a non-negative integer effect index.
         * @param isStartEvent {@code true} if the event represents a start event, {@code false} otherwise.
         * @param isEndEvent {@code true} if the event represents an end event, {@code false} otherwise.
         */
        private EventTraceInfo(UmlToCifTranslationPurpose purpose, RedefinableElement umlElement, Integer effectIdx,
                boolean isStartEvent, boolean isEndEvent)
        {
            Verify.verify(isStartEvent || isEndEvent, "Event must be a either start event, or an end event, or both.");
            Verify.verify((effectIdx != null) == (!isStartEvent && isEndEvent),
                    "Events that are both start and end events, as well as start-only events, must have null effect index. "
                            + "End-only events must have integer effect index.");
            Verify.verify(effectIdx == null || effectIdx >= 0, "Effect index must not be negative.");
            Verify.verify(effectIdx == null || umlElement != null,
                    "UML element must be non-null if effect index is non-null.");

            this.purpose = purpose;
            this.umlElement = umlElement;
            this.effectIdx = effectIdx;
            this.isStartEvent = isStartEvent;
            this.isEndEvent = isEndEvent;
        }

        /**
         * Returns the translation purpose of the CIF event.
         *
         * @return The translation purpose.
         */
        private UmlToCifTranslationPurpose getTranslationPurpose() {
            return purpose;
        }

        /**
         * Returns the UML element that relates to the CIF event, or {@code null} if no such element exists.
         *
         * @return The related UML element, or {@code null}.
         */
        private RedefinableElement getUmlElement() {
            return umlElement;
        }

        /**
         * Returns the effect index of the UML element originally related to the transition. This transition must be an
         * end-only transition, which then always has a non-negative integer effect index.
         *
         * @return The effect index of the related UML element, or {@code null}.
         */
        private Integer getEffectIdx() {
            return effectIdx;
        }

        /**
         * Returns {@code true} if the event represents a start event, {@code false} otherwise.
         *
         * @return {@code true} if the event represents a start event, {@code false} otherwise.
         */
        public boolean isStartEvent() {
            return isStartEvent;
        }

        /**
         * Returns {@code true} if the event represents an end event, {@code false} otherwise.
         *
         * @return {@code true} if the event represents an end event, {@code false} otherwise.
         */
        private boolean isEndEvent() {
            return isEndEvent;
        }

        /**
         * Indicates whether the CIF event is a complete event, i.e., represents both the start and the end of an
         * action.
         *
         * @return {@code true} if the CIF event is both a start and an end event, {@code false} otherwise.
         */
        private boolean isCompleteEvent() {
            return isStartEvent() && isEndEvent();
        }

        /**
         * Indicates whether the CIF event is a start-only event, i.e., represents the start of an action but does not
         * represent the end of the action.
         *
         * @return {@code true} if the CIF event is a start-only event, {@code false} otherwise.
         */
        private boolean isStartOnlyEvent() {
            return isStartEvent() && !isEndEvent();
        }

        /**
         * Indicates whether the CIF event is a end-only event, i.e., represents the end of an action but does not
         * represent the start of the action.
         *
         * @return {@code true} if the CIF event is a end-only event, {@code false} otherwise.
         */
        private boolean isEndOnlyEvent() {
            return !isStartEvent() && isEndEvent();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Section dealing with Petri net transitions.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates the map from Petri net transitions to CIF events, provided that the map from CIF event to their tracing
     * info is not empty.
     *
     * @param petriNet The Petri net.
     */
    public void addPetriNetTransitions(PetriNet petriNet) {
        Verify.verify(!cifEventTraceInfo.isEmpty(), "The map from CIF event names to their tracing infos is empty.");

        // Create a map from event names to CIF events, for better handling CIF event names.
        Map<String, Event> namesToCifEvents = cifEventTraceInfo.keySet().stream()
                .collect(Collectors.toMap(e -> e.getName(), e -> e));

        // Get Petri net transition list.
        List<Transition> petriNetTransitions = petriNet.getPages().stream()
                .flatMap(p -> p.getObjects().stream().filter(o -> o instanceof Transition).map(Transition.class::cast))
                .toList();

        for (Transition t: petriNetTransitions) {
            // Store the transition and the related CIF event.
            Event cifEvent = namesToCifEvents.get(t.getName().getText());
            Verify.verify(cifEvent != null, "Could not find CIF event for transition '" + t.getName().getText() + "'.");
            TransitionTraceInfo transitionInfo = new TransitionTraceInfo(Set.of(cifEvent));
            transitionTraceInfo.put(t, transitionInfo);
        }
    }

    /**
     * Merges the transitions composing a pattern. The end transitions' entries are removed from the tracker's internal
     * map. The start transition's entry gets an updated tracing info, storing all the merged events.
     *
     * @param startEndTransitions The map from transition related to a start CIF event to the transitions related to the
     *     corresponding CIF end events to be merged.
     */
    public void mergeTransitionPatterns(Map<Transition, List<Transition>> startEndTransitions) {
        for (Entry<Transition, List<Transition>> startEndTransition: startEndTransitions.entrySet()) {
            Transition startTransition = startEndTransition.getKey();
            List<Transition> endTransitions = startEndTransition.getValue();

            // Collect the start event and the end events.
            Set<Event> patternEvents = new LinkedHashSet<>();
            patternEvents.addAll(transitionTraceInfo.get(startTransition).getCifEvents());
            patternEvents.addAll(
                    endTransitions.stream().flatMap(t -> transitionTraceInfo.get(t).getCifEvents().stream()).toList());

            // Create a new transition tracing info.
            TransitionTraceInfo mergedTransitionInfo = new TransitionTraceInfo(patternEvents);

            // Remove end transitions' entries from the transition map.
            transitionTraceInfo.keySet().removeAll(endTransitions);

            // Update start transition entry with the merged transition tracing info.
            transitionTraceInfo.put(startTransition, mergedTransitionInfo);
        }
    }

    /** Tracing information related to a Petri net transition. */
    class TransitionTraceInfo {
        /**
         * The CIF events related to the Petri net transition. If the set contains only a single event, this can be
         * either a start or an end event (or both). If the set contains multiple events, these must compose a complete
         * "pattern", i.e. one single start-only event along with all its related end-only events.
         */
        private final Set<Event> cifEvents;

        /**
         * Create a new transition trace info, after some validation checks. If the input CIF event set contains only a
         * single event, this can be either a start or an end event (or both). If the set contains multiple events,
         * these must compose a complete "pattern", i.e. one single start-only event along with all its related end-only
         * events.
         *
         * @param cifEvents The set of CIF events for the tracing info.
         */
        private TransitionTraceInfo(Set<Event> cifEvents) {
            Verify.verifyNotNull(cifEvents, "CIF event set cannot be null.");
            Verify.verify(cifEvents.size() > 0, "CIF event set cannot be empty.");
            Verify.verify(cifEventTraceInfo.keySet().containsAll(cifEvents),
                    "All CIF events must be contained in the CIF event tracing info map.");

            if (cifEvents.size() > 1) {
                // The events must compose a pattern: single start-only event, one or more end-only events, all
                // referring to the same UML element, all with the same translation purpose, and the effect indexes that
                // are coherent with the UML element effects cardinality.
                List<Event> startEvents = cifEvents.stream().filter(e -> getEventTraceInfo(e).isStartOnlyEvent())
                        .toList();
                Verify.verify(startEvents.size() == 1, String.format("Found %d start-only events within events '%s'.",
                        startEvents.size(), String.join(",", cifEvents.stream().map(e -> e.getName()).toList())));

                List<Event> endEvents = cifEvents.stream().filter(e -> getEventTraceInfo(e).isEndOnlyEvent()).toList();
                Verify.verify(endEvents.size() >= 1, "There must be at least one end-only event.");

                Verify.verify(startEvents.size() + endEvents.size() == cifEvents.size(),
                        "Events that are both start- and end-events are not supported for merged patterns.");

                Set<RedefinableElement> umlElements = cifEvents.stream().map(e -> getEventTraceInfo(e).getUmlElement())
                        .collect(Collectors.toSet());
                Verify.verify(umlElements.size() == 1,
                        String.format("Events must refer to a single UML element, found %d.", umlElements.size()));

                Verify.verify(
                        cifEvents.stream()
                                .allMatch(e -> getEventTraceInfo(e).getTranslationPurpose()
                                        .equals(UmlToCifTranslationPurpose.SYNTHESIS)),
                        "All events must have 'synthesis' translation purpose.");

                // Collect all effect indexes and the number of effects of the UML element. Check if the CIF events
                // tracing info effect indexes are the same numbers as the UML element's effects. Verify that there are
                // no additional effect indexes.
                Set<Integer> eventsEffectIdxs = endEvents.stream().map(e -> getEventTraceInfo(e).getEffectIdx())
                        .collect(Collectors.toSet());
                int umlElemEffectSize = PokaYokeUmlProfileUtil
                        .getEffects(getEventTraceInfo(cifEvents.iterator().next()).getUmlElement()).size();
                for (int i = 0; i < umlElemEffectSize; i++) {
                    Verify.verify(eventsEffectIdxs.contains(i),
                            String.format("Effect index %d of UML element '%s' is missing.", i,
                                    getEventTraceInfo(cifEvents.iterator().next()).getUmlElement().getName()));
                    eventsEffectIdxs.remove(i);
                }
                Verify.verify(eventsEffectIdxs.isEmpty(),
                        String.format("The set of CIF events contains unexpected indexes: %s.",
                                String.join(", ", eventsEffectIdxs.stream().map(i -> String.valueOf(i)).toList())));
            }

            this.cifEvents = cifEvents;
        }

        /**
         * Returns the set of CIF events linked to the transition.
         *
         * @return The CIF events.
         */
        private Set<Event> getCifEvents() {
            return Collections.unmodifiableSet(cifEvents);
        }

        /**
         * Indicates whether the transition relates to a merged (rewritten) non-atomic pattern.
         *
         * @return {@code true} if the transition is merged, {@code false} otherwise.
         */
        private boolean isMergedTransition() {
            // If the transition tracing info contains more than one event, it represent a merged (rewritten) pattern.
            return cifEvents.size() > 1;
        }

        /**
         * Indicates whether the transition relates to a CIF event that is both a start and end event (e.g. related to
         * an atomic behavior) or if the transition relates to a merged (rewritten) non-atomic pattern.
         *
         * @return {@code true} if the transition is merged or is related to a start and end CIF event, {@code false}
         *     otherwise.
         */
        private boolean isCompleteTransition() {
            // If the transition is not merged, it has a single CIF event, and we can query if that is complete.
            Event cifEvent = cifEvents.iterator().next();
            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            return isMergedTransition() || eventInfo.isCompleteEvent();
        }

        /**
         * Indicates whether the transition relates to a CIF event that is a start-only event, i.e., represents the
         * start of an action but does not represent the end of the action.
         *
         * @return {@code true} if the transition is start-only, {@code false} otherwise.
         */
        private boolean isStartOnlyTransition() {
            if (isMergedTransition()) {
                return false;
            }

            // If the transition is not merged, it has a single CIF event, and we can query if that is start-only.
            Event cifEvent = cifEvents.iterator().next();
            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            return eventInfo.isStartOnlyEvent();
        }

        /**
         * Indicates whether the transition relates to a CIF event that is a end-only event, i.e., represents the end of
         * an action but does not represent the start of the action.
         *
         * @return {@code true} if the transition is end-only, {@code false} otherwise.
         */
        private boolean isEndOnlyTransition() {
            if (isMergedTransition()) {
                return false;
            }

            // If the transition is not merged, it has a single CIF event, and we can query if that is end-only.
            Event cifEvent = cifEvents.iterator().next();
            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            return eventInfo.isEndOnlyEvent();
        }

        /**
         * Returns the UML element originally related to the transition, or {@code null} if no such element exists.
         *
         * @return The related UML element, or {@code null}.
         */
        private RedefinableElement getUmlElement() {
            // If the transition is not merged, it has a single CIF event, and we can query the UML element related to
            // it. If the transition is merged, all CIF events are related to the same UML element, so we can query the
            // first one.
            Event cifEvent = cifEvents.iterator().next();
            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            return eventInfo.getUmlElement();
        }

        /**
         * Returns the effect index of the UML element originally related to the transition, if it is end-only. End-only
         * events must have a non-negative integer effect index.
         *
         * @return The effect index of the related UML element.
         */
        private int getEffectIdx() {
            // Sanity check: the transition should be related to a end-only CIF event.
            Verify.verify(isEndOnlyTransition(), "Effect index is valid exlusively for end-only CIF events.");

            // The transition is not merged, thus it has a single CIF event, and we query its related effect index.
            Event cifEvent = cifEvents.iterator().next();
            EventTraceInfo eventInfo = getEventTraceInfo(cifEvent);
            return eventInfo.getEffectIdx();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Section dealing with newly generated opaque actions.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Stores the non-finalized UML opaque actions and the Petri net transitions they originate from. A non-finalized
     * opaque action represents a 'placeholder' UML element, that will later be finalized in the synthesis chain. It has
     * no guard nor effects. It can be finalized into an opaque action with guard and/or effects, or into a call
     * behavior action. The synthesis tracker stores the non-finalized opaque actions even when they might be destroyed
     * in the finalization synthesis chain step, and might no longer be present in the intermediate and final UML
     * models.
     *
     * @param transitionActionMap The map from Petri net transitions to UML actions.
     */
    public void addActions(Map<Transition, Action> transitionActionMap) {
        // Sanity check: ensure that there are no duplicate actions before reversing the map.
        Verify.verify(
                transitionActionMap.values().stream().collect(Collectors.toSet()).size() == transitionActionMap.size(),
                "Found duplicate actions in the transition-action map.");

        transitionActionMap.entrySet().stream()
                .forEach(e -> actionToTransition.put((OpaqueAction)e.getValue(), e.getKey()));
    }

    /**
     * Returns the Petri net transition tracing info corresponding to the input opaque action.
     *
     * @param action The opaque action.
     * @return The transition tracing info related to the opaque action.
     */
    private TransitionTraceInfo getTransitionTraceInfo(OpaqueAction action) {
        Transition transition = actionToTransition.get(action);
        Verify.verifyNotNull(transition, String
                .format("Opaque action '%s' does not have a corresponding Petri net transition.", action.getName()));
        TransitionTraceInfo transitionInfo = transitionTraceInfo.get(transition);
        Verify.verifyNotNull(transitionInfo,
                String.format("Transition '%s' does not have any tracing info.", transition.getName().getText()));
        return transitionInfo;
    }

    /**
     * Returns the kind of activity node the given action should translate to, based on the UML element the action
     * originates from, and whether its parts were merged back during the synthesis chain.
     *
     * @param action The UML action created in the body of the newly synthesized activity.
     * @return An enumeration defining the kind of activity node the action should translate to.
     */
    public ActionKind getActionKind(OpaqueAction action) {
        TransitionTraceInfo transitionInfo = getTransitionTraceInfo(action);

        if (transitionInfo.getUmlElement() instanceof OpaqueBehavior) {
            if (transitionInfo.isCompleteTransition()) {
                return ActionKind.COMPLETE_OPAQUE_BEHAVIOR;
            } else if (transitionInfo.isStartOnlyTransition()) {
                return ActionKind.START_OPAQUE_BEHAVIOR;
            } else {
                Verify.verify(transitionInfo.isEndOnlyTransition(), "Expected an end-only event.");
                return ActionKind.END_OPAQUE_BEHAVIOR;
            }
        }

        return ActionKind.CONTROL_NODE;
    }

    /**
     * Returns the UML element originally related to the opaque action, or {@code null} if no such element exists.
     *
     * @param action The opaque action.
     * @return The related UML element, or {@code null}.
     */
    public RedefinableElement getUmlElement(OpaqueAction action) {
        return getTransitionTraceInfo(action).getUmlElement();
    }

    /**
     * Returns the effect index of the UML element originally related to the opaque action. It is {@code null} for
     * events that are both start and end events, as well as for start-only events. End-only events must have a
     * non-negative integer effect index.
     *
     * @param action The opaque action.
     * @return The effect index of the related UML element, or {@code null}.
     */
    public int getEffectIdx(OpaqueAction action) {
        return getTransitionTraceInfo(action).getEffectIdx();
    }

    /**
     * Returns {@code true} if the Petri net transition related to the opaque action is a start-only transition.
     *
     * @param action The opaque action.
     * @return {@code true} if the action is related to a start-only transition, {@code false} otherwise.
     */
    private boolean isStartOnlyAction(OpaqueAction action) {
        return getTransitionTraceInfo(action).isStartOnlyTransition();
    }

    /**
     * Returns {@code true} if the Petri net transition related to the opaque action is an end-only transition.
     *
     * @param action The opaque action.
     * @return {@code true} if the action is related to an end-only transition, {@code false} otherwise.
     */
    private boolean isEndOnlyAction(OpaqueAction action) {
        return getTransitionTraceInfo(action).isEndOnlyTransition();
    }

    /**
     * Returns {@code true} if the Petri net transition related to the opaque action is a complete transition.
     *
     * @param action The opaque action.
     * @return {@code true} if the action is related to a complete transition, {@code false} otherwise.
     */
    private boolean isCompleteAction(OpaqueAction action) {
        return getTransitionTraceInfo(action).isCompleteTransition();
    }

    /**
     * Removes the internal actions created for petrification in the internal mappings. Specifically targeted at the
     * single source and single sink "__start" and "__end" events.
     */
    public void removeInternalActions() {
        // Removes internal actions created for petrification. Store the related transitions.
        Set<Action> actionsToRemove = new LinkedHashSet<>();
        Set<Transition> transitionsToRemove = new LinkedHashSet<>();
        for (Entry<OpaqueAction, Transition> entry: actionToTransition.entrySet()) {
            if (entry.getKey().getName().contains("__") && getUmlElement(entry.getKey()) == null) {
                actionsToRemove.add(entry.getKey());
                transitionsToRemove.add(entry.getValue());
            }
        }
        actionToTransition.keySet().removeAll(actionsToRemove);

        // Remove the transitions corresponding to internal actions. Store the corresponding CIF events.
        Set<Event> eventsToRemove = transitionsToRemove.stream()
                .flatMap(t -> transitionTraceInfo.get(t).getCifEvents().stream()).collect(Collectors.toSet());
        transitionTraceInfo.keySet().removeAll(transitionsToRemove);

        // Remove the corresponding CIF events.
        cifEventTraceInfo.keySet().removeAll(eventsToRemove);
    }

    /**
     * Returns {@code true} if the opaque action is internal, i.e. the corresponding UML element is {@code null}.
     *
     * @param action The opaque action.
     * @return {@code true} if the action is internal, {@code false} otherwise.
     */
    public boolean isInternalAction(OpaqueAction action) {
        // Internal actions (e.g. control nodes) do not have any UML element to refer to.
        return getUmlElement(action) == null;
    }

    /**
     * Returns the set of internal actions.
     *
     * @return The set of internal actions.
     */
    public Set<OpaqueAction> getInternalActions() {
        return actionToTransition.keySet().stream().filter(a -> isInternalAction(a))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Section dealing with finalized UML elements and synthesized UML elements.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Registers that the given finalized UML element has been created, as the result of the finalization of the given
     * opaque action. A finalized UML element represents an element belonging to the synthesized activity, and can be of
     * two kinds:
     * <ul>
     * <li>it is an opaque action including guard and effects, if it represents a start-only or end-only event</li>
     * <li>it is a call behavior, if it represents a complete event</li>
     * </ul>
     *
     * @param finalizedElement The finalized UML element.
     * @param action The opaque action.
     */
    public void addFinalizedUmlElement(RedefinableElement finalizedElement, OpaqueAction action) {
        // Sanity check: ensure that the finalized UML element and the opaque action are not present in the map.
        Verify.verify(!finalizedElementToAction.containsKey(finalizedElement), String.format(
                "Finalized UML element '%s' is already contained in the tracker mapping.", finalizedElement.getName()));
        Verify.verify(!finalizedElementToAction.values().contains(action), String.format(
                "Action '%s' is already contained in the finalized UML element tracker mapping.", action.getName()));
        Verify.verify(finalizedElement instanceof OpaqueAction || finalizedElement instanceof CallBehaviorAction,
                "Expected a finalized UML element to be either an opaque action or a call behavior action.");

        finalizedElementToAction.put(finalizedElement, action);
    }

    /**
     * Returns {@code true} if the given UML element is a finalized element, {@code false} otherwise.
     *
     * @param umlElement The UML element.
     * @return {@code true} if the given UML element is a finalized element, {@code false} otherwise.
     */
    private boolean isFinalizedUmlElement(RedefinableElement umlElement) {
        return finalizedElementToAction.containsKey(umlElement);
    }

    /**
     * Checks whether the given non-{@code null} UML element belongs to the elements contained in the synthesized UML
     * activity.
     *
     * @param umlElement The non-{@code null} UML element to check.
     * @return {@code true} if the input element belongs to the synthesized activity, {@code false} otherwise.
     */
    private boolean belongsToSynthesizedActivity(RedefinableElement umlElement) {
        Verify.verifyNotNull(umlElement, "Element cannot be 'null'.");

        return cifEventTraceInfo.values().stream()
                .anyMatch(info -> !info.getTranslationPurpose().equals(UmlToCifTranslationPurpose.SYNTHESIS)
                        && info.getUmlElement() instanceof RedefinableElement cifEventUmlElement
                        && cifEventUmlElement.equals(umlElement));
    }

    /**
     * Returns the original UML element for which the given UML element in the synthesized activity was created, or
     * {@code null} if no such element exists.
     *
     * @param umlElement The UML element in the synthesized activity.
     * @return The related original UML element, or {@code null} if no such UML element exists.
     */
    private RedefinableElement getOriginalUmlElement(RedefinableElement umlElement) {
        // Precondition check.
        Verify.verify(belongsToSynthesizedActivity(umlElement),
                String.format("UML element '%s' does not belong to the synthesized activity.", umlElement.getName()));

        OpaqueAction action = finalizedElementToAction.get(umlElement);
        return (action == null) ? null : getUmlElement(action);
    }

    /**
     * Returns the non-finalized opaque action corresponding to the given finalized UML element.
     *
     * @param umlElement The finalized UML element.
     * @return The corresponding opaque action.
     */
    private OpaqueAction getOpaqueAction(RedefinableElement umlElement) {
        Verify.verify(isFinalizedUmlElement(umlElement),
                String.format("Element '%s' is not a finalized element.", umlElement.getName()));
        OpaqueAction action = finalizedElementToAction.get(umlElement);
        Verify.verifyNotNull(action, String.format(
                "Element '%s' does not have a corresponding non-finalized opaque action.", umlElement.getName()));
        return action;
    }

    /**
     * Returns {@code true} if the finalized UML element contained in the given event trace info is related to an
     * original CIF start-only event or if it is related to a merged pattern and it is related to a current CIF start
     * event; {@code false} otherwise.
     *
     * @param finalizedEventInfo The event trace info corresponding to the finalized UML element.
     * @return {@code true} if the given UML element is related to an original CIF start-only event or if it is related
     *     to a merged pattern and it is related to a current CIF start event, {@code false} otherwise.
     */
    private boolean isRelatedToStartOfOriginalElement(EventTraceInfo finalizedEventInfo) {
        RedefinableElement finalizedUmlElement = finalizedEventInfo.getUmlElement();
        Verify.verify(isFinalizedUmlElement(finalizedUmlElement),
                String.format("Element '%s' is not a finalized UML element.", finalizedUmlElement.getName()));
        // This handles three cases: when a non-atomic pattern is not merged, it can link back to the original CIF event
        // and check that it is a start-only event; when a non-atomic pattern is merged, it links to an original
        // complete CIF event while the current CIF event is a start-only event; an atomic behavior, that results in
        // complete original CIF event and a complete current CIF event. These last two cases can be combined, checking
        // that the original CIF event is complete and the current CIF event is a start event (i.e. start-only or
        // complete).
        return isRelatedToOriginalStartOnlyEvent(finalizedUmlElement)
                || (isRelatedToOriginalCompleteEvent(finalizedUmlElement) && finalizedEventInfo.isStartEvent());
    }

    /**
     * Returns {@code true} if the finalized UML element contained in the given event trace info is related to an
     * original CIF end-only event or if it is related to a merged pattern and it is related to a current CIF end-only
     * event; {@code false} otherwise.
     *
     * @param finalizedEventInfo The event trace info corresponding to the finalized UML element.
     * @return {@code true} if the given UML element is related to an original CIF end-only event or if it is related to
     *     a merged pattern and it is related to a current CIF end-only event, {@code false} otherwise.
     */
    private boolean isRelatedToEndOnlyOfOriginalElement(EventTraceInfo finalizedEventInfo) {
        RedefinableElement finalizedUmlElement = finalizedEventInfo.getUmlElement();
        Verify.verify(isFinalizedUmlElement(finalizedUmlElement),
                String.format("Element '%s' is not a finalized UML element.", finalizedUmlElement.getName()));
        // This handles two cases: when a non-atomic pattern is not merged, it can link back to the original CIF event
        // and check that it is an end-only event; when a non-atomic pattern is merged, it links to an original
        // complete CIF event while the current CIF event is an end-only event.
        return isRelatedToOriginalEndOnlyEvent(finalizedUmlElement)
                || (isRelatedToOriginalCompleteEvent(finalizedUmlElement) && finalizedEventInfo.isEndOnlyEvent());
    }

    /**
     * Returns {@code true} if the given CIF event has been created for a finalized UML element during the guard
     * computation or language equivalence check phase, which corresponds to a CIF event created for the synthesis phase
     * that represents an end-only event.
     *
     * @param cifEvent The CIF event.
     * @param purpose The translation purpose.
     * @return {@code true} if the CIF event corresponds to an original end-only event, {@code false} otherwise.
     */
    public boolean isRelatedToEndOnlyOfOriginalElement(Event cifEvent, UmlToCifTranslationPurpose purpose) {
        // Precondition check.
        Verify.verify(purpose != UmlToCifTranslationPurpose.SYNTHESIS,
                "Reference to original UML element is undefined for synthesis translation.");

        // Check if the event is related to the end-only of an original element.
        EventTraceInfo finalizedEventInfo = cifEventTraceInfo.get(cifEvent);
        Verify.verifyNotNull(finalizedEventInfo, String.format(
                "Event '%s' does not have any tracing info referring to the finalized UML model.", cifEvent.getName()));
        return isRelatedToEndOnlyOfOriginalElement(finalizedEventInfo);
    }

    /**
     * Returns {@code true} if the finalized UML element is related to a start-only original CIF event.
     *
     * @param finalizedUmlElement The finalized UML element.
     * @return {@code true} if the finalized UML element is related to a start-only original CIF event, {@code false}
     *     otherwise.
     */
    private boolean isRelatedToOriginalStartOnlyEvent(RedefinableElement finalizedUmlElement) {
        Verify.verify(isFinalizedUmlElement(finalizedUmlElement),
                String.format("Element '%s' is not a finalized element.", finalizedUmlElement.getName()));
        OpaqueAction action = getOpaqueAction(finalizedUmlElement);
        return isStartOnlyAction(action);
    }

    /**
     * Returns {@code true} if the finalized UML element is related to an end-only original CIF event.
     *
     * @param finalizedUmlElement The finalized UML element.
     * @return {@code true} if the finalized UML element is related to an end-only original CIF event, {@code false}
     *     otherwise.
     */
    private boolean isRelatedToOriginalEndOnlyEvent(RedefinableElement finalizedUmlElement) {
        Verify.verify(isFinalizedUmlElement(finalizedUmlElement),
                String.format("Element '%s' is not a finalized element.", finalizedUmlElement.getName()));
        OpaqueAction action = getOpaqueAction(finalizedUmlElement);
        return isEndOnlyAction(action);
    }

    /**
     * Returns {@code true} if the finalized UML element is related to a complete (both start and end) original CIF
     * event.
     *
     * @param finalizedUmlElement The finalized UML element.
     * @return {@code true} if the finalized UML element is related to a complete (both start and end) original CIF
     *     event, {@code false} otherwise.
     */
    private boolean isRelatedToOriginalCompleteEvent(RedefinableElement finalizedUmlElement) {
        Verify.verify(isFinalizedUmlElement(finalizedUmlElement),
                String.format("Element '%s' is not a finalized element.", finalizedUmlElement.getName()));
        OpaqueAction action = getOpaqueAction(finalizedUmlElement);
        return isCompleteAction(action);
    }

    /**
     * Returns {@code true} if the given CIF event has been created for a finalized UML element during the guard
     * computation or language equivalence check phase, which corresponds to a CIF event created for the synthesis phase
     * that represents the start event of an original opaque behavior.
     *
     * @param cifEvent The CIF event.
     * @param purpose The translation purpose.
     * @return {@code true} if the CIF event corresponds to the start of an original opaque behavior, {@code false}
     *     otherwise.
     */
    public boolean isStartOfOriginalOpaqueBehavior(Event cifEvent, UmlToCifTranslationPurpose purpose) {
        // Precondition check.
        Verify.verify(purpose != UmlToCifTranslationPurpose.SYNTHESIS,
                "Reference to original UML element is undefined for synthesis translation.");

        // Check if the event is related to the start of an original opaque behavior.
        EventTraceInfo finalizedEventInfo = cifEventTraceInfo.get(cifEvent);
        Verify.verifyNotNull(finalizedEventInfo, String.format(
                "Event '%s' does not have any tracing info referring to the finalized UML model.", cifEvent.getName()));
        RedefinableElement finalizedUmlElement = finalizedEventInfo.getUmlElement();
        return getOriginalUmlElement(finalizedUmlElement) instanceof OpaqueBehavior
                && isRelatedToStartOfOriginalElement(finalizedEventInfo);
    }

    /**
     * Gives the list of CIF start events created for any finalized UML element during the guard computation or language
     * equivalence check phase, that corresponds to the given original UML element.
     *
     * @param originalUmlElement The non-{@code null} original UML element.
     * @param purpose The translation purpose.
     * @return The list of CIF start events of finalized UML elements corresponding to the given original UML element.
     */
    public List<Event> getFinalizedElementStartEventsForOriginalElement(RedefinableElement originalUmlElement,
            UmlToCifTranslationPurpose purpose)
    {
        // Precondition check.
        Verify.verify(purpose != UmlToCifTranslationPurpose.SYNTHESIS,
                "Reference to original UML element is undefined for synthesis translation.");
        Verify.verify(isOriginalUmlElement(originalUmlElement),
                "The given UML element is not an original UML element.");

        // Get the list of CIF events whose translation purpose is the given one, whose original UML element is equal to
        // the given one, and is related to the start of an original element.
        List<Event> filteredEvents = cifEventTraceInfo.entrySet().stream().filter(e ->
        // Filter to only events with given translation purpose.
        e.getValue().getTranslationPurpose().equals(purpose)
                // Filter to only redefinable elements (avoid 'null' for control nodes).
                && getOriginalUmlElement(e.getValue().getUmlElement()) instanceof RedefinableElement umlElement
                // Filter to only UML elements that are equal to the original UML element.
                && umlElement.equals(originalUmlElement)
                // Filter to only CIF events related to the start of an original element.
                && isRelatedToStartOfOriginalElement(e.getValue()))
                // Collect the CIF events.
                .map(Map.Entry::getKey).toList();

        return filteredEvents;
    }
}
