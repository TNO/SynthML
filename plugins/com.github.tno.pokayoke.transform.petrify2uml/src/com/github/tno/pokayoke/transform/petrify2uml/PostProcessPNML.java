
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

    public static void removeLoop(PetriNet petriNet) {
        List<Page> pages = petriNet.getPages();
        Preconditions.checkArgument(pages.size() == 1,
                "Expected that there is exactly one Petri Net in the Petri Net page.");
        Page page = pages.get(0);
        List<Transition> endTransitions = page.getObjects().stream().filter(Transition.class::isInstance)
                .map(Transition.class::cast).filter(transition -> transition.getName().getText().equals("end"))
                .toList();
        Preconditions.checkArgument(endTransitions.size() == 1,
                "Expected that there is exactly one transition named 'end'.");
        Transition endTransition = endTransitions.get(0);

        List<Place> places = page.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast).toList();
        List<Place> markedPlaces = places.stream().filter(place -> place.getInitialMarking() != null).toList();
        Place markedPlace = markedPlaces.get(0);

        // Remove the arc between end transition and the marked place.
        endTransition.getOutArcs().stream().forEach(arc -> page.getObjects().remove(arc));
        markedPlace.getInArcs().remove(0);
        endTransition.getOutArcs().remove(0);

        String finalPlace = "FinalPlace";
        Petrify2PNMLTranslator.createArc(endTransition, Petrify2PNMLTranslator.createPlace(finalPlace, page), page);
    }
}
