
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.javabdd.BDD;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;

/** Helper for working with event guards and updates. */
public class EventGuardUpdateHelper {
    private EventGuardUpdateHelper() {
    }

    /**
     * Collect the uncontrolled system guards of all events for the given CIF/BDD specification. The uncontrolled system
     * guard for every controllable event is the guard that is specified in the original UML input model from which the
     * CIF specification was translated. The uncontrolled system guard for every uncontrollable event is simply 'true'.
     *
     * @param cifBddSpec The CIF/BDD specification.
     * @param translator The UML to CIF translator that was used to translate the UML input model to the given CIF
     *     specification.
     * @return A map from the names of all CIF events to their uncontrollable system guards as BDDs.
     */
    public static Map<String, BDD> collectUncontrolledSystemGuards(CifBddSpec cifBddSpec,
            UmlToCifTranslator translator)
    {
        Map<String, BDD> guards = new LinkedHashMap<>();

        try {
            for (Event event: cifBddSpec.eventEdges.keySet()) {
                BDD guard = null;

                if (event.getControllable()) {
                    guard = CifToBddConverter.convertPred(translator.getGuard(event), false, cifBddSpec);
                } else {
                    guard = cifBddSpec.factory.one();
                }

                guards.put(event.getName(), guard);
            }
        } catch (UnsupportedPredicateException ex) {
            throw new RuntimeException("Failed to translate a guard predicate to a BDD: " + ex.getMessage(), ex);
        }

        return guards;
    }

    /**
     * Collect all controlled system guards as a mapping from names of controllable events, to their synthesized guards.
     *
     * @param synthesisResult The data-based synthesis results.
     * @return The controlled system guards mapping.
     */
    public static Map<String, BDD> collectControlledSystemGuards(CifDataSynthesisResult synthesisResult) {
        Map<String, BDD> guards = new LinkedHashMap<>();

        for (Entry<Event, BDD> entry: synthesisResult.outputGuards.entrySet()) {
            guards.put(entry.getKey().getName(), entry.getValue());
        }

        return guards;
    }
}
