
package com.github.tno.pokayoke.transform.app.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Named;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.github.tno.pokayoke.transform.app.Uml2CameoTranslationApp;

/** Menu action handler for translating UML models to fUML that can be simulated with the Cameo Systems Modeler. */
public class Uml2CameoTranslationHandler {
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
        IResource inputResource = (IResource)selection.getFirstElement();
        Path inputPath = Paths.get(inputResource.getLocationURI());
        Path outputPath = inputPath.resolveSibling("output");
        Job job = Job.create("Translating UML to Cameo", monitor -> {
            try {
                Uml2CameoTranslationApp.translateUml2Cameo(inputPath, outputPath);

                inputResource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

                return Status.OK_STATUS;
            } catch (IOException | CoreException e) {
                return new Status(IStatus.ERROR, getClass().getPackageName(),
                        "Failed to translate to Cameo: " + inputPath, e);
            }
        });
        job.setUser(true);
        job.schedule();
    }
}
