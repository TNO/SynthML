
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

import com.github.tno.pokayoke.uml.profile.transform.ApplyPokaYokeUmlProfile;

/** Menu action handler for translating UML models to GAL specifications. */
public class ApplyPokaYokeUmlProfileHandler {
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
        IResource inputResource = (IResource)selection.getFirstElement();
        Path inputPath = Paths.get(inputResource.getLocationURI());
        Path outputPath;
        if (!inputResource.getName().endsWith("_py.uml")) {
            String outputName = inputResource.getName().replaceFirst("\\.uml$", "_py.uml");
            outputPath = inputPath.resolveSibling(outputName);
        } else {
            outputPath = inputPath;
        }
        Job job = Job.create("Applying Poka Yoke UML profile", monitor -> {
            try {
                ApplyPokaYokeUmlProfile.applyUmlProfile(inputPath.toString(), outputPath.toString());

                inputResource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

                return Status.OK_STATUS;
            } catch (IOException | CoreException e) {
                return new Status(IStatus.ERROR, getClass().getPackageName(),
                        "Failed to apply Poka Yoke UML profile: " + inputPath, e);
            }
        });
        job.setUser(true);
        job.schedule();
    }
}
