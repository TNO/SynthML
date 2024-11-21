
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.DependencyOrderer;
import org.eclipse.uml2.uml.Activity;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;

/**
 * A dependency orderer for abstract activities, which determines the order in which a given collection of abstract
 * activities should be synthesized.
 */
public class AbstractActivityDependencyOrderer extends DependencyOrderer<Activity> {
    @Override
    protected Set<Activity> findDirectDependencies(Activity activity) {
        return activity.getOwnedRules().stream()
                // Filter out all occurrence constraints of the current activity.
                .filter(CifContext::isOptimalityConstraint)
                // Get all the elements that are constrained by these occurrence constraints.
                .flatMap(constraint -> constraint.getConstrainedElements().stream())
                // Filter out all abstract activities from these elements.
                .filter(element -> element instanceof Activity act && act.isAbstract())
                // Collect all these abstract activities.
                .map(Activity.class::cast).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
