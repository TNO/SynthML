
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.google.common.base.Preconditions;

import fr.lip6.move.pnml.framework.utils.PNMLUtils;
import fr.lip6.move.pnml.framework.utils.exception.BadFileFormatException;
import fr.lip6.move.pnml.framework.utils.exception.ImportException;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;
import fr.lip6.move.pnml.framework.utils.exception.InvocationFailedException;
import fr.lip6.move.pnml.framework.utils.exception.OCLValidationFailed;
import fr.lip6.move.pnml.framework.utils.exception.OtherException;
import fr.lip6.move.pnml.framework.utils.exception.UnhandledNetType;
import fr.lip6.move.pnml.framework.utils.exception.ValidationFailedException;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI;

public class PNMLUMLFileHelper {
    private PNMLUMLFileHelper() {
    }

    public static PetriNet readPetriNet(String inputPath) throws ImportException, InvalidIDException {
        PetriNetDocHLAPI netAPI = (PetriNetDocHLAPI)PNMLUtils.importPnmlDocument(new File(inputPath), false);
        List<PetriNet> nets = netAPI.getNets();
        Preconditions.checkArgument(nets.size() == 1, "Expected the PNML document contains exactly one Petri Net.");
        return nets.get(0);
    }

    public static void writePetriNet(PetriNet petriNet, String outputPath) {
        try {
            PNMLUtils.exportPetriNetDocToPNML(petriNet.getContainerPetriNetDoc(), outputPath);
        } catch (UnhandledNetType | OCLValidationFailed | IOException | ValidationFailedException
                | BadFileFormatException | OtherException | InvalidIDException | InvocationFailedException e)
        {
            throw new RuntimeException("Failed to write Petri Net into " + outputPath, e);
        }
    }

    public static void storeModel(Model model, String pathName) throws IOException {
        // Initialize a UML resource set to store the model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Store the model.
        URI uri = URI.createFileURI(pathName);
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(model);
        resource.save(Collections.EMPTY_MAP);
    }
}
