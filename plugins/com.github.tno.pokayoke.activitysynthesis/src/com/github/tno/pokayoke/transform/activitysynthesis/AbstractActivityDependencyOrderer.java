
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.DependencyOrderer;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;

/**
 * A dependency orderer for abstract activities, which determines the order in which a given collection of abstract
 * activities should be synthesized.
 */
public class AbstractActivityDependencyOrderer extends DependencyOrderer<Activity> {
    @Override
    protected Set<Activity> findDirectDependencies(Activity activity) {
        return activity.getOwnedRules().stream()
                // Get all occurrence constraints of the current activity.
                .filter(this::isPositiveOccurrenceConstraint)
                // Get all the elements that are constrained by these occurrence constraints.
                .flatMap(constraint -> constraint.getConstrainedElements().stream())
                // Get all abstract activities from these elements.
                .filter(element -> element instanceof Activity act && act.isAbstract())
                // Collect all these abstract activities.
                .map(Activity.class::cast).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Checks whether the given UML constraint is an occurrence constraint that allows the constrained elements to
     * happen at least once.
     *
     * @param constraint The UML constraint to check.
     * @return {@code true} if the given UML constraint is an occurrence constraint that allows the constrained elements
     *     to happen at least once, or {@code false} otherwise.
     */
    private boolean isPositiveOccurrenceConstraint(Constraint constraint) {
        if (CifContext.isOptimalityConstraint(constraint)) {
            IntervalConstraint intervalConstraint = (IntervalConstraint)constraint;
            Interval interval = (Interval)intervalConstraint.getSpecification();
            return interval.getMax().integerValue() > 0;
        }

        return false;
    }
}
