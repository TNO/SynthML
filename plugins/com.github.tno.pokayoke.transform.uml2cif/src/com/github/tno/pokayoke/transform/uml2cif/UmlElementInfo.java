
package com.github.tno.pokayoke.transform.uml2cif;

import org.eclipse.uml2.uml.CallBehaviorAction;
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
     * non-deterministic action, or if the UML element is atomic.
     */
    private boolean isStartAction;

    /** Represents the effect index of non-atomic or non-deterministic actions. */
    private int effectNr;

    /** {@code true} if the Petri net transition pattern has been merged during the synthesis chain. */
    private boolean isMerged;

    public UmlElementInfo(RedefinableElement umlElement) {
        if (umlElement != null) {
            this.umlElement = umlElement;
            isAtomic = PokaYokeUmlProfileUtil.isAtomic(umlElement);
            isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(umlElement);
            isShadowed = umlElement instanceof CallBehaviorAction && PokaYokeUmlProfileUtil.isFormalElement(umlElement);
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

    public int getEffectNr() {
        return effectNr;
    }

    public void setEffectNr(int n) {
        this.effectNr = n;
    }
}
