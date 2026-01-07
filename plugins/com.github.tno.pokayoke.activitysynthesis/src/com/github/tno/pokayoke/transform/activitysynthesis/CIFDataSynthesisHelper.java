////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.activitysynthesis;

import static org.eclipse.escet.common.java.Lists.list;

import java.nio.file.Path;
import java.util.List;

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

    public static CifDataSynthesisSettings getSynthesisSettings() {
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();
        settings.setDoForwardReach(true);
        settings.setFixedPointComputationsOrder(FixedPointComputationsOrder.REACH_NONBLOCK_CTRL);
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
