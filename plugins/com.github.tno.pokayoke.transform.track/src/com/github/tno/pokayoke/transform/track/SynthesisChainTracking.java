
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.RedefinableElement;

/**
 * Tracks the activity synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, to the synthesized activity UML opaque actions, to the UML to CIF
 * transformation needed for guard computation and the UML to CIF transformation for the language equivalence check.
 */
public class SynthesisChainTracking {
    /**
     * The map from CIF events generated for the initial data-based synthesis to their corresponding UML elements of the input model. Gets updated as the
     * activity synthesis chain rewrites, removes, or add events.
     */
    private final Map<Event, RedefinableElement> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The set of internal CIF events (e.g. corresponding to control nodes) generated for the initial data-based synthesis. */
    private final Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    public SynthesisChainTracking() {
        // Empty constructor.
    }
}
