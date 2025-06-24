
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;

import com.google.common.base.Preconditions;

/** Helper for manipulating state annotations. */
public class StateAnnotationHelper {
    private StateAnnotationHelper() {
    }

    /**
     * Get state annotations from a CIF specification.
     *
     * @param spec The CIF specification.
     * @return A map from locations to annotations.
     */
    public static Map<Location, List<Annotation>> getStateAnnotations(Specification spec) {
        Map<Location, List<Annotation>> locationAnnotationMap = new LinkedHashMap<>();

        // Obtain the automaton in the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to include exactly one automaton.");
        Automaton automaton = automata.get(0);

        for (Location location: automaton.getLocations()) {
            List<Annotation> annotations = location.getAnnotations().stream()
                    .filter(annotation -> annotation.getName().equals("state")).toList();
            locationAnnotationMap.put(location, annotations);
        }

        return locationAnnotationMap;
    }
}
