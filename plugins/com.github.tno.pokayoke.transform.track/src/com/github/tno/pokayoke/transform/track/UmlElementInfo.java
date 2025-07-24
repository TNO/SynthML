
package com.github.tno.pokayoke.transform.track;

import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;

/**
 * Container class to store UML elements and how they get used throughout the synthesis chain. The class fields refer to
 * the actual UML element (isAtomic, isDeterministic, isShadowed), to the associated CIF events (isStartAction if it is
 * the start of a UML atomic or non-atomic action/behavior, effectNr represents the effect number if the CIF event
 * represents the end of a UML non-atomic or non-deterministic action) and whether the related transition pattern in the
 * Petri net has been merged or not (isMerged).
 */
public class UmlElementInfo {
    /** The UML element of the input model. */
    private RedefinableElement umlElement;

    /** {@code true} if the input UML element is atomic. */
    private boolean isAtomic;

    /** {@code true} if the input UML element is deterministic. */
    private boolean isDeterministic;

    /** {@code true} if the input UML element is a shadowed call behavior. */
    private boolean isShadowed;

    /**
     * {@code true} if the CIF event associated to the UML element is the start event of a non-atomic or
     * non-deterministic action, or if the UML element is atomic. By default, all control nodes are translated as a
     * start event, and thus are start actions. It can be {@code false} only for the events/action related to the end of
     * non-merged non-atomic or non-deterministic UML elements.
     */
    private boolean isStartAction;

    /** Represents the effect index of non-atomic or non-deterministic actions. */
    private int effectIdx;

    /**
     * {@code true} if the atomic non-deterministic event pattern, or if the Petri net non-atomic transition pattern has
     * been merged during the synthesis chain.
     */
    private boolean isMerged;

    public UmlElementInfo(RedefinableElement umlElement) {
        if (umlElement != null) {
            this.umlElement = umlElement;

            if (umlElement instanceof CallBehaviorAction cbAction) {
                // If shadowed call behavior, use the call behavior element properties. Otherwise, use the called
                // element properties.
                if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
                    isAtomic = PokaYokeUmlProfileUtil.isAtomic(umlElement);
                    isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(umlElement);
                    isShadowed = true;
                } else {
                    isAtomic = PokaYokeUmlProfileUtil.isAtomic(cbAction.getBehavior());
                    isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(cbAction.getBehavior());
                    isShadowed = false;
                }
            } else {
                isAtomic = PokaYokeUmlProfileUtil.isAtomic(umlElement);
                isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(umlElement);
                isShadowed = false;
            }
        }
    }

    public RedefinableElement getUmlElement() {
        return umlElement;
    }

    public boolean isAtomic() {
        return isAtomic;
    }

    public boolean isDeterministic() {
        return isDeterministic;
    }

    public boolean isStartAction() {
        return isStartAction;
    }

    public void setStartAction(boolean isStartAction) {
        this.isStartAction = isStartAction;
    }

    public boolean isMerged() {
        return isMerged;
    }

    public void setMerged(boolean isMerged) {
        this.isMerged = isMerged;
    }

    public boolean isShadowed() {
        return isShadowed;
    }

    public int getEffectIdx() {
        return effectIdx;
    }

    public void setEffectIdx(int n) {
        this.effectIdx = n;
    }

    /**
     * Return {@code true} if the UML element info is equivalent to the input UML element info. Equivalence is based on
     * all the fields, excluding 'isMerged' which represents structural information about how the CIF event (or Petri
     * net transition, or UML finalized action) have been used in the synthesis chain so far.
     *
     * @param thatUmlElementInfo The UML element info to compare.
     * @return {@code true} if the input UML element info is equivalent to the current one.
     */
    public boolean isEquivalentWithoutStructure(UmlElementInfo thatUmlElementInfo) {
        // Check if all fields are equal, excluding field 'isMerged' which is a structural information, and does not
        // regard the UML element per se. It is used only for finalizing the action.
        if (isAtomic != thatUmlElementInfo.isAtomic() || isDeterministic != thatUmlElementInfo.isDeterministic()
                || isShadowed != thatUmlElementInfo.isShadowed() || isStartAction != thatUmlElementInfo.isStartAction()
                || effectIdx != thatUmlElementInfo.getEffectIdx())
        {
            return false;
        }

        // If this or the other UML element info is a non-shadowed call behavior, compare the called behaviors.
        RedefinableElement thisUmlCalledElement;
        RedefinableElement thatUmlCalledElement;
        if (umlElement instanceof CallBehaviorAction cbAction && !PokaYokeUmlProfileUtil.isFormalElement(cbAction)) {
            thisUmlCalledElement = cbAction.getBehavior();
        } else {
            thisUmlCalledElement = this.umlElement;
        }

        if (thatUmlElementInfo.getUmlElement() instanceof CallBehaviorAction cbAction
                && !PokaYokeUmlProfileUtil.isFormalElement(cbAction))
        {
            thatUmlCalledElement = cbAction.getBehavior();
        } else {
            thatUmlCalledElement = thatUmlElementInfo.getUmlElement();
        }

        // If UML element is a non-shadowed call behavior, compare the opaque behaviors; else, compare the UML elements
        // themselves.
        if (thisUmlCalledElement instanceof CallBehaviorAction thisCallBehaviorAction
                && PokaYokeUmlProfileUtil.isFormalElement(thisCallBehaviorAction)
                && thatUmlCalledElement instanceof CallBehaviorAction thatCallBehaviorAction
                && PokaYokeUmlProfileUtil.isFormalElement(thatCallBehaviorAction))
        {
            return thisCallBehaviorAction.equals(thatCallBehaviorAction);
        } else if (thisUmlCalledElement instanceof CallBehaviorAction thisCallBehaviorAction
                && thatUmlCalledElement instanceof CallBehaviorAction thatCallBehaviorAction)
        {
            return thisCallBehaviorAction.getBehavior().equals(thatCallBehaviorAction.getBehavior());
        } else if (thisUmlCalledElement instanceof OpaqueAction thisOpaqueAction
                && thatUmlCalledElement instanceof OpaqueAction thatOpaqueAction)
        {
            return thisOpaqueAction.equals(thatOpaqueAction);
        } else if (thisUmlCalledElement instanceof OpaqueBehavior thisOpaqueBehavior
                && thatUmlCalledElement instanceof OpaqueBehavior thatOpaqueBehavior)
        {
            return thisOpaqueBehavior.equals(thatOpaqueBehavior);
        }

        return false;
    }

    /**
     * Return {@code true} if the current UML element info represents an internal action: if the underlying UML element
     * is null or a control node.
     *
     * @return {@code true} if the current UML element info represents an internal action.
     */
    public boolean isInternal() {
        return umlElement == null || umlElement instanceof DecisionNode || umlElement instanceof MergeNode
                || umlElement instanceof ForkNode || umlElement instanceof JoinNode || umlElement instanceof InitialNode
                || umlElement instanceof ActivityFinalNode;
    }

    /**
     * Return a copy of the current UML element info.
     *
     * @return A copy of the current UML element info.
     */
    public UmlElementInfo copy() {
        UmlElementInfo copiedUmlElementInfo = new UmlElementInfo(umlElement);
        copiedUmlElementInfo.setMerged(isMerged);
        copiedUmlElementInfo.setStartAction(isStartAction);
        copiedUmlElementInfo.setEffectIdx(effectIdx);
        return copiedUmlElementInfo;
    }
}
