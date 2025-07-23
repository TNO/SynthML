
package com.github.tno.pokayoke.transform.track;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.RedefinableElement;

import fr.lip6.move.pnml.ptnet.Transition;

/**
 * Tracks the synthesis chain transformations from the UML elements of the input model, to their translation to CIF
 * events, the translation to Petri net transitions, and finally to the synthesized activity UML opaque actions (before
 * model finalization).
 */
public class SynthesisChainUmlElementTracking {
    /** The suffix of an atomic action outcome. */
    public static final String ATOMIC_OUTCOME_SUFFIX = "__result_";

    /** The suffix of a non-atomic action outcome. */
    public static final String NONATOMIC_OUTCOME_SUFFIX = "__na_result_";

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

    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

    public SynthesisChainUmlElementTracking() {
        // Empty constructor.
    }

    // Section for methods handling CIF events and UML elements.

    /**
     * Add a single CIF event. It is implied that it represents a start event, as there is no effect index in the input
     * arguments.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event.
     * @param purpose The enumeration informing on which translation is occurring.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, TranslationPurpose purpose) {
        // Create the UML element info and store it.
        UmlElementInfo umlElementInfo = new UmlElementInfo(umlElement);
        umlElementInfo.setStartAction(true);
        umlElementInfo.setMerged(false);

        switch (purpose) {
            case TranslationPurpose.SYNTHESIS: {
                synthesisCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
                namesToCifEvents.put(cifEvent.getName(), cifEvent);
                cifEventsToInitialUmlElementInfo.put(cifEvent, umlElementInfo.copy());

                break;
            }

            case TranslationPurpose.GUARD_COMPUTATION: {
                // In guard computation, the input UML element represents the finalized UML element, and the CIF event
                // is the event stemming from it. We need to link this CIF event not only to the finalized event, but
                // also to the original UML element of the input model. The CIF event to finalized UML element defines a
                // start, non-merged action (similarly to the synthesis case). The CIF event to the original UML element
                // inherits the action attributes from the previous step in the synthesis chain, i.e. the finalized UML
                // element info.

                // Store the CIF event in relation to the finalized UML element info.
                guardComputationCifEventsToFinalizedUmlElementInfo.put(cifEvent, umlElementInfo);

                // Store the CIF event in relation to the original UML element. If the current CIF element corresponds
                // to a control node (e.g. decision node), there is no original UML element to refer to, hence create an
                // empty UML element info.
                UmlElementInfo originalUmlElementInfo = (finalizedUmlElementsToUmlElementInfo.get(umlElement) == null)
                        ? new UmlElementInfo(null) : finalizedUmlElementsToUmlElementInfo.get(umlElement).copy();
                guardComputationCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);

                break;
            }

            case TranslationPurpose.LANGUAGE_EQUIVALENCE: {
                // Store the CIF event in relation to the original UML element. If the current CIF element corresponds
                // to a control node (e.g. decision node), there is no original UML element to refer to, hence create an
                // empty UML element info.
                UmlElementInfo originalUmlElementInfo = (finalizedUmlElementsToUmlElementInfo.get(umlElement) == null)
                        ? new UmlElementInfo(null) : finalizedUmlElementsToUmlElementInfo.get(umlElement).copy();
                languageEquivalenceCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);

                break;
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    /**
     * Add a single CIF event. It is implied that it represents an end event, as there is the effect index in the inpu
     * arguments.
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElementAndEffectIdx The pair containing the UML element to be related to the CIF event, and the effect
     *     index.
     * @param purpose The enumeration informing on which translation is occurring.
     */
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

        switch (purpose) {
            case TranslationPurpose.SYNTHESIS: {
                synthesisCifEventsToUmlElementInfo.put(cifEvent, umlElementInfo);
                namesToCifEvents.put(cifEvent.getName(), cifEvent);
                cifEventsToInitialUmlElementInfo.put(cifEvent, umlElementInfo.copy());
                break;
            }

            case TranslationPurpose.GUARD_COMPUTATION: {
                // In guard computation, the input UML element represents the finalized UML element, and the CIF event
                // is the event stemming from it. We need to link this CIF event not only to the finalized event, but
                // also to the original UML element of the input model. The CIF event to finalized UML element defines
                // an end, non-merged action (similarly to the synthesis case). Also the CIF event to the original UML
                // element defines an end, non-merged action.

                // Store the CIF event in relation to the finalized UML element info.
                guardComputationCifEventsToFinalizedUmlElementInfo.put(cifEvent, umlElementInfo);

                // Store the CIF event in relation to the original UML element. If the current CIF element corresponds
                // to a control node (e.g. decision node), there is no original UML element to refer to, hence create an
                // empty UML element info.
                UmlElementInfo originalUmlElementInfo = (finalizedUmlElementsToUmlElementInfo.get(umlElement) == null)
                        ? new UmlElementInfo(null) : finalizedUmlElementsToUmlElementInfo.get(umlElement).copy();
                originalUmlElementInfo.setStartAction(false);
                originalUmlElementInfo.setMerged(false);
                originalUmlElementInfo.setEffectIdx(effectNr);

                guardComputationCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);

                break;
            }

            case TranslationPurpose.LANGUAGE_EQUIVALENCE: {
                // Store the CIF event in relation to the original UML element. If the current CIF element corresponds
                // to a control node (e.g. decision node), there is no original UML element to refer to, hence create an
                // empty UML element info.
                UmlElementInfo originalUmlElementInfo = (finalizedUmlElementsToUmlElementInfo.get(umlElement) == null)
                        ? new UmlElementInfo(null) : finalizedUmlElementsToUmlElementInfo.get(umlElement).copy();
                originalUmlElementInfo.setStartAction(false);
                originalUmlElementInfo.setMerged(false);
                originalUmlElementInfo.setEffectIdx(effectNr);

                languageEquivalenceCifEventsToUmlElementInfo.put(cifEvent, originalUmlElementInfo);

                break;
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }
}
