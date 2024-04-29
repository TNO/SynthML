
package com.github.tno.pokayoke.transform.uml2gal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.uml2.uml.Model;
import org.json.JSONException;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Preconditions;

import fr.lip6.move.gal.And;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.Comparison;
import fr.lip6.move.gal.ComparisonOperators;
import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.False;
import fr.lip6.move.gal.GalFactory;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.Or;
import fr.lip6.move.gal.Specification;
import fr.lip6.move.gal.True;
import fr.lip6.move.gal.WrapBoolExpr;
import fr.lip6.move.serialization.SerializationUtil;

/** Helper for translating UML models to GAL specifications. */
public class Uml2GalTranslationHelper {
    static final GalFactory FACTORY = GalFactory.eINSTANCE;

    private static final int BOOL_FALSE = 0;

    private static final int BOOL_TRUE = 1;

    private Uml2GalTranslationHelper() {
        // Empty for utility classes
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
     * @param tracingPath The path to store the JSON tracing information.
     * @throws IOException Thrown in case the model could not be loaded or the specification be stored.
     * @throws JSONException In case generating the tracing JSON failed.
     * @throws CoreException Thrown when model cannot be transformed.
     */
    public static void translateCifAnnotatedModel(String sourcePath, String targetPath, String tracingPath)
            throws IOException, JSONException, CoreException
    {
        // Translate the UML model and store the result.
        Model model = FileHelper.loadModel(sourcePath);
        Uml2GalTranslator translator = new Uml2GalTranslator();
        Specification specification = translator.translate(model);
        store(specification, targetPath);

        // Store the tracing information as JSON.
        try (FileWriter tracingFileWriter = new FileWriter(tracingPath)) {
            tracingFileWriter.write(translator.getTracingAsJson().toString());
        }
    }

    static IntExpression toIntExpression(int value) {
        Constant constant = Uml2GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(value);
        return constant;
    }

    static IntExpression toIntExpression(boolean value) {
        // Below is the reduction of toIntExpression(toBooleanExpression(value));
        return value ? toIntExpression(BOOL_TRUE) : toIntExpression(BOOL_FALSE);
    }

    static IntExpression toIntExpression(BooleanExpression expression) {
        // Wrapped boolean literals are not supported in variable declarations.
        // They do parse, but verification fails. Therefore they are translated into constants.
        if (expression instanceof True) {
            return toIntExpression(BOOL_TRUE);
        } else if (expression instanceof False) {
            return toIntExpression(BOOL_FALSE);
        }

        WrapBoolExpr wrapBoolExpr = FACTORY.createWrapBoolExpr();
        wrapBoolExpr.setValue(expression);
        return wrapBoolExpr;
    }

    static BooleanExpression toBooleanExpression(boolean value) {
        return value ? FACTORY.createTrue() : FACTORY.createFalse();
    }

    static BooleanExpression toBooleanExpression(IntExpression expression) {
        Comparison comparison = FACTORY.createComparison();
        comparison.setOperator(ComparisonOperators.EQ);
        comparison.setLeft(expression);
        comparison.setRight(toIntExpression(true));
        return comparison;
    }

    static And combineAsAnd(BooleanExpression left, BooleanExpression right) {
        And conjunction = FACTORY.createAnd();
        conjunction.setLeft(left);
        conjunction.setRight(right);
        return conjunction;
    }

    static BooleanExpression combineAsAnd(Collection<BooleanExpression> exprs) {
        return exprs.stream().reduce(Uml2GalTranslationHelper::combineAsAnd).orElse(FACTORY.createTrue());
    }

    static Or combineAsOr(BooleanExpression left, BooleanExpression right) {
        Or disjunction = FACTORY.createOr();
        disjunction.setLeft(left);
        disjunction.setRight(right);
        return disjunction;
    }

    static BooleanExpression combineAsOr(Collection<BooleanExpression> exprs) {
        return exprs.stream().reduce(Uml2GalTranslationHelper::combineAsOr).orElse(FACTORY.createTrue());
    }

    static void ensureNameDoesNotContainDollarSign(String name) {
        Preconditions.checkArgument(!name.contains("$"), "Expected a name not containing '$', but got: " + name);
    }
}
