
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Transition;

public class PostProcessPNML {
    private PostProcessPNML() {
    }

    /**
     * Removes the loop introduced when transforming CIF to Petrify input, i.e., the 'reset transition' that connects
     * the final location where the activity postcondition is satisfied to the initial location. Such a loop is needed
     * since Petrify does not work well with sink states.
     *
     * @param petriNet The Petri net to remove the loop from.
     */
    public static void removeLoop(PetriNet petriNet) {
        List<Page> pages = petriNet.getPages();
        Preconditions.checkArgument(pages.size() == 1,
                "Expected that there is exactly one Petri Net page in the Petri Net.");
        Page page = pages.get(0);

        // Find the single reset transition in the Petri Net.
        List<Transition> resetTransitions = page.getObjects().stream().filter(Transition.class::isInstance)
                .map(Transition.class::cast).filter(t -> t.getName().getText().equals("__reset")).toList();
        Preconditions.checkArgument(resetTransitions.size() == 1,
                "Expected exactly one reset transition, but got " + resetTransitions.size());
        Transition resetTransition = resetTransitions.get(0);

        // Remove the reset transition and all arcs attached to it.
        resetTransition.getInArcs().forEach(arc -> arc.getSource().getOutArcs().remove(arc));
        resetTransition.getOutArcs().forEach(arc -> arc.getTarget().getInArcs().remove(arc));
        page.getObjects().removeAll(resetTransition.getInArcs());
        page.getObjects().removeAll(resetTransition.getOutArcs());
        page.getObjects().remove(resetTransition);
    }
}
