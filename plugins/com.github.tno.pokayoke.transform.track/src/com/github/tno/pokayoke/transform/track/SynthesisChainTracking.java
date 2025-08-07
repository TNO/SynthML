
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.RedefinableElement;

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
     * The enumeration that describes the purpose of a UML-to-CIF translation. It is used in the UML-to-CIF translator
     * to decide whether or not to translate certain model elements (e.g. opaque behaviors, occurrence constraints), to
     * generate different pre- and postconditions, to modify the controllability of CIF events. It is used by
     * {@link SynthesisChainTracking the activity synthesis tracker} to store the CIF events for the different
     * UML-to-CIF translations.
     */
    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

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
            TranslationPurpose purpose, boolean isStartEvent)
    {
        cifEventTraceInfo.put(cifEvent, new EventTraceInfo(purpose, umlElement, effectIdx, isStartEvent));
    }

    /**
     * Gives the map from CIF start events to the corresponding UML elements for the specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding UML elements for the specified translation purpose.
     */
    public Map<Event, RedefinableElement> getStartEventMap(TranslationPurpose purpose) {
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
     * Returns the events corresponding to the given set of nodes, based on the indicated translation purpose.
     *
     * @param nodes The set of activity nodes, to find the related CIF events.
     * @param purpose The translation purpose.
     * @return The list of CIF events corresponding to the activity nodes.
     */
    public List<Event> getEventsOf(Set<? extends ActivityNode> nodes, TranslationPurpose purpose) {
        return cifEventTraceInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && nodes.contains(e.getValue().umlElement()))
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
    public List<Event> getStartEventsOf(RedefinableElement umlElement, TranslationPurpose purpose) {
        if (purpose != TranslationPurpose.SYNTHESIS) {
            throw new RuntimeException("Unsupported translation purpose: " + purpose + ".");
        }

        return cifEventTraceInfo.entrySet().stream().filter(e -> e.getValue().purpose().equals(purpose)
                && e.getValue().isStartEvent() && e.getValue().umlElement().equals(umlElement)).map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Return the map from CIF end events to the corresponding UML elements and effect indexes. Only supported for the
     * initial data-based synthesis phase.
     *
     * @return The map from CIF end events generated for the initial synthesis to their corresponding UML elements and
     *     effect indexes.
     */
    public Map<Event, Pair<RedefinableElement, Integer>> getEndEventMap() {
        return cifEventTraceInfo.entrySet().stream().filter(
                e -> e.getValue().purpose().equals(TranslationPurpose.SYNTHESIS) && !e.getValue().isStartEvent())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new Pair<>(e.getValue().umlElement(), e.getValue().effectIdx())));
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
    private record EventTraceInfo(TranslationPurpose purpose, RedefinableElement umlElement, Integer effectIdx,
            boolean isStartEvent)
    {
    }
}
