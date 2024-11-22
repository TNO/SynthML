
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.DependencyOrderer;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.google.common.collect.ImmutableSet;

/**
 * A dependency orderer for abstract activities, which determines the order in which a given collection of abstract
 * activities should be synthesized.
 */
public class AbstractActivityDependencyOrderer {
    /** The input abstract activities. */
    private final Set<Activity> abstractActivities;

    /** The dependency orderer. */
    private final ActivityOrderer orderer;

    /**
     * Constructs a new dependency orderer for the given collection of abstract activities.
     *
     * @param abstractActivities The input abstract activities.
     */
    public AbstractActivityDependencyOrderer(Collection<Activity> abstractActivities) {
        this.abstractActivities = new LinkedHashSet<>(abstractActivities);
        this.orderer = new ActivityOrderer();

        abstractActivities.forEach(orderer::addObject);
    }

    /**
     * Computes the order in which the abstract activities must be synthesized.
     *
     * @return The list of activities in the order they should be synthesized, or {@code null} if a cycle in the
     *     dependencies is detected.
     */
    public List<Activity> computeOrder() {
        return orderer.computeOrder();
    }

    /**
     * Gives a description of a cycle in case there are cyclic dependencies in the activities to synthesize.
     *
     * @return The cycle description.
     */
    public String getCycleDescription() {
        return orderer.getCycle().stream().map(Activity::getName).collect(Collectors.joining(" -> "));
    }

    private class ActivityOrderer extends DependencyOrderer<Activity> {
        @Override
        protected Set<Activity> findDirectDependencies(Activity activity) {
            // Find all activities that cannot be called by the current activity due to its occurrence constraints.
            Set<Activity> blockedActivities = activity.getOwnedRules().stream()
                    // Get all blocking occurrence constraints, which express that certain elements can't happen.
                    .filter(this::isBlockingOccurrenceConstraint)
                    // Get all the elements that are constrained by these blocking occurrence constraints.
                    .flatMap(constraint -> constraint.getConstrainedElements().stream())
                    // Get all activities from these blocked elements.
                    .filter(Activity.class::isInstance).map(Activity.class::cast)
                    // Collect all these activities.
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // The activity depends on all abstract activities except itself and those that are blocked explicitly.
            return Sets.difference(abstractActivities, ImmutableSet.of(activity), blockedActivities);
        }

        /**
         * Checks whether the given UML constraint is an occurrence constraint expressing that the constrained elements
         * cannot be performed at all, i.e., can happen at most zero times.
         *
         * @param constraint The UML constraint to check.
         * @return {@code true} if the given UML constraint is a blocking occurrence constraint, or {@code false}
         *     otherwise.
         */
        private boolean isBlockingOccurrenceConstraint(Constraint constraint) {
            if (CifContext.isOptimalityConstraint(constraint)) {
                IntervalConstraint intervalConstraint = (IntervalConstraint)constraint;
                Interval interval = (Interval)intervalConstraint.getSpecification();
                return interval.getMax().integerValue() <= 0;
            }

            return false;
        }
    }
}
