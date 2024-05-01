
package com.github.tno.pokayoke.transform.petrify2uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.petrify2uml.PetriNet2Activity;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

import fr.lip6.move.pnml.framework.utils.exception.ImportException;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;

/** Regression test for the translation from Petrify output to Activity. */
class PetriNet2ActivityRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "pnml";

    public static final String OUTPUT_FILE_EXTENSION = "umltst";

    public static final String REGRESSIONTESTS_NAME = "regressiontests-pnml2uml";

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(OUTPUT_FILE_EXTENSION, new UMLTestFactory());
    }

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        RegressionTest.setRegressionTestsName(REGRESSIONTESTS_NAME);
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException, ImportException, InvalidIDException {
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path umlOutputFilePath = outputPath.resolve(filePrefix + "." + OUTPUT_FILE_EXTENSION);

        PetriNet2Activity petriNet2Activity = new PetriNet2Activity();
        petriNet2Activity.transformFile(inputPath.toString(), umlOutputFilePath.toString());
    }
}
