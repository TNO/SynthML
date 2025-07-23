
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.RedefinableElement;

import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, and finally to the synthesized activity UML opaque actions (before
 * model finalization).
 */
public class SynthesisChainUmlElementTracking {
    /**
     * The map from CIF events generated for the synthesis to their corresponding UML element info. Does not get updated
     * as the synthesis chain progresses. Needed for the language equivalence check.
     */
    private Map<Event, UmlElementInfo> cifEventsToInitialUmlElementInfo = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for the synthesis to their corresponding UML element info. Gets updated as the
     * synthesis chain rewrites, removes, or add events.
     */
    private Map<Event, UmlElementInfo> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF event names generated for the synthesis to their corresponding CIF events. */
    private Map<String, Event> namesToCifEvents = new LinkedHashMap<>();

    /** The map from Petri net transitions to their corresponding UML element info. */
    private Map<Transition, UmlElementInfo> transitionsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from UML opaque actions to their corresponding UML element info. */
    private Map<Action, UmlElementInfo> actionsToUmlElementInfoMap = new LinkedHashMap<>();

    /** The map from finalized UML elements to their corresponding UML element info. */
    private Map<RedefinableElement, UmlElementInfo> finalizedUmlElementsToUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF events generated for guard computation to their corresponding input UML element info. */
    private Map<Event, UmlElementInfo> guardComputationCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The map from CIF events generated for guard computation to their corresponding synthesized model UML element
     * info.
     */
    private Map<Event, UmlElementInfo> guardComputationCifEventsToFinalizedUmlElementInfo = new LinkedHashMap<>();

    /** The map from CIF events generated for language equivalence to their corresponding UML element info. */
    private Map<Event, UmlElementInfo> languageEquivalenceCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /** The set of internal CIF events (e.g. corresponding to control nodes) generated for synthesis. */
    private Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    /** The set of internal CIF events (e.g. corresponding to control nodes) generated for language equivalence. */
    private Set<Event> internalLanguageEquivalenceEvents = new LinkedHashSet<>();

    public SynthesisChainUmlElementTracking() {
        // Empty constructor.
    }
}
