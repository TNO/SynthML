
package com.github.tno.pokayoke.transform.app.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.github.tno.pokayoke.transform.app.FullSynthesisApp;

/** Menu action handler to perform full synthesis. */
public class FullSynthesisHandler {
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection,
            @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        IResource inputResource = (IResource)selection.getFirstElement();
        Path inputPath = Paths.get(inputResource.getLocationURI());
        Path outputPath = inputPath.resolveSibling("output");
        Job job = Job.create("Performing full synthesis", monitor -> {
            try {
                // Perform activity synthesis.
                List<String> warnings = new ArrayList<>();
                FullSynthesisApp.performFullSynthesis(inputPath, outputPath, warnings);

                // In case warnings came up during synthesis, show a warning dialog to notify the user.
                if (!warnings.isEmpty()) {
                    MessageDialog dialog = new WarningDialog(shell, "Warnings",
                            "Activity synthesis resulted in warnings:", warnings);
                    Display.getDefault().asyncExec(dialog::open);
                }

                inputResource.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

                return Status.OK_STATUS;
            } catch (IOException | CoreException e) {
                return new Status(IStatus.ERROR, getClass().getPackageName(),
                        "Failed to perform full synthesis: " + inputPath, e);
            }
        });
        job.setUser(true);
        job.schedule();
    }

    private class WarningDialog extends MessageDialog {
        private final List<String> warnings;

        private WarningDialog(Shell parentShell, String title, String message, List<String> warnings) {
            super(parentShell, title, null, message, MessageDialog.WARNING, 0, IDialogConstants.OK_LABEL);

            this.warnings = warnings;

            int shellStyle = SWT.DIALOG_TRIM | getDefaultOrientation();

            if (isResizable()) {
                shellStyle |= SWT.MAX | SWT.RESIZE;
            }

            shellStyle |= SWT.APPLICATION_MODAL;
            setBlockOnOpen(true);
            setShellStyle(shellStyle);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            ListViewer viewer = new ListViewer(parent);

            for (String warning: warnings) {
                viewer.add(warning);
            }

            Control control = viewer.getControl();
            control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            return control;
        }
    }
}
