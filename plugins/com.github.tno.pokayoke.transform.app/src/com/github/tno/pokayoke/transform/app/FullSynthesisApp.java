
package com.github.tno.pokayoke.transform.app;

import static org.eclipse.escet.common.java.Lists.list;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.CifDataSynthesis;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisResult;
import org.eclipse.escet.cif.datasynth.CifDataSynthesisTiming;
import org.eclipse.escet.cif.datasynth.conversion.SynthesisToCifConverter;
import org.eclipse.escet.cif.datasynth.settings.BddSimplify;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.eventbased.apps.DfaMinimizationApplication;
import org.eclipse.escet.cif.eventbased.apps.ProjectionApplication;
import org.eclipse.escet.cif.explorer.app.ExplorerApplication;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.app.framework.AppEnv;

import com.github.javabdd.BDDFactory;
import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.cif2petrify.FileHelper;
import com.github.tno.pokayoke.transform.petrify2uml.PetriNet2Activity;
import com.google.common.base.Verify;

/** Application that performs full synthesis. */
public class FullSynthesisApp {
    private FullSynthesisApp() {
    }

    public static void performFullSynthesis(Path inputPath, Path outputFolderPath) throws IOException {
        Files.createDirectories(outputFolderPath);
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());

        // Load CIF specification.
        Specification cifSpec = FileHelper.loadCifSpec(inputPath);

        // Perform Synthesis.
        Path cifSynthesisPath = outputFolderPath.resolve(filePrefix + ".ctrlsys.cif");
        CifDataSynthesisResult cifSynthesisResult = synthesize(cifSpec);

        // Convert synthesis result back to CIF.
        convertSynthesisResultToCif(cifSpec, cifSynthesisResult, cifSynthesisPath.toString(),
                outputFolderPath.toString());

        // TODO Extract action guards from specification and synthesized guards from the synthesis result.

        // Perform state space generation.
        Path cifStateSpacePath = outputFolderPath.resolve(filePrefix + ".ctrlsys.statespace.cif");
        String[] stateSpaceGenerationArgs = new String[] {cifSynthesisPath.toString(),
                "--output=" + cifStateSpacePath.toString()};
        ExplorerApplication explorerApp = new ExplorerApplication();
        explorerApp.run(stateSpaceGenerationArgs, false);

        // Perform event-based automaton projection.
        Specification cifStateSpace = FileHelper.loadCifSpec(cifStateSpacePath);
        String preservedEvents = getPreservedEvents(cifStateSpace);
        Path cifProjectedStateSpacePath = outputFolderPath.resolve(filePrefix + ".ctrlsys.statespace.projected.cif");
        String[] projectionArgs = new String[] {cifStateSpacePath.toString(), "--preserve=" + preservedEvents,
                "--output=" + cifProjectedStateSpacePath.toString()};
        ProjectionApplication projectionApp = new ProjectionApplication();
        projectionApp.run(projectionArgs, false);

        // Perform DFA minimization.
        Path cifMinimizedStateSpacePath = outputFolderPath
                .resolve(filePrefix + ".ctrlsys.statespace.projected.minimized.cif");
        String[] dfaMinimizationArgs = new String[] {cifProjectedStateSpacePath.toString(),
                "--output=" + cifMinimizedStateSpacePath.toString()};
        DfaMinimizationApplication dfaMinimizationApp = new DfaMinimizationApplication();
        dfaMinimizationApp.run(dfaMinimizationArgs, false);

        // Translate the CIF state space to Petrify input and output the Petrify input.
        Path petrifyInputPath = outputFolderPath.resolve(filePrefix + ".g");
        Cif2Petrify.transformFile(cifMinimizedStateSpacePath.toString(), petrifyInputPath.toString());

        // Petrify the state space and output the generated Petri Net.
        Path petrifyOutputPath = outputFolderPath.resolve(filePrefix + ".out");
        Path petrifyLogPath = outputFolderPath.resolve("petrify.log");
        convertToPetriNet(petrifyInputPath, petrifyOutputPath, petrifyLogPath, 20);

        // Translate Petri Net to UML Activity and output the activity.
        Path umlOutputPath = outputFolderPath.resolve(filePrefix + ".uml");
        PetriNet2Activity.transformFile(petrifyOutputPath.toString(), umlOutputPath.toString());
    }

    private static CifDataSynthesisResult synthesize(Specification spec) {
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();
        settings.setDoForwardReach(true);
        settings.setBddSimplifications(EnumSet.noneOf(BddSimplify.class));

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

        // Perform synthesis.
        CifDataSynthesisResult synthResult = CifDataSynthesis.synthesize(cifBddSpec, settings,
                new CifDataSynthesisTiming());

        return synthResult;
    }

    private static Specification convertSynthesisResultToCif(Specification spec, CifDataSynthesisResult synthResult,
            String outPutFilePath, String outFolderPath)
    {
        Specification rslt;

        // Construct output CIF specification.
        SynthesisToCifConverter converter = new SynthesisToCifConverter();
        rslt = converter.convert(synthResult, spec);

        // Write output CIF specification.
        try {
            AppEnv.registerSimple();
            CifWriter.writeCifSpec(rslt, outPutFilePath, outFolderPath);
        } finally {
            AppEnv.unregisterApplication();
        }
        return rslt;
    }

    private static String getPreservedEvents(Specification spec) {
        List<Event> events = new ArrayList<>();
        CifCollectUtils.collectEvents(spec, events);
        List<String> eventNames = events.stream().filter(event -> event.getControllable())
                .map(event -> CifTextUtils.getAbsName(event, false)).toList();

        return String.join(",", eventNames);
    }

    /**
     * Convert CIF state space to Petri Net using Petrify.
     *
     * @param petrifyInputPath The path of the Petrify input file.
     * @param petrifyOutputPath The path of the Petrify output file.
     * @param petrifyLogPath The path of the Petrify log file.
     * @param timeoutInSeconds The timeout for the conversion process.
     */
    private static void convertToPetriNet(Path petrifyInputPath, Path petrifyOutputPath, Path petrifyLogPath,
            int timeoutInSeconds)
    {
        // Construct the command for Petrify.
        List<String> command = new ArrayList<>();

        command.add(ExecutableHelper.getExecutable("petrify", "com.github.tno.pokayoke.transform.distribution", "bin"));
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyInputPath.toString()));
        command.add("-o");
        command.add(WindowsLongPathSupport.ensureLongPathPrefix(petrifyOutputPath.toString()));

        // When this option is used, Petrify tries to produce the best possible result.
        command.add("-opt");

        // Produce a choice free Petri net. By being choice free, the Petri Net becomes easier to translate to an
        // activity.
        command.add("-fc");

        // Produce Petri Net with intermediate places. If this option is not used, implied places are described as
        // transition-transition arcs.
        command.add("-ip");

        // Generate a log file.
        command.add("-log");
        command.add(petrifyLogPath.toString());

        ProcessBuilder petrifyProcessBuilder = new ProcessBuilder(command);

        // Start the process for Petrify.
        Process petrifyProcess;

        try {
            petrifyProcess = petrifyProcessBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start the Petrify process.", e);
        }

        // Wait for the process to finish within the given timeout period.
        boolean petrifyProcessCompleted;

        try {
            petrifyProcessCompleted = petrifyProcess.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            petrifyProcess.destroyForcibly();
            throw new RuntimeException("Interrupted while waiting for Petrify process to finish.", e);
        }

        // Check whether the process timed out.
        if (!petrifyProcessCompleted) {
            petrifyProcess.destroyForcibly();
            throw new RuntimeException("Petrify process timed out.");
        }

        Verify.verify(petrifyProcess.exitValue() == 0,
                "Petrify process exited with non-zero exit code (" + petrifyProcess.exitValue() + ").");
    }
}
