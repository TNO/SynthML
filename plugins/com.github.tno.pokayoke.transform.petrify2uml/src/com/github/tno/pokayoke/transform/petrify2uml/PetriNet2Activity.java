
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.util.List;

import org.eclipse.uml2.uml.Activity;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.PnObject;
import fr.lip6.move.pnml.ptnet.Transition;

/** Transforms Petri Net to Activity. */
public class PetriNet2Activity {
    private PetriNet2Activity() {
    }

    public static void transformFile(String inutPath, String outputPath) throws IOException {
        List<String> input = FileHelper.readFile(inutPath);
        PetriNet petriNet = Petrify2PNMLTranslator.transform(input);
        Activity activity = transform(petriNet);
        FileHelper.storeModel(activity.getModel(), outputPath);
    }

    public static Activity transform(PetriNet petriNet) {
        // According to PNML documents, each Petri Net needs to contain at least one page. Users can add multiple pages
        // to structure their Petri Net in various ways.In our transformation, we add only one page that is mandatory.
        // See more info in : https://dev.lip6.fr/trac/research/ISOIEC15909/wiki/English/User/Structure.
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected that the Petri Net has exactly one page");
        Page page = petriNet.getPages().get(0);
        Activity activity = PetriNet2ActivityHelper.initializeUMLActivity(page);

        // Translate all transitions into actions.
        page.getObjects().stream().filter(Transition.class::isInstance).map(Transition.class::cast)
                .forEach(transition -> PetriNet2ActivityHelper.transformTransition(transition.getId(), activity));

        // Translate the marked place and final place into initial and final nodes respectively.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.isMarkedPlace(place)) {
                    PetriNet2ActivityHelper.transformMarkedPlace(activity, place);
                } else if (PetriNet2ActivityHelper.isFinalPlace(place)) {
                    PetriNet2ActivityHelper.transformFinalPlace(activity, place);
                }
            }
        }

        // Translate the place-based patterns based on the number of incoming and outgoing arcs of the place.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.hasSingleInArc(place)) {
                    if (PetriNet2ActivityHelper.hasSingleOutArc(place)) {
                        PetriNet2ActivityHelper.transformOneToOnePattern(place, activity);
                    } else if (PetriNet2ActivityHelper.hasMultiOutArcs(place)) {
                        PetriNet2ActivityHelper.transformDecisionPattern(place, activity);
                    }
                } else if (PetriNet2ActivityHelper.hasMultiInArcs(place)) {
                    if (PetriNet2ActivityHelper.hasSingleOutArc(place)) {
                        PetriNet2ActivityHelper.transformMergePattern(place, activity);
                    } else if (PetriNet2ActivityHelper.hasMultiOutArcs(place)) {
                        PetriNet2ActivityHelper.transformMergeDecisionPattern(place, activity);
                    }
                }
            }
        }

        // Translate the transition-based patterns based on the number of incoming and outgoing arcs of the transition.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Transition transition) {
                if (PetriNet2ActivityHelper.hasMultiInArcs(transition)) {
                    PetriNet2ActivityHelper.transformJoinPattern(transition, activity);
                }
                if (PetriNet2ActivityHelper.hasMultiOutArcs(transition)) {
                    PetriNet2ActivityHelper.transformForkPattern(transition, activity);
                }
            }
        }

        // Rename the actions translated from duplicate transitions to have the same name (i.e., remove the postfix).
        PetriNet2ActivityHelper.renameDuplicateActions();

        return activity;
    }
}
