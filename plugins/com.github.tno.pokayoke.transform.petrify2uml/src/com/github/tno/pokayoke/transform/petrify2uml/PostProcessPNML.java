
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

public class PostProcessPNML {
    private PostProcessPNML() {
    }

    /**
     * Removes the self-loop that was introduced when transforming CIF to Petrify input, i.e., the 'done transition'
     * that connects the final location where the activity postcondition is satisfied to itself. Such a loop is needed
     * since Petrify does not work well with sink states.
     *
     * @param petriNet The Petri net to remove the self-loop from.
     */
    public static void removeLoop(PetriNet petriNet) {
        List<Page> pages = petriNet.getPages();
        Preconditions.checkArgument(pages.size() == 1,
                "Expected that there is exactly one Petri Net page in the Petri Net.");
        Page page = pages.get(0);

        // Find the single loop transition in the Petri Net.
        List<Transition> resetTransitions = page.getObjects().stream().filter(Transition.class::isInstance)
                .map(Transition.class::cast).filter(t -> t.getName().getText().equals("__loop")).toList();
        Preconditions.checkArgument(resetTransitions.size() == 1,
                "Expected exactly one done transition, but got " + resetTransitions.size());
        Transition resetTransition = resetTransitions.get(0);

        // Make sure that this loop transition is indeed a self-loop.
        List<Place> incomingPlaces = resetTransition.getInArcs().stream().map(a -> (Place)a.getSource()).toList();
        List<Place> outgoingPlaces = resetTransition.getOutArcs().stream().map(a -> (Place)a.getTarget()).toList();
        Preconditions.checkArgument(incomingPlaces.size() == 1,
                "Expected the done transition to be connected to a single incoming place.");
        Preconditions.checkArgument(outgoingPlaces.size() == 1,
                "Expected the done transition to be connected to a single outgoing place.");
        Preconditions.checkArgument(incomingPlaces.get(0).equals(outgoingPlaces.get(0)),
                "Expected the done transition to be a self-loop.");

        // Remove the self-loop and all arcs attached to it.
        resetTransition.getInArcs().forEach(arc -> arc.getSource().getOutArcs().remove(arc));
        resetTransition.getOutArcs().forEach(arc -> arc.getTarget().getInArcs().remove(arc));
        page.getObjects().removeAll(resetTransition.getInArcs());
        page.getObjects().removeAll(resetTransition.getOutArcs());
        page.getObjects().remove(resetTransition);
    }
}
