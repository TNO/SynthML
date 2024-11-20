
package com.github.tno.pokayoke.uml.profile.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.escet.cif.parser.CifScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tno.pokayoke.transform.common.NameHelper;

class ReservedKeywordsTest {
    @Test
    @DisplayName("Test Reserved Keywords for CIF, GAL, and Petrify.")
    void testReservedKeywords() {
        // Check all CIF keywords.
        for (String word: CifScanner.getKeywords("Keywords")) {
            assertTrue(NameHelper.isReservedKeyword(word), "CIF Keywords should be detected");
        }
        for (String word: CifScanner.getKeywords("Operator")) {
            assertTrue(NameHelper.isReservedKeyword(word), "CIF Keywords should be detected");
        }
        for (String word: CifScanner.getKeywords("StdLibFunction")) {
            assertTrue(NameHelper.isReservedKeyword(word), "CIF Keywords should be detected");
        }
        for (String word: CifScanner.getKeywords("SupKind")) {
            assertTrue(NameHelper.isReservedKeyword(word), "CIF Keywords should be detected");
        }

        // Get all keywords from GAL.
        String[] galKeywords = new String[] {"A", "AF", "AG", "AX", "E", "EF", "EG", "EX", "F", "G", "GAL", "M", "R",
                "TRANSIENT", "U", "W", "X", "abort", "alias", "array", "atom", "bounds", "composite", "ctl", "else",
                "extends", "false", "fixpoint", "for", "gal", "hotbit", "if", "import", "int", "interface", "invariant",
                "label", "ltl", "main", "never", "predicate", "property", "reachable", "self", "synchronization",
                "transition", "true", "typedef"};

        // Check GAL keywords.
        for (String word: galKeywords) {
            assertTrue(NameHelper.isReservedKeyword(word), "GAL Keywords should be detected");
        }

        // Check Petrify notation.
        assertTrue(NameHelper.isReservedKeyword(".name"), "Petrify Keywords should be detected");

        // Check on a non-reserved keyword.
        assertFalse(NameHelper.isReservedKeyword("random_name"), "Neither CIF, GAL, Petrify keyword.");
    }
}
