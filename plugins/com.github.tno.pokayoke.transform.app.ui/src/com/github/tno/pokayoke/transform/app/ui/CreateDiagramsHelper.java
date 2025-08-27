
package com.github.tno.pokayoke.transform.app.ui;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.LinkedList;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.sirius.business.api.dialect.DialectManager;
import org.eclipse.sirius.business.api.dialect.command.CreateRepresentationCommand;
import org.eclipse.sirius.business.api.modelingproject.ModelingProject;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.business.internal.session.SessionTransientAttachment;
import org.eclipse.sirius.common.tools.api.interpreter.EvaluationException;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreter;
import org.eclipse.sirius.tools.api.command.semantic.AddSemanticResourceCommand;
import org.eclipse.sirius.ui.business.api.session.UserSession;
import org.eclipse.sirius.viewpoint.description.RepresentationDescription;
import org.eclipse.swt.widgets.Display;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;

import com.github.tno.pokayoke.transform.common.FileHelper;

@SuppressWarnings("restriction")
public class CreateDiagramsHelper {
    private CreateDiagramsHelper() {
        // Empty for utilities
    }

    public static IStatus createUmlDiagrams(Model model, IProgressMonitor monitor) {
        try {
            Resource modelResource = model.eResource();
            IFile modelFile = WorkspaceSynchronizer.getFile(modelResource);
            IProject modelProject = modelFile.getProject();
            IFile representationsFile = modelProject.getFile(ModelingProject.DEFAULT_REPRESENTATIONS_FILE_NAME);
            if (!representationsFile.exists()) {
                return Status.OK_STATUS;
            }

            LinkedList<Activity> activities = from(model).collect(Model::getOwnedElements).objectsOfKind(Class.class)
                    .collect(Class::getOwnedBehaviors).objectsOfKind(Activity.class).asList();

            SubMonitor subMonitor = SubMonitor.convert(monitor, 6 + activities.size());

            Session session = SessionManager.INSTANCE.getSession(FileHelper.asURI(representationsFile),
                    subMonitor.split(1));

            TransactionalEditingDomain transDomain = session.getTransactionalEditingDomain();
            transDomain.getCommandStack()
                    .execute(new AddSemanticResourceCommand(session, modelResource.getURI(), subMonitor.split(1)));

            // Setting viewpoint
            UserSession userSession = UserSession.from(session);
            Display.getDefault().syncExec(() -> userSession.selectViewpoint("A_SynthML"));
            subMonitor.worked(1);

            // Creating diagrams
            CompoundCommand command = new CompoundCommand();
            command.append(createDiagram(session, model, "Class Diagram", subMonitor.split(1)));
            for (Activity activity: activities) {
                command.append(createDiagram(session, activity, "Activity Diagram", subMonitor.split(1)));
            }
            transDomain.getCommandStack().execute(command);

            modelProject.close(subMonitor.split(1));
            modelProject.open(subMonitor.split(1));

            return Status.OK_STATUS;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, CreateScopedModelHandler.class,
                    "Failed to create UML diagrams: " + e.getLocalizedMessage(), e);
        }
    }

    private static CreateRepresentationCommand createDiagram(Session session, EObject eObject, String diagramName,
            IProgressMonitor monitor) throws EvaluationException
    {
        // Otherwise Sirius is not able to find the session from the model
        eObject.eAdapters().add(new SessionTransientAttachment(session));

        RepresentationDescription representationDescription = from(DialectManager.INSTANCE
                .getAvailableRepresentationDescriptions(session.getSelectedViewpoints(false), eObject))
                        .any(rd -> Objects.equals(diagramName, rd.getName()));

        if (representationDescription == null) {
            throw new RuntimeException(
                    String.format("Diagram %s not found for type %s", diagramName, eObject.eClass().getName()));
        }

        IInterpreter interpreter = session.getInterpreter();
        String diagramTitle = interpreter.evaluateString(eObject, representationDescription.getTitleExpression());
        CreateRepresentationCommand command = new CreateRepresentationCommand(session, representationDescription,
                eObject, diagramTitle, monitor);

        return command;
    }
}
