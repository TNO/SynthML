
package com.github.tno.pokayoke.uml.profile.transform;

import java.io.IOException;
import java.util.Arrays;
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
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

import PokaYoke.GuardEffectsAction;

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
            if (o instanceof OpaqueAction oa) {
                applyUmlProfile(oa);
            }
        });
    }

    /**
     * Applies {@link GuardEffectsAction} stereotype and copies the {@link OpaqueAction#getBodies()} to its
     * {@link GuardEffectsAction#setGuard(String)} and {@link GuardEffectsAction#setEffects(String)}. If the
     * {@link GuardEffectsAction} stereotype is already applied, copies its <code>guard</code> and <code>effects</code>
     * back to the <code>bodies</code> of the <code>action</code>.
     * 
     * @param action the action to transform
     */
    private static void applyUmlProfile(OpaqueAction action) {
        String guard = PokaYokeUmlProfileUtil.getGuard(action);
        String effects = PokaYokeUmlProfileUtil.getEffects(action);
        if (guard != null || effects != null) {
            // Data provisioned on stereotype, copy to bodies
            action.getBodies().clear();
            action.getBodies().add(guard == null || guard.isEmpty() ? "true" : guard);
            if (effects != null) {
                action.getBodies().addAll(Arrays.asList(effects.split(",")));
            }
            return;
        } else if (action.getBodies().isEmpty()) {
            return;
        }
        Iterator<String> bodiesIterator = action.getBodies().iterator();
        PokaYokeUmlProfileUtil.setGuard(action, bodiesIterator.next());
        if (!bodiesIterator.hasNext()) {
            return;
        }
        StringBuilder effectsBuilder = new StringBuilder(bodiesIterator.next());
        while (bodiesIterator.hasNext()) {
            effectsBuilder.append(",\n").append(bodiesIterator.next());
        }
        PokaYokeUmlProfileUtil.setEffects(action, effectsBuilder.toString());
    }
}
