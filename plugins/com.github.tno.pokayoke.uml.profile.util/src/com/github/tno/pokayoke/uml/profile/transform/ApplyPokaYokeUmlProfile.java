
package com.github.tno.pokayoke.uml.profile.transform;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.uml2.uml.BodyOwner;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

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
                applyUmlProfile(oa, false);
            } else if (o instanceof OpaqueBehavior ob) {
                applyUmlProfile(ob, true);
            }
        });
    }

    /**
     * Applies {@link FormalElement} stereotype and copies the {@link BodyOwner#getBodies()} to its
     * {@link FormalElement#setGuard(String)} and {@link FormalElement#setEffects(String)}. If the {@link FormalElement}
     * stereotype is already applied, copies its <code>guard</code> and <code>effects</code> back to the
     * <code>bodies</code> of the <code>action</code>.
     *
     * @param <T> type that can both be a {@link FormalElement} and a {@link BodyOwner}.
     * @param element The action to transform
     * @param multiEffects {@code true} if the remaining bodies should be treated as separate effects.
     */
    private static <T extends RedefinableElement & BodyOwner> void applyUmlProfile(T element, boolean multiEffects) {
        String guard = PokaYokeUmlProfileUtil.getGuard(element);
        String effects = PokaYokeUmlProfileUtil.getEffects(element);
        if (guard != null || effects != null) {
            // Data provisioned on stereotype, copy to bodies
            element.getBodies().clear();
            element.getBodies().add(guard == null || guard.isEmpty() ? "true" : guard);
            if (effects != null) {
                if (multiEffects) {
                    element.getBodies().add(effects);
                } else {
                    element.getBodies().addAll(Arrays.asList(effects.split(",")));
                }
            }
            return;
        } else if (element.getBodies().isEmpty()) {
            return;
        }
        Iterator<String> bodiesIterator = element.getBodies().iterator();
        PokaYokeUmlProfileUtil.setGuard(element, bodiesIterator.next());
        if (!bodiesIterator.hasNext()) {
            return;
        }
        StringBuilder effectsBuilder = new StringBuilder(bodiesIterator.next());
        if (multiEffects && bodiesIterator.hasNext()) {
            throw new RuntimeException("Multiple effects are not supported yet!");
        }
        while (bodiesIterator.hasNext()) {
            effectsBuilder.append(",\n").append(bodiesIterator.next());
        }
        PokaYokeUmlProfileUtil.setEffects(element, effectsBuilder.toString());
    }
}
