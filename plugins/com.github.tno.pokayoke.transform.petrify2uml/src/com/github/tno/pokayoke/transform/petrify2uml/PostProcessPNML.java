
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Arc;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

public class PostProcessPNML {
    private PostProcessPNML() {
    }

    /**
     * Removes the loop introduced when transforming CIF to Petrify input, that connects initial locations to ones where
     * the activity postcondition is satisfied. Such a loop is needed since Petrify does not work well with sink states.
     *
     * @param petriNet The Petri net to remove the loop from.
     */
    public static void removeLoop(PetriNet petriNet) {
        List<Page> pages = petriNet.getPages();
        Preconditions.checkArgument(pages.size() == 1,
                "Expected that there is exactly one Petri Net page in the Petri Net.");
        Page page = pages.get(0);

        // Get the marked place.
        List<Place> places = page.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast).toList();
        List<Place> markedPlaces = places.stream().filter(place -> place.getInitialMarking() != null).toList();
        Preconditions.checkArgument(markedPlaces.size() == 1, "Expected that there is exactly one marked place.");
        Place markedPlace = markedPlaces.get(0);

        // Get the source of the incoming arc into the marked place.
        List<Arc> inArcs = markedPlace.getInArcs();
        Preconditions.checkArgument(inArcs.size() == 1,
                "Expected that there is exactly one incoming arc for the marked place.");
        Transition sourceTransition = (Transition)inArcs.get(0).getSource();

        // Remove the arc between end transition and the marked place.
        List<Arc> outArcs = sourceTransition.getOutArcs();
        Preconditions.checkArgument(outArcs.size() == 1, String.format(
                "Expected that there is exactly one outgoing arc for transition %s.", sourceTransition.getName()));
        page.getObjects().remove(outArcs.get(0));
        markedPlace.getInArcs().remove(0);
        sourceTransition.getOutArcs().remove(0);

        // Add a new arc between the transition and a newly created final place.
        String finalPlace = "FinalPlace";
        Petrify2PNMLTranslator.createArc(sourceTransition, Petrify2PNMLTranslator.createPlace(finalPlace, page), page);
    }
}
