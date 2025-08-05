
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
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
     * The map from CIF events generated for the initial data-based synthesis to a pair composed of their corresponding
     * UML elements of the input model, and the effect index, if relevant. The effect index is either a positive integer
     * when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of a non-atomic
     * action). Gets updated as the activity synthesis chain rewrites, removes, or add events.
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for the guard computation step to a pair composed of their corresponding UML
     * elements of the input model, and the effect index, if relevant. The effect index is either a positive integer
     * when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of a non-atomic
     * action).
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> guardComputationCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for the language equivalence check step to a pair composed of their
     * corresponding UML elements of the input model, and the effect index, if relevant. The effect index is either a
     * positive integer when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of
     * a non-atomic action).
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> languageCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of internal CIF events (e.g. corresponding to control nodes) generated for the initial data-based
     * synthesis.
     */
    private final Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    /**
     * Add a single CIF event. The effect index is either a positive integer when relevant, or {@code null} when
     * irrelevant (e.g., in case the CIF event is a start event of a non-atomic action).
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event.
     * @param effectIdx The effect index. Can be {@code null} if the CIF event is a start event.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, Integer effectIdx) {
        synthesisCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, effectIdx));
    }

    /**
     * Return the map from CIF start events to the corresponding UML elements.
     *
     * @return The map from CIF start events to their corresponding UML elements.
     */
    public Map<Event, RedefinableElement> getStartEventMap() {
        return synthesisCifEventsToUmlElementInfo.entrySet().stream().filter(e -> e.getValue().right == null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().left));
    }
}
