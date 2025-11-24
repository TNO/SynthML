
package com.github.tno.pokayoke.transform.petrify2uml;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.common.NameHelper;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

public class ConcreteActivityRestorer {
    /** The synthesized UML activity where to add control flow guards coming from any called concrete activity. */
    private final Activity activity;

    /**
     * The tracker that indicates how results from intermediate steps of the activity synthesis chain relate to the
     * input UML.
     */
    protected final SynthesisChainTracking tracker;

    public ConcreteActivityRestorer(Activity activity, SynthesisChainTracking tracker) {
        this.activity = activity;
        this.tracker = tracker;
    }

    /** Restores the control flow guards from called concrete activities. */
    public void restoreConcreteControlFlowGuards() {
        for (ActivityNode node: activity.getNodes()) {
            // Track the original UML element related to the current node. If the original UML element is 'null', or if
            // the original UML element belongs to the activity (e.g., the current node is a call behavior and the
            // original UML element is an opaque behavior), the node is newly created. Do not add guards to these nodes.
            RedefinableElement originalSourceElement = tracker.getOriginalUmlElement(node);
            if (originalSourceElement == null || tracker.belongsToSynthesizedActivity(originalSourceElement)) {
                continue;
            }
            String entryGuard = tracker.getEntryGuard(node);
            String exitGuard = tracker.getExitGuard(node);

            // Sanity check: the node's incoming and outgoing edges must have no guards.
            Verify.verify(
                    node.getOutgoings().stream().allMatch(
                            e -> NameHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(e))),
                    String.format("Node '%s' has non-'null' incoming guards on its outgoing edges.", node.getName()));
            Verify.verify(
                    node.getIncomings().stream().allMatch(
                            e -> NameHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(e))),
                    String.format("Node '%s' has non-'null' outgoing guards on its incoming edges.", node.getName()));

            // Add the entry and exit guards to its incoming and outgoing edges, respectively.
            node.getIncomings().stream().forEach(e -> PokaYokeUmlProfileUtil.setOutgoingGuard(e, entryGuard));
            node.getOutgoings().stream().forEach(e -> PokaYokeUmlProfileUtil.setIncomingGuard(e, exitGuard));
        }
    }
}
