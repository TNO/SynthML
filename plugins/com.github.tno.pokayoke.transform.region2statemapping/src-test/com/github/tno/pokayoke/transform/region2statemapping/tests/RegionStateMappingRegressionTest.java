
package com.github.tno.pokayoke.transform.region2statemapping.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.region2statemapping.ExtractRegionStateMapping;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/** Regression tests for extracting region-state map. */
public class RegionStateMappingRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_1_EXTENSION = "g";

    public static final String INPUT_FILE_2_EXTENSION = "out";

    public Stream<? extends Arguments> provideArguments() throws Exception {
        return super.provideArguments(INPUT_FILE_1_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException {
        ExtractRegionStateMapping.extractMappingFromFiles(inputPath,
                getSecondInputPathFrom(inputPath), outputPath);
    }

    private Path getSecondInputPathFrom(Path inputPath) {
        String inputFilePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        return inputPath.getParent().resolve(inputFilePrefix + "." + INPUT_FILE_2_EXTENSION);
    }
}
