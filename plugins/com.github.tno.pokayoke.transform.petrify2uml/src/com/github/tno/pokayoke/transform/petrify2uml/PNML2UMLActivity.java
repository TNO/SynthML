
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.OpaqueAction;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.framework.utils.exception.ImportException;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;
import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Transition;

/** Transforms Petri Net to Activity. */
public class PNML2UMLActivity {
    private Map<Transition, OpaqueAction> transitionToAction;

    public void transformFile(Path inputPath, Path outputPath) throws ImportException, InvalidIDException, IOException {
        PetriNet petriNet = PNMLUMLFileHelper.readPetriNet(inputPath.toString());
        Activity activity = transform(petriNet);
        PostProcessActivity.removeInternalActions(activity);

        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path umlOutputFilePath = outputPath.resolve(filePrefix + ".uml");
        FileHelper.storeModel(activity.getModel(), umlOutputFilePath.toString());
    }

    public Activity transform(PetriNet petriNet) {
        // According to PNML documents, each Petri Net needs to contain at least one page. Users can add multiple pages
        // to structure their Petri Net in various ways. In our transformation, we add only one page that is mandatory.
        // See more info in : https://dev.lip6.fr/trac/research/ISOIEC15909/wiki/English/User/Structure.
        Preconditions.checkArgument(petriNet.getPages().size() == 1,
                "Expected that the Petri Net has exactly one page");
        Page page = petriNet.getPages().get(0);

        PNML2UMLActivityHelper petriNet2ActivityHelper = new PNML2UMLActivityHelper();
        Activity activity = petriNet2ActivityHelper.initializeUMLActivity(page);

        transitionToAction = petriNet2ActivityHelper.transformTransitions(page, activity);
        petriNet2ActivityHelper.transformMarkedAndFinalPlaces(page, activity);
        petriNet2ActivityHelper.transformPlaceBasedPatterns(page, activity);
        petriNet2ActivityHelper.transformTransitionBasedPatterns(page, activity);

        petriNet2ActivityHelper.renameDuplicateActions();

        return activity;
    }

    public Map<Transition, OpaqueAction> getTransitionActionMap() {
        return transitionToAction;
    }
}
