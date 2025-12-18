
package com.github.tno.pokayoke.transform.track;

/**
 * The enumeration that describes the purpose of a UML-to-CIF translation. It is used in the UML-to-CIF translator to
 * decide whether or not to translate certain model elements, or how to translate them. It is used by
 * {@link SynthesisChainTracking the activity synthesis tracker} to store the CIF events for the different UML-to-CIF
 * translations.
 */
public enum UmlToCifTranslationPurpose {
    SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE, INTERFACE;
}
