
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
     * The map from CIF events to a record composed of their translation purpose, their corresponding UML elements of
     * the input model, and the effect index, if relevant. The effect index is either a positive integer when relevant,
     * or {@code null} when irrelevant (e.g., in case the CIF event is a start event of a non-atomic action). Gets
     * updated as the activity synthesis chain rewrites, removes, or add events.
     */
    private final Map<Event, CifEventInfo> cifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of CIF events representing the start of a UML action/behavior.
     */
    private final Set<Event> cifStartEvents = new LinkedHashSet<>();

    /**
     * The enumeration that describes the purpose of the current UML to CIF translation. It is used in the UML to CIF
     * translator and in the Model to CIF translator to decide whether to translate or not certain model elements (e.g.
     * opaque behaviors, occurrence constraints), to generate different pre- and postconditions, to modify the
     * controllability of CIF events. It is used here to store the CIF events in different maps.
     */
    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

    /**
     * Add a single CIF event. The effect index is either a positive integer when relevant, or {@code null} when
     * irrelevant (e.g., in case the CIF event is a start event of a non-atomic action). Note that even when the action
     * effects are empty, we add a "default" end event, with empty effects and effect index equal to zero.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event.
     * @param effectIdx The effect index. Can be {@code null} if the CIF event is a start event.
     * @param purpose The translation purpose.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, Integer effectIdx,
            TranslationPurpose purpose)
    {
        cifEventsToUmlElementInfo.put(cifEvent, new CifEventInfo(purpose, umlElement, effectIdx));

        if (effectIdx == null) {
            cifStartEvents.add(cifEvent);
        }
    }

    /**
     * Gives the map from CIF start events to the corresponding UML elements for the specified translation purpose.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding UML elements for the specified translation purpose.
     */
    public Map<Event, RedefinableElement> getStartEventMap(TranslationPurpose purpose) {
        return cifEventsToUmlElementInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && e.getValue().effectIdx() == null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().umlElement()));
    }

    /**
     * Indicates whether the CIF event represents a start action for the given translation purpose.
     *
     * @param cifEvent The CIF event.
     * @return {@code true} if the CIF event corresponds to a start event, {@code false} otherwise.
     */
    public boolean isStartEvent(Event cifEvent) {
        return cifStartEvents.contains(cifEvent);
    }

    /**
     * Returns the events corresponding to the input set of nodes, based on the translation purpose.
     *
     * @param nodes The set of activity nodes, to find the related CIF events.
     * @param purpose The translation purpose.
     * @return The list of CIF events corresponding to the activity nodes.
     */
    public List<Event> getNodeEvents(Set<ActivityNode> nodes, TranslationPurpose purpose) {
        return cifEventsToUmlElementInfo.entrySet().stream()
                .filter(e -> e.getValue().purpose().equals(purpose) && nodes.contains(e.getValue().umlElement()))
                .map(Map.Entry::getKey).toList();
    }

    /**
     * Gives the list of CIF start events corresponding to the UML element for the specified translation purpose. Not
     * yet supported for guard computation and language equivalence check.
     *
     * @param umlElement The UML element.
     * @param purpose The translation purpose.
     * @return The list of CIF start events corresponding to the input UML element.
     */
    public List<Event> getStartEventsOf(RedefinableElement umlElement, TranslationPurpose purpose) {
        if (purpose != TranslationPurpose.SYNTHESIS) {
            throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }

        return cifEventsToUmlElementInfo.entrySet().stream().filter(e -> e.getValue().purpose().equals(purpose)
                && isStartEvent(e.getKey()) && e.getValue().umlElement().equals(umlElement)).map(Map.Entry::getKey)
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
        return cifEventsToUmlElementInfo.entrySet().stream().filter(
                e -> e.getValue().purpose().equals(TranslationPurpose.SYNTHESIS) && e.getValue().effectIdx() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new Pair<>(e.getValue().umlElement(), e.getValue().effectIdx())));
    }

    /**
     * The information related to a CIF event: the purpose for which it is translated, the UML element it refers to, the
     * effect index if it is an end event ({@code null} if it is a start event).
     *
     * @param purpose The translation purpose.
     * @param umlElement The UML element to be related to the CIF event.
     * @param effectIdx The effect index. Can be {@code null} if the CIF event is a start event.
     */
    record CifEventInfo(TranslationPurpose purpose, RedefinableElement umlElement, Integer effectIdx) {
    }
}
