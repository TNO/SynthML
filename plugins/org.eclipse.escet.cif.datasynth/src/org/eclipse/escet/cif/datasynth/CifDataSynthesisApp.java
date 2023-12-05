//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.datasynth;

import static org.eclipse.escet.common.app.framework.output.OutputProvider.dbg;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.out;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.warn;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Strings.fmt;
import static org.eclipse.escet.common.java.Strings.str;

import java.util.List;
import java.util.Set;

import org.eclipse.escet.cif.cif2cif.ElimComponentDefInst;
import org.eclipse.escet.cif.cif2cif.RemoveIoDecls;
import org.eclipse.escet.cif.datasynth.bdd.BddUtils;
import org.eclipse.escet.cif.datasynth.conversion.CifToSynthesisConverter;
import org.eclipse.escet.cif.datasynth.conversion.SynthesisToCifConverter;
import org.eclipse.escet.cif.datasynth.options.BddAdvancedVariableOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddDcshVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddDebugMaxNodesOption;
import org.eclipse.escet.cif.datasynth.options.BddDebugMaxPathsOption;
import org.eclipse.escet.cif.datasynth.options.BddForceVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption;
import org.eclipse.escet.cif.datasynth.options.BddInitNodeTableSizeOption;
import org.eclipse.escet.cif.datasynth.options.BddOpCacheRatioOption;
import org.eclipse.escet.cif.datasynth.options.BddOpCacheSizeOption;
import org.eclipse.escet.cif.datasynth.options.BddOutputNamePrefixOption;
import org.eclipse.escet.cif.datasynth.options.BddOutputOption;
import org.eclipse.escet.cif.datasynth.options.BddSimplifyOption;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowSizeOption;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddVariableOrderOption;
import org.eclipse.escet.cif.datasynth.options.ContinuousPerformanceStatisticsFileOption;
import org.eclipse.escet.cif.datasynth.options.EdgeGranularityOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderBackwardOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderDuplicateEventsOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderForwardOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderOption;
import org.eclipse.escet.cif.datasynth.options.EdgeWorksetAlgoOption;
import org.eclipse.escet.cif.datasynth.options.EventWarnOption;
import org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption;
import org.eclipse.escet.cif.datasynth.options.ForwardReachOption;
import org.eclipse.escet.cif.datasynth.options.PlantsRefReqsWarnOption;
import org.eclipse.escet.cif.datasynth.options.StateReqInvEnforceOption;
import org.eclipse.escet.cif.datasynth.options.SupervisorNameOption;
import org.eclipse.escet.cif.datasynth.options.SupervisorNamespaceOption;
import org.eclipse.escet.cif.datasynth.options.SynthesisStatistics;
import org.eclipse.escet.cif.datasynth.options.SynthesisStatisticsOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.Application;
import org.eclipse.escet.common.app.framework.Paths;
import org.eclipse.escet.common.app.framework.io.AppStream;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.io.FileAppStream;
import org.eclipse.escet.common.app.framework.options.InputFileOption;
import org.eclipse.escet.common.app.framework.options.Option;
import org.eclipse.escet.common.app.framework.options.OptionCategory;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.options.OutputFileOption;
import org.eclipse.escet.common.app.framework.output.IOutputComponent;
import org.eclipse.escet.common.app.framework.output.OutputMode;
import org.eclipse.escet.common.app.framework.output.OutputModeOption;
import org.eclipse.escet.common.app.framework.output.OutputProvider;
import org.eclipse.escet.common.box.GridBox;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.FileSizes;

import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDFactory.CacheStats;
import com.github.javabdd.JFactory;

/** CIF data-based supervisory controller synthesis application. */
public class CifDataSynthesisApp extends Application<IOutputComponent> {
    /**
     * Application main method.
     *
     * @param args The command line arguments supplied to the application.
     */
    public static void main(String[] args) {
        CifDataSynthesisApp app = new CifDataSynthesisApp();
        app.run(args, true);
    }

    /** Constructor for the {@link CifDataSynthesisApp} class. */
    public CifDataSynthesisApp() {
        // Nothing to do here.
    }

    /**
     * Constructor for the {@link CifDataSynthesisApp} class.
     *
     * @param streams The streams to use for input, output, warning, and error streams.
     */
    public CifDataSynthesisApp(AppStreams streams) {
        super(streams);
    }

    @Override
    public String getAppName() {
        return "CIF data-based supervisory controller synthesis tool";
    }

    @Override
    public String getAppDescription() {
        return "Synthesizes a supervisory controller for a CIF specification with data.";
    }

    @Override
    protected int runInternal() {
        // Initialize timing statistics.
        Set<SynthesisStatistics> stats = SynthesisStatisticsOption.getStatistics();
        boolean doTiming = stats.contains(SynthesisStatistics.TIMING);
        CifDataSynthesisTiming timing = new CifDataSynthesisTiming();

        // Do synthesis.
        if (doTiming) {
            timing.total.start();
        }
        try {
            doSynthesis(doTiming, timing);
        } finally {
            // Print timing statistics.
            if (doTiming) {
                timing.total.stop();
                timing.print(AppEnv.getData());
            }
        }

        // All done.
        return 0;
    }

    /**
     * Perform synthesis.
     *
     * @param doTiming Whether to collect timing statistics.
     * @param timing The timing statistics data. Is modified in-place.
     */
    private void doSynthesis(boolean doTiming, CifDataSynthesisTiming timing) {
        // Read option value, to validate it early.
        String supName = SupervisorNameOption.getSupervisorName("sup");
        String supNamespace = SupervisorNamespaceOption.getNamespace();

        // Initialize debugging.
        boolean dbgEnabled = OutputModeOption.getOutputMode() == OutputMode.DEBUG;

        // Read CIF specification.
        String inputPath = InputFileOption.getPath();
        if (dbgEnabled) {
            dbg("Reading CIF file \"%s\".", inputPath);
        }

        CifReader cifReader = new CifReader().init();
        Specification spec;
        if (doTiming) {
            timing.inputRead.start();
        }
        try {
            spec = cifReader.read();
        } finally {
            if (doTiming) {
                timing.inputRead.stop();
            }
        }

        if (isTerminationRequested()) {
            return;
        }

        // Perform preprocessing.
        if (dbgEnabled) {
            dbg("Preprocessing CIF specification.");
        }

        if (doTiming) {
            timing.inputPreProcess.start();
        }
        try {
            // Remove/ignore I/O declarations, to increase the supported subset.
            RemoveIoDecls removeIoDecls = new RemoveIoDecls();
            removeIoDecls.transform(spec);
            if (removeIoDecls.haveAnySvgInputDeclarationsBeenRemoved()) {
                warn("The specification contains CIF/SVG input declarations. These will be ignored.");
            }

            // Eliminate component definition/instantiation, to avoid having to handle them.
            new ElimComponentDefInst().transform(spec);

            // Check whether plants reference requirements.
            if (PlantsRefReqsWarnOption.isEnabled()) {
                new CifDataSynthesisPlantsRefsReqsChecker().checkPlantRefToRequirement(spec);
            }
        } finally {
            if (doTiming) {
                timing.inputPreProcess.stop();
            }
        }

        if (isTerminationRequested()) {
            return;
        }

        // Create BDD factory.
        int bddTableSize = BddInitNodeTableSizeOption.getInitialSize();
        Integer bddCacheSize = BddOpCacheSizeOption.getCacheSize();
        double bddCacheRatio = BddOpCacheRatioOption.getCacheRatio();
        if (bddCacheSize == null) {
            // Initialize BDD cache size using cache ratio.
            bddCacheSize = (int)(bddTableSize * bddCacheRatio);
            if (bddCacheSize < 2) {
                bddCacheSize = 2;
            }
        } else {
            // Disable cache ratio.
            bddCacheRatio = -1;
        }

        BDDFactory factory = JFactory.init(bddTableSize, bddCacheSize);
        if (bddCacheRatio != -1) {
            factory.setCacheRatio(bddCacheRatio);
        }

        Set<SynthesisStatistics> stats = SynthesisStatisticsOption.getStatistics();
        boolean doGcStats = stats.contains(SynthesisStatistics.BDD_GC_COLLECT);
        boolean doResizeStats = stats.contains(SynthesisStatistics.BDD_GC_RESIZE);
        boolean doContinuousPerformanceStats = stats.contains(SynthesisStatistics.BDD_PERF_CONT);
        List<Long> continuousOpMisses = list();
        List<Integer> continuousUsedBddNodes = list();
        BddUtils.registerBddCallbacks(factory, doGcStats, doResizeStats, doContinuousPerformanceStats,
                continuousOpMisses, continuousUsedBddNodes);

        boolean doCacheStats = stats.contains(SynthesisStatistics.BDD_PERF_CACHE);
        boolean doMaxBddNodesStats = stats.contains(SynthesisStatistics.BDD_PERF_MAX_NODES);
        boolean doMaxMemoryStats = stats.contains(SynthesisStatistics.MAX_MEMORY);
        if (doCacheStats || doContinuousPerformanceStats) {
            factory.getCacheStats().enableMeasurements();
        }
        if (doMaxBddNodesStats) {
            factory.getMaxUsedBddNodesStats().enableMeasurements();
        }
        if (doMaxMemoryStats) {
            factory.getMaxMemoryStats().enableMeasurements();
        }

        // Perform synthesis.
        Specification rslt;
        try {
            // Convert CIF specification to synthesis format, checking for
            // precondition violations along the way.
            if (dbgEnabled) {
                dbg("Converting CIF specification to internal format.");
            }
            CifToSynthesisConverter converter1 = new CifToSynthesisConverter();

            SynthesisAutomaton aut;
            if (doTiming) {
                timing.inputConvert.start();
            }
            try {
                aut = converter1.convert(spec, factory, dbgEnabled);
            } finally {
                if (doTiming) {
                    timing.inputConvert.stop();
                }
            }

            if (isTerminationRequested()) {
                return;
            }

            // Perform synthesis.
            if (dbgEnabled) {
                dbg("Starting data-based synthesis.");
            }
            boolean doPrintCtrlSysStates = stats.contains(SynthesisStatistics.CTRL_SYS_STATES);
            CifDataSynthesis.synthesize(aut, dbgEnabled, doTiming, timing, doPrintCtrlSysStates);
            if (isTerminationRequested()) {
                return;
            }

            // Construct output CIF specification.
            if (dbgEnabled) {
                dbg("Constructing output CIF specification.");
            }
            SynthesisToCifConverter converter2 = new SynthesisToCifConverter();

            if (doTiming) {
                timing.outputConvert.start();
            }
            try {
                rslt = converter2.convert(aut, spec, supName, supNamespace);
            } finally {
                if (doTiming) {
                    timing.outputConvert.stop();
                }
            }

            if (isTerminationRequested()) {
                return;
            }

            // Print statistics before we clean up the factory.
            if (doCacheStats) {
                printBddCacheStats(factory.getCacheStats());
            }
            if (doContinuousPerformanceStats) {
                printBddContinuousPerformanceStats(continuousOpMisses, continuousUsedBddNodes);
            }
            if (doMaxBddNodesStats) {
                out(fmt("Maximum used BDD nodes: %d.", factory.getMaxUsedBddNodesStats().getMaxUsedBddNodes()));
            }
            if (doMaxMemoryStats) {
                long maxMemoryBytes = factory.getMaxMemoryStats().getMaxMemoryBytes();
                out(fmt("Maximum used memory: %d bytes = %s.", maxMemoryBytes,
                        FileSizes.formatFileSize(maxMemoryBytes, false)));
            }

            if (isTerminationRequested()) {
                return;
            }
        } finally {
            // Always clean up the BDD factory.
            factory.done();
        }

        // Write output CIF specification.
        String outPath = OutputFileOption.getDerivedPath(".cif", ".ctrlsys.cif");
        if (dbgEnabled) {
            dbg("Writing output CIF file \"%s\".", outPath);
        }
        outPath = Paths.resolve(outPath);

        if (doTiming) {
            timing.outputWrite.start();
        }
        try {
            CifWriter.writeCifSpec(rslt, outPath, cifReader.getAbsDirPath());
        } finally {
            if (doTiming) {
                timing.outputWrite.stop();
            }
        }

        if (isTerminationRequested()) {
            return;
        }
    }

    /**
     * Print the BDD factory cache statistics.
     *
     * @param stats The BDD factory cache statistics.
     */
    private void printBddCacheStats(CacheStats stats) {
        // Create grid.
        GridBox grid = new GridBox(7, 2, 0, 1);

        grid.set(0, 0, "Node creation requests:");
        grid.set(1, 0, "Node creation chain accesses:");
        grid.set(2, 0, "Node creation cache hits:");
        grid.set(3, 0, "Node creation cache misses:");
        grid.set(4, 0, "Operation count:");
        grid.set(5, 0, "Operation cache hits:");
        grid.set(6, 0, "Operation cache misses:");

        grid.set(0, 1, str(stats.uniqueAccess));
        grid.set(1, 1, str(stats.uniqueChain));
        grid.set(2, 1, str(stats.uniqueHit));
        grid.set(3, 1, str(stats.uniqueMiss));
        grid.set(4, 1, str(stats.opAccess));
        grid.set(5, 1, str(stats.opHit));
        grid.set(6, 1, str(stats.opMiss));

        // Print statistics.
        out("BDD cache statistics:");
        for (String line: grid.getLines()) {
            out("  " + line);
        }
    }

    /**
     * Print the continuous BDD performance statistics to a file.
     *
     * @param operationsSamples The collected continuous operation misses samples.
     * @param nodesSamples The collected continuous used BDD nodes statistics samples.
     */
    private void printBddContinuousPerformanceStats(List<Long> operationsSamples, List<Integer> nodesSamples) {
        // Get number of data points.
        Assert.areEqual(operationsSamples.size(), nodesSamples.size());
        int numberOfDataPoints = operationsSamples.size();

        // Get the file to print to.
        String outPath = ContinuousPerformanceStatisticsFileOption.getPath();
        dbg("Writing continuous BDD performance statistics file \"%s\".", outPath);
        String absOutPath = Paths.resolve(outPath);

        // Start the actual printing.
        try (AppStream stream = new FileAppStream(outPath, absOutPath)) {
            stream.println("Operations,Used BBD nodes");
            long lastOperations = -1;
            int lastNodes = -1;
            for (int i = 0; i < numberOfDataPoints; i++) {
                // Only print new data points.
                long nextOperations = operationsSamples.get(i);
                int nextNodes = nodesSamples.get(i);
                if (nextOperations != lastOperations || nextNodes != lastNodes) {
                    lastOperations = nextOperations;
                    lastNodes = nextNodes;
                    stream.printfln("%d,%d", lastOperations, lastNodes);
                }
            }
        }
    }

    @Override
    protected OutputProvider<IOutputComponent> getProvider() {
        return new OutputProvider<>();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected OptionCategory getAllOptions() {
        OptionCategory generalCat = getGeneralOptionCategory();

        List<Option> bddOpts = list();
        bddOpts.add(Options.getInstance(BddOutputOption.class));
        bddOpts.add(Options.getInstance(BddOutputNamePrefixOption.class));
        bddOpts.add(Options.getInstance(BddVariableOrderOption.class));
        bddOpts.add(Options.getInstance(BddHyperEdgeAlgoOption.class));
        bddOpts.add(Options.getInstance(BddDcshVarOrderOption.class));
        bddOpts.add(Options.getInstance(BddForceVarOrderOption.class));
        bddOpts.add(Options.getInstance(BddSlidingWindowVarOrderOption.class));
        bddOpts.add(Options.getInstance(BddSlidingWindowSizeOption.class));
        bddOpts.add(Options.getInstance(BddAdvancedVariableOrderOption.class));
        bddOpts.add(Options.getInstance(BddSimplifyOption.class));
        bddOpts.add(Options.getInstance(BddInitNodeTableSizeOption.class));
        bddOpts.add(Options.getInstance(BddOpCacheSizeOption.class));
        bddOpts.add(Options.getInstance(BddOpCacheRatioOption.class));
        bddOpts.add(Options.getInstance(BddDebugMaxNodesOption.class));
        bddOpts.add(Options.getInstance(BddDebugMaxPathsOption.class));
        OptionCategory bddCat = new OptionCategory("BDD", "BDD options.", list(), bddOpts);

        List<Option> synthOpts = list();
        synthOpts.add(Options.getInstance(InputFileOption.class));
        synthOpts.add(Options.getInstance(OutputFileOption.class));
        synthOpts.add(Options.getInstance(SupervisorNameOption.class));
        synthOpts.add(Options.getInstance(SupervisorNamespaceOption.class));
        synthOpts.add(Options.getInstance(ForwardReachOption.class));
        synthOpts.add(Options.getInstance(FixedPointComputationsOrderOption.class));
        synthOpts.add(Options.getInstance(EdgeGranularityOption.class));
        synthOpts.add(Options.getInstance(EdgeOrderOption.class)); // No longer supported.
        synthOpts.add(Options.getInstance(EdgeOrderBackwardOption.class));
        synthOpts.add(Options.getInstance(EdgeOrderForwardOption.class));
        synthOpts.add(Options.getInstance(EdgeOrderDuplicateEventsOption.class));
        synthOpts.add(Options.getInstance(EdgeWorksetAlgoOption.class));
        synthOpts.add(Options.getInstance(StateReqInvEnforceOption.class));
        synthOpts.add(Options.getInstance(SynthesisStatisticsOption.class));
        synthOpts.add(Options.getInstance(ContinuousPerformanceStatisticsFileOption.class));
        synthOpts.add(Options.getInstance(EventWarnOption.class));
        synthOpts.add(Options.getInstance(PlantsRefReqsWarnOption.class));
        OptionCategory synthCat = new OptionCategory("Synthesis", "Synthesis options.", list(bddCat), synthOpts);

        List<OptionCategory> cats = list(generalCat, synthCat);
        OptionCategory options = new OptionCategory("CIF Data-based Synthesis Options",
                "All options for the CIF data-based supervisory controller synthesis tool.", cats, list());

        return options;
    }
}
