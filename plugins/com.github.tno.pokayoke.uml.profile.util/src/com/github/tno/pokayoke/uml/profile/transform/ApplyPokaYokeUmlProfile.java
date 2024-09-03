
package com.github.tno.pokayoke.uml.profile.transform;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

import PokaYoke.FormalElement;

public class ApplyPokaYokeUmlProfile {
    private ApplyPokaYokeUmlProfile() {
        // Empty for utility classes
    }

    public static void applyUmlProfile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        applyUmlProfile(model);
        FileHelper.storeModel(model, targetPath);
    }

    private static void applyUmlProfile(Model model) {
        model.eAllContents().forEachRemaining(o -> {
            if (o instanceof OpaqueAction oa) {
                applyUmlProfile(oa);
            }
        });
    }

    /**
     * Applies {@link FormalElement} stereotype and copies the {@link OpaqueAction#getBodies()} to its
     * {@link FormalElement#setGuard(String)} and {@link FormalElement#setEffects(String)}. If the {@link FormalElement}
     * stereotype is already applied, copies its <code>guard</code> and <code>effects</code> back to the
     * <code>bodies</code> of the <code>action</code>.
     *
     * @param action The action to transform
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
