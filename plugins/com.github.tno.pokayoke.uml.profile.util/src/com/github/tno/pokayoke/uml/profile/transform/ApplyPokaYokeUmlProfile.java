
package com.github.tno.pokayoke.uml.profile.transform;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.uml2.uml.BodyOwner;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import SynthML.FormalElement;

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
            } else if (o instanceof OpaqueBehavior ob) {
                applyUmlProfile(ob);
            }
        });
    }

    /**
     * Applies {@link FormalElement} stereotype and copies the {@link BodyOwner#getBodies()} to its
     * {@link FormalElement#setGuard(String) guard} and {@link FormalElement#getEffects() effects}. If the
     * {@link FormalElement} stereotype is already applied, copies its <code>guard</code> and <code>effects</code> back
     * to the <code>bodies</code> of the <code>action</code>.
     *
     * @param action The action to transform
     */
    private static void applyUmlProfile(OpaqueAction action) {
        if (PokaYokeUmlProfileUtil.isFormalElement(action)) {
            String guard = PokaYokeUmlProfileUtil.getGuard(action);
            List<String> effects = PokaYokeUmlProfileUtil.getEffects(action);
            if (effects.size() > 1) {
                throw new RuntimeException("Multi-effects are not supported on OpaqueActions");
            }
            // Data provisioned on stereotype, copy to bodies
            action.getBodies().clear();
            action.getBodies().add(Strings.isNullOrEmpty(guard) ? "true" : guard);
            if (!effects.isEmpty()) {
                action.getBodies().addAll(Arrays.asList(effects.get(0).split(",")));
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
        PokaYokeUmlProfileUtil.setEffects(action, Arrays.asList(effectsBuilder.toString()));
    }

    /**
     * Applies {@link FormalElement} stereotype and copies the {@link BodyOwner#getBodies()} to its
     * {@link FormalElement#setGuard(String) guard} and {@link FormalElement#getEffects() effects}. If the
     * {@link FormalElement} stereotype is already applied, copies its <code>guard</code> and <code>effects</code> back
     * to the <code>bodies</code> of the <code>action</code>.
     *
     * @param behavior The action to transform
     */
    private static void applyUmlProfile(OpaqueBehavior behavior) {
        if (PokaYokeUmlProfileUtil.isFormalElement(behavior)) {
            String guard = PokaYokeUmlProfileUtil.getGuard(behavior);
            List<String> effects = PokaYokeUmlProfileUtil.getEffects(behavior);

            // Data provisioned on stereotype, copy to bodies
            effects.add(0, Strings.isNullOrEmpty(guard) ? "true" : guard);
            ECollections.setEList(behavior.getBodies(), effects);
            return;
        } else if (behavior.getBodies().isEmpty()) {
            return;
        }
        Iterator<String> bodiesIterator = behavior.getBodies().iterator();
        PokaYokeUmlProfileUtil.setGuard(behavior, bodiesIterator.next());
        PokaYokeUmlProfileUtil.setEffects(behavior, Lists.newArrayList(bodiesIterator));
    }
}
