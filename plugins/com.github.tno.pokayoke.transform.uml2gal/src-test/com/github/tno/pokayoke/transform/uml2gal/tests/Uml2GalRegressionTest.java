
package com.github.tno.pokayoke.transform.uml2gal.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.tests.common.RegressionTest;
import com.github.tno.pokayoke.transform.uml2gal.Uml2GalTranslationHelper;

/** Regression tests for translating UML models to GAL specifications. */
public class Uml2GalRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String OUTPUT_FILE_EXTENSION = "gal";

    public static final String TRACE_FILE_EXTENSION = "json";

    public Stream<? extends Arguments> provideArguments() throws Exception {
        return super.provideArguments(INPUT_FILE_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException, CoreException {
        try {
            String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
            Path galOutputFilePath = outputPath.resolve(filePrefix + "." + OUTPUT_FILE_EXTENSION);
            Files.createDirectories(outputPath);
            Uml2GalTranslationHelper.translateCifAnnotatedModel(inputPath.toString(), galOutputFilePath.toString(),
                    getTracePathFrom(galOutputFilePath).toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyTest(Path expectedPath, Path outputPath, String message) throws IOException {
        super.verifyTest(expectedPath, outputPath, message);
    }

    @Override
    protected void tearDownTest(Path outputPath) throws IOException {
        super.tearDownTest(outputPath);
        super.tearDownTest(getTracePathFrom(outputPath));
    }

    private Path getTracePathFrom(Path outputPath) {
        String outputFilePrefix = FilenameUtils.removeExtension(outputPath.getFileName().toString());
        return outputPath.getParent().resolve(outputFilePrefix + "." + TRACE_FILE_EXTENSION);
    }
}
