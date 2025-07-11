
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
    private RedefinableElement umlElement;

    private boolean isAtomic;

    private boolean isDeterministic;

    private boolean isStartAction;

    private boolean isMerged;

    private boolean isShadowed;

    private int effectNr;

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
