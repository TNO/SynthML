
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;

import com.google.common.base.Preconditions;

/** Auxiliary functionality for conveniently handling/querying {@link Location CIF locations} for activity synthesis. */
public class CifLocationHelper {
    private CifLocationHelper() {
        // Static class.
    }

    /**
     * Gives the single (trivially) initial location in the given automaton, assuming it has exactly one such location.
     *
     * @param automaton The input automaton.
     * @return The single (trivially) initial location in the given automaton.
     */
    public static Location getInitialLocation(Automaton automaton) {
        Set<Location> locations = getInitialLocations(automaton);
        Preconditions.checkArgument(locations.size() == 1,
                "Expected exactly one initial location, but got " + locations.size() + ".");
        return locations.iterator().next();
    }

    /**
     * Gives the set of all (trivially) initial locations in the given automaton.
     *
     * @param automaton The input automaton.
     * @return The set of all (trivially) initial locations in the given automaton.
     */
    public static Set<Location> getInitialLocations(Automaton automaton) {
        return automaton.getLocations().stream().filter(CifLocationHelper::isInitial)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gives the single (trivially) marked location in the given automaton, assuming it has exactly one such location.
     *
     * @param automaton The input automaton.
     * @return The single (trivially) marked location in the given automaton.
     */
    public static Location getMarkedLocation(Automaton automaton) {
        Set<Location> locations = getMarkedLocations(automaton);
        Preconditions.checkArgument(locations.size() == 1,
                "Expected exactly one marked location, but got " + locations.size() + ".");
        return locations.iterator().next();
    }

    /**
     * Gives the set of all (trivially) marked locations in the given automaton.
     *
     * @param automaton The input automaton.
     * @return The set of all (trivially) marked locations in the given automaton.
     */
    public static Set<Location> getMarkedLocations(Automaton automaton) {
        return automaton.getLocations().stream().filter(CifLocationHelper::isMarked)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Checks whether the given location is (trivially) initial.
     *
     * @param location The location to check.
     * @return {@code true} if the given location is (trivially) initial, {@code false} otherwise.
     */
    public static boolean isInitial(Location location) {
        boolean isTriviallyInitial = location.getInitials().isEmpty() ? false
                : CifValueUtils.isTriviallyTrue(location.getInitials(), true, true);
        boolean isTriviallyNotInitial = location.getInitials().isEmpty() ? true
                : CifValueUtils.isTriviallyFalse(location.getInitials(), true, true);

        Preconditions.checkArgument(isTriviallyInitial || isTriviallyNotInitial,
                "Expected that locations are either trivially initial or trivially not initial.");

        return isTriviallyInitial;
    }

    /**
     * Checks whether the given location is (trivially) marked.
     *
     * @param location The location to check.
     * @return {@code true} if the given location is (trivially) marked, {@code false} otherwise.
     */
    public static boolean isMarked(Location location) {
        boolean isTriviallyMarked = location.getMarkeds().isEmpty() ? false
                : CifValueUtils.isTriviallyTrue(location.getMarkeds(), false, true);
        boolean isTriviallyNotMarked = location.getMarkeds().isEmpty() ? true
                : CifValueUtils.isTriviallyFalse(location.getMarkeds(), false, true);

        Preconditions.checkArgument(isTriviallyMarked || isTriviallyNotMarked,
                "Expected that locations are either trivially marked or trivially not marked.");

        return isTriviallyMarked;
    }
}
