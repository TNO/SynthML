
package com.github.tno.pokayoke.uml.profile.design;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.emf.core.util.EMFCoreUtil;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.diagram.business.api.query.EObjectQuery;
import org.eclipse.sirius.diagram.ui.business.api.view.SiriusGMFHelper;
import org.eclipse.sirius.diagram.ui.internal.providers.SiriusMarkerNavigationProvider;
import org.eclipse.sirius.diagram.ui.tools.internal.marker.SiriusMarkerNavigationProviderSpec;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.sirius.viewpoint.DSemanticDecorator;
import org.eclipse.sirius.viewpoint.DView;
import org.eclipse.sirius.viewpoint.ViewpointPackage;

/**
 * A utility class to validate all diagrams in a Sirius session.
 *
 * @see org.eclipse.sirius.diagram.ui.part.ValidateAction
 */
@SuppressWarnings("restriction")
public class SessionValidator {
    private SessionValidator() {
        // Empty for utility classes
    }

    public static void validateAllDiagrams(Session session) {
        IFile sessionFile = WorkspaceSynchronizer.getFile(session.getSessionResource());
        SiriusMarkerNavigationProvider.deleteMarkers(sessionFile);

        for (DRepresentationDescriptor representation: QueryableIterable.from(session.getOwnedViews())
                .collect(DView::getOwnedRepresentationDescriptors))
        {
            validateDiagram(representation, session);
        }
    }

    private static void validateDiagram(DRepresentationDescriptor representationDesc, Session session) {
        IFile sessionFile = WorkspaceSynchronizer.getFile(session.getSessionResource());
        String diagramDescriptorURI = EcoreUtil.getURI(representationDesc).toString();

        // Validating root element of diagram
        Diagnostician diagnostician = new Diagnostician() {
            @Override
            public String getObjectLabel(EObject eObject) {
                return EMFCoreUtil.getQualifiedName(eObject, true);
            }
        };
        Diagnostic validationResult = diagnostician.validate(representationDesc.getTarget());

        // Reporting validation messages
        for (Diagnostic diagnostic: validationResult.getChildren()) {
            if (diagnostic.getSeverity() >= Diagnostic.INFO && !diagnostic.getData().isEmpty()
                    && diagnostic.getData().get(0) instanceof EObject element)
            {
                DRepresentation representation = representationDesc.getRepresentation();
                Collection<EObject> xrefs = new EObjectQuery(element)
                        .getInverseReferences(ViewpointPackage.Literals.DSEMANTIC_DECORATOR__TARGET);
                DSemanticDecorator dSemanticDecorator = QueryableIterable.from(xrefs)
                        .objectsOfKind(DSemanticDecorator.class).any(r -> EcoreUtil.isAncestor(representation, r));
                if (dSemanticDecorator == null) {
                    continue;
                }
                View view = SiriusGMFHelper.getGmfView(dSemanticDecorator, session);
                if (view == null || view.eResource() == null) {
                    continue;
                }
                String elementId = view.eResource().getURIFragment(view);
                String semanticURI = EcoreUtil.getURI(element).toString();
                String location = EMFCoreUtil.getQualifiedName(element, true);

                SiriusMarkerNavigationProviderSpec.addMarker(sessionFile, elementId, diagramDescriptorURI, semanticURI,
                        location, diagnostic.getMessage(), diagnostic.getSeverity());
            }
        }
    }
}
