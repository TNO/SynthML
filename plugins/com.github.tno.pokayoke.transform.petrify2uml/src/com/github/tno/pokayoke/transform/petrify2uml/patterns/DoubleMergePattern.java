
package com.github.tno.pokayoke.transform.petrify2uml.patterns;

import java.util.List;
import java.util.Optional;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.MergeNode;

import com.github.tno.pokayoke.transform.track.SynthesisUmlElementTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;

/**
 * Functionality for finding and replacing <i>double merge</i> patterns in UML activities.
 * <p>
 * A <i>double merge</i> pattern is a guard-free control flow in the UML activity that connects two merge nodes. The two
 * merge node must not represent a concrete activity final node. Such a control flow is redundant since all the merging
 * that's done on the source merge node can also be done on the target merge node. Every such control flow can thus be
 * rewritten by removing the source merge node, and redirecting all merging control flows to merge into the target merge
 * node instead.
 * </p>
 */
public class DoubleMergePattern {
    private final ActivityEdge controlFlow;

    private DoubleMergePattern(ActivityEdge controlFlow) {
        this.controlFlow = controlFlow;
    }

    /**
     * Finds and rewrites all <i>double merge</i> patterns in the given activity.
     *
     * @param activity The input activity, which is modified in-place.
     * @param tracker The synthesis tracker.
     * @return {@code true} if the input activity has been rewritten, {@code false} otherwise.
     */
    public static boolean findAndRewriteAll(Activity activity, SynthesisUmlElementTracking tracker) {
        boolean hasFoundPatterns = false;

        // Only rewrite one pattern at a time, to prevent issues when patterns overlap.
        while (true) {
            Optional<DoubleMergePattern> patterns = findAny(activity, tracker);
            if (patterns.isEmpty()) {
                break;
            } else {
                patterns.get().rewrite();
                hasFoundPatterns = true;
            }
        }
        return hasFoundPatterns;
    }

    /**
     * Finds a <i>double merge</i> pattern in the given activity, if present.
     *
     * @param activity The input activity.
     * @param tracker The synthesis tracker.
     * @return A <i>double merge</i> pattern in the given activity, if present.
     */
    public static Optional<DoubleMergePattern> findAny(Activity activity, SynthesisUmlElementTracking tracker) {
        return activity.getEdges().stream().flatMap(edge -> findAny(edge, tracker).stream()).findFirst();
    }

    /**
     * Tries finding a <i>double merge</i> pattern that involves the given control flow.
     *
     * @param controlFlow The input control flow.
     * @param tracker The synthesis tracker.
     * @return Some <i>double merge</i> pattern in case one was found, or an empty result otherwise.
     */
    private static Optional<DoubleMergePattern> findAny(ActivityEdge controlFlow, SynthesisUmlElementTracking tracker) {
        if (controlFlow.getSource() instanceof MergeNode sourceMergeNode
                && controlFlow.getTarget() instanceof MergeNode targetMergeNode
                && !PokaYokeUmlProfileUtil.isGuardedControlFlow((ControlFlow)controlFlow)
                && !(tracker.representsActivityFinalNode(sourceMergeNode))
                && !(tracker.representsActivityFinalNode(targetMergeNode)))
        {
            return Optional.of(new DoubleMergePattern(controlFlow));
        } else {
            return Optional.empty();
        }
    }

    /** Rewrites this <i>double merge</i> pattern. */
    public void rewrite() {
        ActivityNode source = controlFlow.getSource();
        ActivityNode target = controlFlow.getTarget();

        // Remove the control flow in this pattern.
        controlFlow.destroy();

        // Redirect all incoming control flows into the source node to go to the target node instead.
        for (ActivityEdge controlFlow: List.copyOf(source.getIncomings())) {
            controlFlow.setTarget(target);
        }

        // Remove the source node, which is now replaced by the target node.
        source.destroy();
    }
}
