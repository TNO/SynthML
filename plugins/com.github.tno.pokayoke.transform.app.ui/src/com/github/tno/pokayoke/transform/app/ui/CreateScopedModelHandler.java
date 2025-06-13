
package com.github.tno.pokayoke.transform.app.ui;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.inject.Named;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Evaluate;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.lsat.common.util.IterableUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;

public class CreateScopedModelHandler {
    @Evaluate
    @CanExecute
    public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection) {
        return !selection.isEmpty()
                && QueryableIterable.from((Iterable<?>)selection).forAll(Activity.class::isInstance);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection,
            @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        QueryableIterable<Activity> activities = QueryableIterable.from((Iterable<?>)selection)
                .objectsOfKind(Activity.class);

        Activity firstActivity = activities.first();
        IFile inputFile = WorkspaceSynchronizer.getFile(firstActivity.eResource());
        IFile suggestFile = inputFile.getParent().getFile(new Path(firstActivity.getName() + ".uml"));

        MySaveAsDialog saveAsDialog = new MySaveAsDialog(shell);
        saveAsDialog.setOriginalFile(suggestFile);
        if (saveAsDialog.open() != Window.OK) {
            return;
        }
        IPath savePath = saveAsDialog.getResult();
        if (savePath.getFileExtension() == null) {
            savePath = savePath.addFileExtension("uml");
        }
        IFile saveFile = ResourcesPlugin.getWorkspace().getRoot().getFile(savePath);

        if (saveAsDialog.includeCalledActivities) {
            // Finding all called activities and sorting them by name
            activities = activities.union(
                    activities.closure(CreateScopedModelHandler::getCalledActivities).sortedBy(Activity::getName));
        }

        // Note: a local variable can only be referenced from a closure when its final, hence this extra declaration.
        final QueryableIterable<Activity> finalActivities = activities;

        Job job = Job.create("Create scoped model", monitor -> {
            try {
                Model scope = createScope(finalActivities.asOrderedSet());
                Package commonPackage = getCommonPackage(finalActivities);
                if (commonPackage != null) {
                    scope.setName(commonPackage.getName());
                } else {
                    scope.setName(saveFile.getFullPath().removeFileExtension().lastSegment());
                }
                return storeScope(scope, saveFile, monitor);
            } catch (Throwable e) {
                return new Status(IStatus.ERROR, CreateScopedModelHandler.class,
                        "Failed to create scoped model: " + e.getLocalizedMessage(), e);
            }
        });

        job.setUser(true);
        job.schedule();
    }

    private static IStatus storeScope(Model scope, IFile saveFile, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
        try {
            URI saveUri = FileHelper.asURI(saveFile);
            FileHelper.storeModel(scope, saveUri);
            subMonitor.worked(9);

            IProject umlProject = saveFile.getProject();
            umlProject.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.split(1));

            return CreateDiagramsHelper.createUmlDiagrams(scope, subMonitor.split(90));
        } catch (Exception e) {
            return new Status(IStatus.ERROR, CreateScopedModelHandler.class,
                    "Failed to create scoped model: " + e.getLocalizedMessage(), e);
        }
    }

    private static Iterable<Activity> getCalledActivities(Activity activity) {
        return QueryableIterable.from(activity.getNodes()).objectsOfKind(CallBehaviorAction.class)
                .xcollectOne(CallBehaviorAction::getBehavior).objectsOfKind(Activity.class);
    }

    private static Model createScope(LinkedHashSet<Activity> activities) {
        Model model = UMLFactory.eINSTANCE.createModel();
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(model);

        Class context = UMLFactory.eINSTANCE.createClass();
        context.setName("Context");
        context.setIsActive(true);
        model.getPackagedElements().add(context);

        LinkedHashSet<Activity> activitiesCopy = new LinkedHashSet<>(EcoreUtil.copyAll(activities));

        // Transform all CallBehaviorActions that point to other activities into OpaqueActions
        LinkedList<CallBehaviorAction> cbActionsToTransform = QueryableIterable.from(activitiesCopy)
                .collect(Activity::getNodes).objectsOfKind(CallBehaviorAction.class)
                .reject(a -> activitiesCopy.contains(a.getBehavior())).asList();
        for (CallBehaviorAction cbAction: cbActionsToTransform) {
            if (cbAction.getBehavior() instanceof Activity) {
                OpaqueAction oAction = UMLFactory.eINSTANCE.createOpaqueAction();
                oAction.setName(cbAction.getName());
                cbAction.getActivity().getOwnedNodes().add(oAction);
                // Update the incoming and outgoing edges.
                cbAction.getIncomings().get(0).setTarget(oAction);
                cbAction.getOutgoings().get(0).setSource(oAction);
                cbAction.destroy();
            } else if (cbAction.getBehavior() instanceof OpaqueBehavior) {
                throw new RuntimeException("Call opaque behavior actions are currently unsupported.");
            }
        }

        context.getOwnedBehaviors().addAll(activitiesCopy);
        context.setClassifierBehavior(IterableUtil.first(activitiesCopy));

        return model;
    }

    private static Package getCommonPackage(Iterable<Activity> activities) {
        Iterator<Activity> iterator = activities.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        LinkedHashSet<Package> packages = new LinkedHashSet<>(iterator.next().allOwningPackages());
        while (iterator.hasNext()) {
            packages.retainAll(iterator.next().allOwningPackages());
        }
        return IterableUtil.first(packages);
    }

    private static class MySaveAsDialog extends SaveAsDialog {
        boolean includeCalledActivities = true;

        public MySaveAsDialog(Shell shell) {
            super(shell);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite parentComposite = (Composite)super.createDialogArea(parent);
            Font font = parentComposite.getFont();

            Composite checkboxGroup = new Composite(parentComposite, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginLeft = 5;
            checkboxGroup.setLayout(layout);
            checkboxGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
            checkboxGroup.setFont(font);

            Button checkBox1 = new Button(checkboxGroup, SWT.CHECK);
            checkBox1.setFont(font);
            checkBox1.setText("Include all called activities");
            checkBox1.setSelection(includeCalledActivities);
            checkBox1.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    includeCalledActivities = checkBox1.getSelection();
                }
            });

            return parentComposite;
        }
    }
}
