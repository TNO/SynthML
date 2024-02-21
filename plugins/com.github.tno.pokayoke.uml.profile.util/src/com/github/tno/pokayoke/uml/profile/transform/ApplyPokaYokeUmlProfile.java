/**
 * 
 */
package com.github.tno.pokayoke.uml.profile.transform;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.util.GuardsEffectsUtil;

/**
 * 
 */
public class ApplyPokaYokeUmlProfile {
    public static void applyUmlProfile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        applyUmlProfile(model);

        // Initialize a UML resource set to store the model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Store the model.
        URI uri = URI.createFileURI(targetPath);
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().addAll(model.eResource().getContents());
        resource.save(Collections.EMPTY_MAP);
    }

    private static void applyUmlProfile(Model model) {
    	model.eAllContents().forEachRemaining(o -> {
    		if (o instanceof OpaqueAction) 
    			applyUmlProfile((OpaqueAction) o);
		});
    }

    private static void applyUmlProfile(OpaqueAction action) {
    	if (action.getBodies().isEmpty()) {
    		return;
    	}
    	Iterator<String> bodiesIterator = action.getBodies().iterator();
    	GuardsEffectsUtil.setGuard(action, bodiesIterator.next());
    	if (!bodiesIterator.hasNext()) {
    		return;
    	}
    	StringBuilder effects = new StringBuilder(bodiesIterator.next());
    	while (bodiesIterator.hasNext()) {
    		effects.append(",\n").append(bodiesIterator.next());
    	}
    	GuardsEffectsUtil.setEffects(action, effects.toString());
    }
}
