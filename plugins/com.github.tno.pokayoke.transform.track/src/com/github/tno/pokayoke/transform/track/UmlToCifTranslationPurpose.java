////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.track;

/**
 * The enumeration that describes the purpose of a UML-to-CIF translation. It is used in the UML-to-CIF translator to
 * decide whether or not to translate certain model elements, or how to translate them. It is used by
 * {@link SynthesisChainTracking the activity synthesis tracker} to store the CIF events for the different UML-to-CIF
 * translations.
 */
public enum UmlToCifTranslationPurpose {
    SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
}
