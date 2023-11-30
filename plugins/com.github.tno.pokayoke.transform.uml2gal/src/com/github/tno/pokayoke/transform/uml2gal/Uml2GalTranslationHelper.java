
package com.github.tno.pokayoke.transform.uml2gal;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.uml2.uml.Model;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Preconditions;

import fr.lip6.move.gal.And;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.GalFactory;
import fr.lip6.move.gal.Specification;
import fr.lip6.move.serialization.SerializationUtil;

/** Helper for translating UML models to GAL specifications. */
public class Uml2GalTranslationHelper {
    static final GalFactory FACTORY = GalFactory.eINSTANCE;

    private Uml2GalTranslationHelper() {
    }

    /**
     * Loads a GAL specification from the specified path.
     *
     * @param pathName The path from which to read the GAL specification.
     * @return The loaded GAL specification.
     */
    public static Specification load(String pathName) {
        SerializationUtil.setStandalone(true);
        return SerializationUtil.fileToGalSystem(pathName);
    }

    /**
     * Stores the given GAL specification to the specified path.
     *
     * @param specification The GAL specification to store.
     * @param pathName The path to store the specification.
     * @throws IOException Thrown in case the specification could not be stored.
     */
    public static void store(Specification specification, String pathName) throws IOException {
        SerializationUtil.systemToFile(specification, pathName, true);
    }

    /**
     * Translates a CIF-annotated UML model to a GAL specification.
     *
     * @param sourcePath The path to load the UML model.
     * @param targetPath The path to store the translated GAL specification.
     * @throws IOException Thrown in case the model could not be loaded or the specification be stored.
     */
    public static void translateCifAnnotatedModel(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        Specification specification = new CifAnnotatedUml2GalTranslator(sourcePath).translate(model);
        store(specification, targetPath);
    }

    static And combineAsAnd(BooleanExpression left, BooleanExpression right) {
        And conjunction = Uml2GalTranslationHelper.FACTORY.createAnd();
        conjunction.setLeft(left);
        conjunction.setRight(right);
        return conjunction;
    }

    static BooleanExpression combineAsAnd(Collection<BooleanExpression> exprs) {
        return exprs.stream().reduce(Uml2GalTranslationHelper::combineAsAnd)
                .orElse(Uml2GalTranslationHelper.FACTORY.createTrue());
    }

    static void ensureNameDoesNotContainDollarSign(String name) {
        Preconditions.checkArgument(!name.contains("$"), "Expected a name not containing '$', but got: " + name);
    }
}
