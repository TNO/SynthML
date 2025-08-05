
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
     * The map from CIF events generated for the initial data-based synthesis to a pair composed of their corresponding
     * UML elements of the input model, and the effect index, if relevant. The effect index is either a positive integer
     * when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of a non-atomic
     * action). Gets updated as the activity synthesis chain rewrites, removes, or add events.
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> synthesisCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of CIF events representing the start of a UML action/behavior generated for the initial data-based
     * synthesis.
     */
    private Set<Event> synthesisCifStartEvents = new LinkedHashSet<>();

    /**
     * The map from CIF events generated for the guard computation step to a pair composed of their corresponding UML
     * elements of the input model, and the effect index, if relevant. The effect index is either a positive integer
     * when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of a non-atomic
     * action).
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> guardComputationCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of CIF events representing the start of a UML action/behavior generated for the guard computation step.
     */
    private Set<Event> guardComputationCifStartEvents = new LinkedHashSet<>();

    /**
     * The map from CIF events generated for the language equivalence check step to a pair composed of their
     * corresponding UML elements of the input model, and the effect index, if relevant. The effect index is either a
     * positive integer when relevant, or {@code null} when irrelevant (e.g., in case the CIF event is a start event of
     * a non-atomic action).
     */
    private final Map<Event, Pair<RedefinableElement, Integer>> languageCifEventsToUmlElementInfo = new LinkedHashMap<>();

    /**
     * The set of CIF events representing the start of a UML action/behavior generated for the language equivalence
     * check step.
     */
    private Set<Event> languageCifStartEvents = new LinkedHashSet<>();

    /**
     * The set of internal CIF events (e.g. corresponding to control nodes) generated for the initial data-based
     * synthesis.
     */
    private final Set<Event> internalSynthesisEvents = new LinkedHashSet<>();

    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

    /**
     * Add a single CIF event. The effect index is either a positive integer when relevant, or {@code null} when
     * irrelevant (e.g., in case the CIF event is a start event of a non-atomic action).
     *
     * @param cifEvent The CIF event to relate to the UML element.
     * @param umlElement The UML element to be related to the CIF event.
     * @param effectIdx The effect index. Can be {@code null} if the CIF event is a start event.
     * @param purpose The translation purpose.
     */
    public void addCifEvent(Event cifEvent, RedefinableElement umlElement, Integer effectIdx,
            TranslationPurpose purpose)
    {
        switch (purpose) {
            case SYNTHESIS: {
                synthesisCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, effectIdx));
                break;
            }
            case GUARD_COMPUTATION: {
                guardComputationCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, effectIdx));
                break;
            }
            case LANGUAGE_EQUIVALENCE: {
                languageCifEventsToUmlElementInfo.put(cifEvent, new Pair<>(umlElement, effectIdx));
                break;
            }

            default:
                throw new RuntimeException("Unsupported translation purpose: " + purpose + ".");
        }
    }

    /**
     * Return the map from CIF start events to the corresponding UML elements.
     *
     * @param purpose The translation purpose.
     * @return The map from CIF start events to their corresponding UML elements.
     */
    public Map<Event, RedefinableElement> getStartEventMap(TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.entrySet().stream().filter(e -> e.getValue().right == null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().left));
            }
            case GUARD_COMPUTATION: {
                return guardComputationCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(e -> e.getValue().right == null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().left));
            }
            case LANGUAGE_EQUIVALENCE: {
                return languageCifEventsToUmlElementInfo.entrySet().stream().filter(e -> e.getValue().right == null)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().left));
            }
            default:
                throw new RuntimeException("Unsupported translation purpose: " + purpose + ".");
        }
    }

    /**
     * Return whether the CIF event represents a start action.
     *
     * @param cifEvent The CIF event.
     * @param purpose The enumeration informing on which translation is occurring.
     * @return {@code true} if the CIF event corresponds to a start event.
     */
    public boolean isStartEvent(Event cifEvent, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                // Compute the synthesis start event set.
                synthesisCifStartEvents = synthesisCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(e -> e.getValue().right == null).map(e -> e.getKey()).collect(Collectors.toSet());
                return synthesisCifStartEvents.contains(cifEvent);
            }

            case GUARD_COMPUTATION: {
                // Compute the guard computation start event set.
                guardComputationCifStartEvents = guardComputationCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(e -> e.getValue().right == null).map(e -> e.getKey()).collect(Collectors.toSet());
                return guardComputationCifStartEvents.contains(cifEvent);
            }

            case LANGUAGE_EQUIVALENCE: {
                // Compute the language equivalence check start event set.
                languageCifStartEvents = languageCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(e -> e.getValue().right == null).map(e -> e.getKey()).collect(Collectors.toSet());
                return languageCifStartEvents.contains(cifEvent);
            }
            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    /**
     * Return the events corresponding to the input set of nodes, based on the translation purpose.
     *
     * @param nodes The set of activity node, to find the related CIF events.
     * @param purpose The enumeration informing on which translation is occurring.
     * @return The list of CIF events corresponding to the activity nodes.
     */
    public List<Event> getNodeEvents(Set<ActivityNode> nodes, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().left)).map(Map.Entry::getKey).toList();
            }
            case GUARD_COMPUTATION: {
                return guardComputationCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().left)).map(Map.Entry::getKey).toList();
            }
            case LANGUAGE_EQUIVALENCE: {
                return languageCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> nodes.contains(entry.getValue().left)).map(Map.Entry::getKey).toList();
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }

    /**
     * Get the list of CIF start events corresponding to the UML element for different translation purposes. Not yet
     * supported for guard computation and language equivalence check.
     *
     * @param umlElement The UML element.
     * @param purpose The enumeration informing on which translation is occurring.
     * @return The list from CIF start events to their corresponding UML element infos.
     */
    public List<Event> getStartEventsOfUmlElement(RedefinableElement umlElement, TranslationPurpose purpose) {
        switch (purpose) {
            case SYNTHESIS: {
                return synthesisCifEventsToUmlElementInfo.entrySet().stream()
                        .filter(entry -> isStartEvent(entry.getKey(), TranslationPurpose.SYNTHESIS)
                                && entry.getValue().left.equals(umlElement))
                        .map(Map.Entry::getKey).toList();
            }

            default:
                throw new RuntimeException("Invalid translation purpose: " + purpose + ".");
        }
    }
}
