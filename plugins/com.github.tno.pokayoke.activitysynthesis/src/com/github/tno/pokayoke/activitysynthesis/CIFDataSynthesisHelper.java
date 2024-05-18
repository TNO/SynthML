
package com.github.tno.pokayoke.activitysynthesis;

import static org.eclipse.escet.common.java.Lists.list;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.CifDataSynthesis;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisTiming;
import org.eclipse.escet.cif.datasynth.conversion.SynthesisToCifConverter;
import org.eclipse.escet.cif.datasynth.settings.BddSimplify;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;

import com.github.javabdd.BDDFactory;

/** Helper for performing CIF data synthesis. */
public class CIFDataSynthesisHelper {
    private CIFDataSynthesisHelper() {
    }

    public static CifDataSynthesisSettings getSynthesisSettings() {
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();
        settings.setDoForwardReach(true);
        settings.setBddSimplifications(EnumSet.noneOf(BddSimplify.class));
        return settings;
    }

    public static CifBddSpec getCifBddSpec(Specification spec, CifDataSynthesisSettings settings) {
        // Perform preprocessing.
        CifToBddConverter.preprocess(spec, settings.getWarnOutput(), settings.getDoPlantsRefReqsWarn());

        // Create BDD factory.
        List<Long> continuousOpMisses = list();
        List<Integer> continuousUsedBddNodes = list();
        BDDFactory factory = CifToBddConverter.createFactory(settings, continuousOpMisses, continuousUsedBddNodes);

        // Convert CIF specification to a CIF/BDD representation, checking for precondition violations along the
        // way.
        CifToBddConverter converter = new CifToBddConverter("Data-based supervisory controller synthesis");
        CifBddSpec cifBddSpec = converter.convert(spec, settings, factory);

        return cifBddSpec;
    }

    public static CifDataSynthesisResult synthesize(CifBddSpec cifBddSpec, CifDataSynthesisSettings settings) {
        CifDataSynthesisResult synthResult = CifDataSynthesis.synthesize(cifBddSpec, settings,
                new CifDataSynthesisTiming());
        return synthResult;
    }

    public static Specification convertSynthesisResultToCif(Specification spec, CifDataSynthesisResult synthResult,
            String outPutFilePath, String outFolderPath)
    {
        Specification result;

        // Construct output CIF specification.
        SynthesisToCifConverter converter = new SynthesisToCifConverter();
        result = converter.convert(synthResult, spec);

        // Write output CIF specification.
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(result, outPutFilePath, outFolderPath);
        } finally {
            AppEnv.unregisterApplication();
        }
        return result;
    }
}
