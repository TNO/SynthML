
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
     * UML elements of the input model, and the effect index, if relevant. Gets updated as the activity synthesis chain
     * rewrites, removes, or add events.
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of internal CIF events (e.g. corresponding to control nodes) generated for the initial data-based
     * synthesis.
     */
    private final Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    // Section for methods handling CIF events and UML elements.

    /**
     * Add a single CIF event. It is implied that it represents a start event, as there is no effect index in the input
     * arguments. The effect index is set to -1 by default, to signal that the CIF event is a start event.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement) {
        synthesisCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, -1));
    }

    /**
     * Add a single CIF event. It is implied that it represents an end event, as there is the effect index in the input
     * arguments.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event
     * @param effectIdx The effect index.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, Integer effectIdx) {
        synthesisCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, effectIdx));
    }
}
