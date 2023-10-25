
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
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected that the Petri Net has exactly one page");
        Page page = petriNet.getPages().get(0);
        Activity activity = PetriNet2ActivityHelper.initializeUMLActivity(page);

        // Translate all transitions into actions.
        page.getObjects().stream().filter(pnObj -> pnObj instanceof Transition).forEach(
                transition -> PetriNet2ActivityHelper.transformTransition(((Transition)transition).getId(), activity));

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

        // Translate the places which have exactly one incoming arc and one outgoing arc to one edge that connects two
        // actions.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.isOneToOnePattern(place)) {
                    PetriNet2ActivityHelper.transformOneToOnePattern(place, activity);
                }
            }
        }

        // Translate the patterns for merge and decision merge nodes. These nodes are created and properly
        // connected to the translated actions. After this step, all places are translated and all the activity nodes
        // are connected.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Place place) {
                if (PetriNet2ActivityHelper.isMergePattern(place)) {
                    PetriNet2ActivityHelper.transformMergePattern(place, activity);
                } else if (PetriNet2ActivityHelper.isDecisionPattern(place)) {
                    PetriNet2ActivityHelper.transformDecisionPattern(place, activity);
                } else if (PetriNet2ActivityHelper.isMergeDecisionPattern(place)) {
                    PetriNet2ActivityHelper.transformMergeDecisionPattern(place, activity);
                }
            }
        }

        // Translate the patterns for fork and join nodes. These nodes are created and properly
        // connected to other activity nodes.
        for (PnObject pnObj: page.getObjects()) {
            if (pnObj instanceof Transition transition) {
                if (PetriNet2ActivityHelper.isForkPattern(transition)) {
                    PetriNet2ActivityHelper.transformForkPattern(transition, activity);
                } else if (PetriNet2ActivityHelper.isJoinPattern(transition)) {
                    PetriNet2ActivityHelper.transformJoinPattern(transition, activity);
                } else if (PetriNet2ActivityHelper.isForkJoinPattern(transition)) {
                    PetriNet2ActivityHelper.transformForkJoinPattern(transition, activity);
                }
            }
        }

        // Rename the actions translated from duplicate transitions to have the same name (i.e., remove the postfix).
        PetriNet2ActivityHelper.renameDuplicateActions();

        return activity;
    }
}
