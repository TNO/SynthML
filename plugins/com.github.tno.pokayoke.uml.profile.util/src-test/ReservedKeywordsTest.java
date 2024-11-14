import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.escet.cif.parser.CifScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tno.pokayoke.uml.profile.validation.PokaYokeProfileValidator;

class ReservedKeywordsTest {
    @Test
    @DisplayName("Test Reserved Keywords for CIF, GAL, and Petrify.")
    void testReservedKeywords() {
        // get all keywords from CIF
        String[] cifKeywords = CifScanner.getKeywords("Keywords");
        // get all keywords from GAL
        String[] galKeywords = new String[] {"A", "AF", "AG", "AX", "E", "EF", "EG", "EX", "F", "G", "GAL", "M", "R",
                "TRANSIENT", "U", "W", "X", "abort", "alias", "array", "atom", "bounds", "composite", "ctl", "else",
                "extends", "false", "fixpoint", "for", "gal", "hotbit", "if", "import", "int", "interface", "invariant",
                "label", "ltl", "main", "never", "predicate", "property", "reachable", "self", "synchronization",
                "transition", "true", "typedef"};
        // check CIF keywords
        for (String word: cifKeywords) {
            assertEquals(true, PokaYokeProfileValidator.isReservedKeyword(word), "CIF Keywords should be detected");
        }
        // check GAL keywords
        for (String word: galKeywords) {
            assertEquals(true, PokaYokeProfileValidator.isReservedKeyword(word), "GAL Keywords should be detected");
        }
        // check Petrify notation
        assertEquals(true, PokaYokeProfileValidator.isReservedKeyword(".name"), "Petrify Keywords should be detected");
        // check random word --> should return FALSE
        assertEquals(false, PokaYokeProfileValidator.isReservedKeyword("random_name"),
                "Neither CIF, GAL, Petrify keyword.");
    }
}
