
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;

/** Helper for working with event guards and updates. */
public class EventGuardUpdateHelper {
    private EventGuardUpdateHelper() {
    }

    /**
     * Collect the original uncontrolled system guards of all controllable events for the given CIF/BDD specification,
     * where original means: as specified in the original UML input model from which the CIF specification was
     * translated.
     *
     * @param cifBddSpec The CIF/BDD specification.
     * @param translator The UML to CIF translator that was used to translate the UML input model to the given CIF
     *     specification.
     * @return A map from all controllable CIF events to their guards as BDDs.
     */
    public static Map<Event, BDD> collectUncontrolledSystemGuards(CifBddSpec cifBddSpec,
            UmlToCifTranslator translator)
    {
        Map<Event, BDD> guards = new LinkedHashMap<>();

        try {
            for (Event event: cifBddSpec.eventEdges.keySet()) {
                if (event.getControllable()) {
                    guards.put(event, CifToBddConverter.convertPred(translator.getGuard(event), false, cifBddSpec));
                }
            }
        } catch (UnsupportedPredicateException ex) {
            throw new RuntimeException("Failed to translate a guard predicate to a BDD: " + ex.getMessage());
        }

        return guards;
    }
}
