
package com.github.tno.pokayoke.transform.uml2cif;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Model;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;
import com.github.tno.pokayoke.transform.track.SynthesisUmlElementTracking;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator.TranslationPurpose;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Uml2CifRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String OUTPUT_FILE_EXTENSION = "cif";

    public static final String REGRESSIONTESTS_NAME = "regressiontests";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION, REGRESSIONTESTS_NAME);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws Exception {
        // Prepare output directory.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Files.createDirectories(outputPath);

        // Load UML synthesis specification.
        Model umlModel = FileHelper.loadModel(inputPath.toString());
        FileHelper.normalizeIds(umlModel);

        // Find and translate every abstract UML activity in the loaded UML model to a separate CIF specification.
        List<Activity> activities = new CifContext(umlModel).getAllAbstractActivities();

        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            Preconditions.checkArgument(!Strings.isNullOrEmpty(activity.getName()), "Expected activities to be named.");

            // Prepare the output path for translating the current UML activity.
            Path localOutputPath = outputPath.resolve(String.format("%d - %s", i + 1, activity.getName()));
            Files.createDirectories(localOutputPath);
            Path outputFilePath = localOutputPath.resolve(filePrefix + "." + OUTPUT_FILE_EXTENSION);

            // Translate the current UML activity to a CIF specification.
            Specification cifSpecification = new UmlToCifTranslator(activity, TranslationPurpose.SYNTHESIS,
                    new SynthesisUmlElementTracking()).translate();

            // Store the translated CIF specification.
            try {
                AppEnv.registerSimple();
                CifWriter.writeCifSpec(cifSpecification, outputFilePath.toAbsolutePath().toString(),
                        outputFilePath.toString());
            } finally {
                AppEnv.unregisterApplication();
            }
        }
    }
}
