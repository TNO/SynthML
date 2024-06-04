
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

import com.github.javabdd.BDD;
import com.google.common.base.Preconditions;

/** Helper for working with event guards and updates. */
public class EventGuardUpdateHelper {
    private EventGuardUpdateHelper() {
    }

    /**
     * Collect uncontrolled system guards from a CIF/BDD specification.
     *
     * @param cifBddSpec The CIF/BDD specification.
     * @return A map from CIF events to their guards as BDDs.
     */
    public static Map<Event, BDD> collectUncontrolledSystemGuards(CifBddSpec cifBddSpec) {
        Map<Event, BDD> guards = new LinkedHashMap<>();
        for (Entry<Event, List<CifBddEdge>> entry: cifBddSpec.eventEdges.entrySet()) {
            List<CifBddEdge> cifBDDEdges = entry.getValue();
            Preconditions.checkArgument(cifBDDEdges.size() == 1,
                    "Expected that each event has exactly one CIF/BDD edge.");
            BDD bdd = cifBDDEdges.get(0).guard.id();
            guards.put(entry.getKey(), bdd);
        }
        return guards;
    }
}
