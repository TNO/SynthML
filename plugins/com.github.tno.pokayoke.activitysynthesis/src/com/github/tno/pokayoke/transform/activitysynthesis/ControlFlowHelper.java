
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.Map;

import org.eclipse.uml2.uml.ControlFlow;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

/** Helper for adding guards to UML control flows. */
public class ControlFlowHelper {
    private ControlFlowHelper() {
    }

    /**
     * Add guards to UML control flows as indicated by the specified mapping.
     *
     * @param controlFlowToGuard The map from UML control flows to their guards as textual CIF expressions.
     */
    public static void addGuards(Map<ControlFlow, String> controlFlowToGuard) {
        controlFlowToGuard.forEach(ControlFlowHelper::addGuard);
    }

    private static void addGuard(ControlFlow controlFlow, String guard) {
        PokaYokeUmlProfileUtil.setIncomingGuard(controlFlow, guard);
    }
}
