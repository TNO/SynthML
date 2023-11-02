
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.util.List;

import org.eclipse.uml2.uml.Activity;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;

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
        // to structure their Petri Net in various ways. In our transformation, we add only one page that is mandatory.
        // See more info in : https://dev.lip6.fr/trac/research/ISOIEC15909/wiki/English/User/Structure.
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected that the Petri Net has exactly one page");
        Page page = petriNet.getPages().get(0);

        Activity activity = PetriNet2ActivityHelper.initializeUMLActivity(page);

        PetriNet2ActivityHelper.transformTransitions(page, activity);
        PetriNet2ActivityHelper.transformMarkedAndFinalPlaces(page, activity);
        PetriNet2ActivityHelper.transformPlaceBasedPatterns(page, activity);
        PetriNet2ActivityHelper.transformTransitionBasedPatterns(page, activity);

        PetriNet2ActivityHelper.renameDuplicateActions();

        return activity;
    }
}
