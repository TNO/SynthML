////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.app.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONException;

import com.github.tno.pokayoke.transform.app.Uml2GalTranslationApp;

import jakarta.inject.Named;

/** Menu action handler for translating UML models to GAL specifications. */
public class Uml2GalTranslationHandler {
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
        IResource inputResource = (IResource)selection.getFirstElement();
        Path inputPath = Paths.get(inputResource.getLocationURI());
        Path outputPath = inputPath.resolveSibling("output");
        Job job = Job.create("Translating UML to GAL", monitor -> {
            try {
                Uml2GalTranslationApp.translateUml2Gal(inputPath, outputPath);

                inputResource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

                return Status.OK_STATUS;
            } catch (IOException | JSONException | CoreException e) {
                return new Status(IStatus.ERROR, getClass().getPackageName(),
                        "Failed to translate to GAL: " + inputPath, e);
            }
        });
        job.setUser(true);
        job.schedule();
    }
}
