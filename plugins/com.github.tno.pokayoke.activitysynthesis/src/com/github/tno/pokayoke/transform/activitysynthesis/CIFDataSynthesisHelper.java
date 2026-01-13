
package com.github.tno.pokayoke.transform.activitysynthesis;

import static org.eclipse.escet.common.java.Lists.list;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.datasynth.CifDataSynthesis;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisTiming;
import org.eclipse.escet.cif.datasynth.conversion.SynthesisToCifConverter;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.datasynth.settings.FixedPointComputationsOrder;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.java.PathPair;
import org.eclipse.escet.common.java.Termination;

import com.github.javabdd.BDDFactory;

/** Helper for performing CIF data synthesis. */
public class CIFDataSynthesisHelper {
    private CIFDataSynthesisHelper() {
    }

    public static CifDataSynthesisSettings getSynthesisSettings(Map<String, String> synthSetting) {
        // Define the configuration for performing data-based synthesis and symbolic reachability searches.
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();

        if (synthSetting.containsKey("--forward-reach")) {
            settings.setDoForwardReach(Boolean.valueOf(synthSetting.get("--forward-reach")));
        } else {
            settings.setDoForwardReach(true); // Get correct and intuitive result.
        }

        if (synthSetting.containsKey("--fixed-point-order")) {
            String fpo = synthSetting.get("--fixed-point-order");

            switch (fpo) {
                case "ctrl-nonblock-reach" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.CTRL_NONBLOCK_REACH);
                }
                case "ctrl-reach-nonblock" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.CTRL_REACH_NONBLOCK);
                }
                case "nonblock-ctrl-reach" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.NONBLOCK_CTRL_REACH);
                }
                case "nonblock-reach-ctrl" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.NONBLOCK_REACH_CTRL);
                }
                case "reach-ctrl-nonblock" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.REACH_CTRL_NONBLOCK);
                }
                case "reach-nonblock-ctrl" -> {
                    settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.REACH_NONBLOCK_CTRL);
                }
                default -> {
                    throw new IllegalArgumentException("Unexpected fixed point order: " + fpo);
                }
            }
        } else {
            // Best performance.
            settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.REACH_NONBLOCK_CTRL);
        }

        if (synthSetting.containsKey("--bdd-table")) {
            settings.setBddInitNodeTableSize(Integer.valueOf(synthSetting.get("--bdd-table")));
        }

        if (synthSetting.containsKey("--bdd-cache-ratio")) {
            settings.setBddOpCacheRatio(Double.valueOf(synthSetting.get("--bdd-cache-ratio")));
        }

        return settings;
    }

    public static CifBddSpec getCifBddSpec(Specification spec, String specAbsPath, CifDataSynthesisSettings settings) {
        // Perform preprocessing.
        CifToBddConverter converter = new CifToBddConverter("Data-based supervisory controller synthesis");
        converter.preprocess(spec, specAbsPath, settings.getWarnOutput(), settings.getDoPlantsRefReqsWarn(),
                Termination.NEVER);

        // Create BDD factory.
        List<Long> continuousOpMisses = list();
        List<Integer> continuousUsedBddNodes = list();
        BDDFactory factory = CifToBddConverter.createFactory(settings, continuousOpMisses, continuousUsedBddNodes);

        // Convert CIF specification to a CIF/BDD representation, checking for precondition violations along the way.
        CifBddSpec cifBddSpec = converter.convert(spec, settings, factory);

        return cifBddSpec;
    }

    public static CifDataSynthesisResult synthesize(CifBddSpec cifBddSpec, CifDataSynthesisSettings settings) {
        CifDataSynthesisResult synthResult = CifDataSynthesis.synthesize(cifBddSpec, settings,
                new CifDataSynthesisTiming());
        return synthResult;
    }

    public static Specification convertSynthesisResultToCif(Specification spec, CifDataSynthesisResult synthResult,
            Path outputFilePath, String outFolderPath)
    {
        Specification result;

        // Construct output CIF specification.
        SynthesisToCifConverter converter = new SynthesisToCifConverter();
        result = converter.convert(synthResult, spec);

        // Write output CIF specification.
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(result,
                    new PathPair(outputFilePath.toString(), outputFilePath.toAbsolutePath().toString()), outFolderPath);
        } finally {
            AppEnv.unregisterApplication();
        }
        return result;
    }
}
