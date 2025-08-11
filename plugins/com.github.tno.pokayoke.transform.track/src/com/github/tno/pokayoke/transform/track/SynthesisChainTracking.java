
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;

/**
 * Tracks the activity synthesis chain transformations from the UML elements of the input model, to their translation to
 * CIF events, the translation to Petri net transitions, to the synthesized activity UML opaque actions, to the UML to
 * CIF transformation needed for guard computation and the UML to CIF transformation for the language equivalence check.
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

    /**
     * Registers that the given CIF event has been created for the given UML element for the indicated translation
     * purpose.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to relate to the CIF event.
     * @param effectIdx The effect index, which can either be a positive integer when relevant, or {@code null} when
     *     irrelevant (e.g., in case the CIF event is a start event of a non-atomic action).
     * @param purpose The translation purpose.
     * @param isStartEvent {@code true} if the event represents a start event, {@code false} otherwise.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, Integer effectIdx,
            UmlToCifTranslationPurpose purpose, boolean isStartEvent)
    {
        cifEventTraceInfo.put(cifEvent, new EventTraceInfo(purpose, umlElement, effectIdx, isStartEvent));
    }

    /**
     * Gives the map from CIF start events to the corresponding UML elements for the specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding UML elements for the specified translation purpose.
     */
    public Map<Event, RedefinableElement> getStartEventMap(UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && e.getValue().isStartEvent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().umlElement()));
    }

    /**
     * Indicates whether the CIF event is a start event, i.e., represents the start of an action.
     *
     * @param cifEvent The CIF event.
     * @return {@code true} if the CIF event is a start event, {@code false} otherwise.
     */
    public boolean isStartEvent(Event cifEvent) {
        return cifEventTraceInfo.get(cifEvent).isStartEvent();
    }

    /**
     * Returns the events corresponding to the given set of UML elements, based on the indicated translation purpose.
     *
     * @param umlElements The set of UML elements, to find the related CIF events.
     * @param purpose The translation purpose.
     * @return The list of CIF events corresponding to the UML elements.
     */
    public List<Event> getEventsOf(Set<? extends RedefinableElement> umlElements, UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && umlElements.contains(e.getValue().umlElement()))
                .map(Map.Entry::getKey).toList();
    }

    /**
     * Gives the list of CIF start events corresponding to the given UML element for the specified translation purpose.
     * Not yet supported for guard computation and language equivalence check.
     *
     * @param umlElement The UML element.
     * @param purpose The translation purpose.
     * @return The list of CIF start events corresponding to the given UML element.
     */
    public List<Event> getStartEventsOf(RedefinableElement umlElement, UmlToCifTranslationPurpose purpose) {
        if (purpose != UmlToCifTranslationPurpose.SYNTHESIS) {
            throw new RuntimeException("Unsupported translation purpose: " + purpose + ".");
        }

        return cifEventTraceInfo.entrySet().stream().filter(e -> e.getValue().purpose().equals(purpose)
                && e.getValue().isStartEvent() && e.getValue().umlElement().equals(umlElement)).map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns the map from CIF end events to the corresponding UML elements and effect indexes. Only supported for the
     * initial data-based synthesis phase.
     *
     * @return The map from CIF end events generated for the initial synthesis to their corresponding UML elements and
     *     effect indexes.
     */
    public Map<Event, Pair<RedefinableElement, Integer>> getEndEventMap() {
        return cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(UmlToCifTranslationPurpose.SYNTHESIS)
                        && !e.getValue().isStartEvent())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new Pair<>(e.getValue().umlElement(), e.getValue().effectIdx())));
    }

    /**
     * Gives the map from CIF start events of non-atomic actions to the corresponding CIF end events, for the specified
     * translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding CIF end events.
     */
    public Map<Event, List<Event>> getNonAtomicEvents(UmlToCifTranslationPurpose purpose) {
        Map<Event, List<Event>> result = new LinkedHashMap<>();

        // Get the map of all start events.
        Map<Event, RedefinableElement> startEventMap = getStartEventMap(purpose);

        // Get the end events for every non-atomic start event.
        for (Entry<Event, RedefinableElement> entry: startEventMap.entrySet()) {
            Event startEvent = entry.getKey();
            RedefinableElement umlElement = entry.getValue();

            if (isAtomicAction(umlElement)) {
                continue;
            }

            if (result.containsKey(startEvent)) {
                throw new RuntimeException("Expected non-atomic actions to have a single start event.");
            }

            result.put(startEvent, getEndEventsOf(entry.getValue(), purpose));
        }

        return result;
    }

    private List<Event> getEndEventsOf(RedefinableElement umlElement, UmlToCifTranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream().filter(e -> e.getValue().purpose().equals(purpose))
                .filter(e -> e.getValue().umlElement().equals(umlElement)).filter(e -> !e.getValue().isStartEvent())
                .map(e -> e.getKey()).toList();
    }

    private boolean isStartNonAtomicAction(EventTraceInfo eventInfo) {
        return eventInfo.isStartEvent() && !isAtomicAction(eventInfo.umlElement());
    }

    private boolean isAtomicAction(RedefinableElement umlElement) {
        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            return PokaYokeUmlProfileUtil.isAtomic(umlElement);
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            return isAtomicAction(cbAction.getBehavior());
        }

        // If the element does not have the Poka Yoke profile applied nor is a call behavior, it is atomic.
        return true;
    }

    /**
     * Gives the map from CIF start events of non-deterministic actions to the corresponding CIF end events, for the
     * specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding CIF end events.
     */
    public Map<Event, List<Event>> getNonDeterministicEvents(UmlToCifTranslationPurpose purpose) {
        Map<Event, List<Event>> nonDeterministicEvents = new LinkedHashMap<>();
        Map<Event, RedefinableElement> startNonDeterministicEventsToUmlElements = cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && isStartNonDeterministicAction(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().umlElement(), (a, b) -> a,
                        LinkedHashMap::new));

        // For each start event, find the corresponding end events and add them to the map.
        for (Entry<Event, RedefinableElement> startEventAndUmlElement: startNonDeterministicEventsToUmlElements
                .entrySet())
        {
            List<Event> endEvents = cifEventTraceInfo.entrySet().stream()
                    .filter(e -> e.getValue().purpose().equals(purpose) && !e.getValue().isStartEvent()
                            && e.getValue().umlElement().equals(startEventAndUmlElement.getValue()))
                    .map(e -> e.getKey()).toList();
            if (!endEvents.isEmpty()) {
                nonDeterministicEvents.put(startEventAndUmlElement.getKey(), endEvents);
            }
        }

        return nonDeterministicEvents;
    }

    private boolean isStartNonDeterministicAction(EventTraceInfo eventInfo) {
        return eventInfo.isStartEvent() && !isDeterministicAction(eventInfo.umlElement());
    }

    private boolean isDeterministicAction(RedefinableElement umlElement) {
        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            return PokaYokeUmlProfileUtil.isDeterministic(umlElement);
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            return isDeterministicAction(cbAction.getBehavior());
        }

        // If the element does not have the Poka Yoke profile applied nor is a call behavior, it is deterministic.
        return true;
    }

    /**
     * Tracing information related to a CIF event.
     *
     * @param purpose The translation purpose.
     * @param umlElement The UML element that relates to the CIF event.
     * @param effectIdx The effect index, which can either be a positive integer when relevant, or {@code null} when
     *     irrelevant (e.g., in case the CIF event is a start event of a non-atomic action).
     * @param isStartEvent {@code true} if the event represents a start event, {@code false} otherwise.
     */
    private record EventTraceInfo(UmlToCifTranslationPurpose purpose, RedefinableElement umlElement, Integer effectIdx,
            boolean isStartEvent)
    {
    }
}
