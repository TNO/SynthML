
package com.github.tno.pokayoke.transform.uml2cif;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.uml2.uml.Model;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

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
        Path outputFilePath = outputPath.resolve(filePrefix + "." + OUTPUT_FILE_EXTENSION);
        Files.createDirectories(outputPath);

        // Load UML synthesis specification.
        Model umlModel = FileHelper.loadModel(inputPath.toString());

        // Translate to CIF specification.
        Specification cifSpecification = new UmlToCifTranslator(umlModel).translate();

        // Store CIF specification.
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(cifSpecification, outputFilePath.toAbsolutePath().toString(),
                    outputFilePath.toString());
        } finally {
            AppEnv.unregisterApplication();
        }
    }
}
