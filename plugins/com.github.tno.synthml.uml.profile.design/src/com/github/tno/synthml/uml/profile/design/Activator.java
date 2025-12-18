////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.design;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionListener;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.business.api.session.SessionManagerListener;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {
    /** The plug-in ID. */
    public static final String PLUGIN_ID = "com.github.tno.synthml.uml.profile.design";

    /** The shared instance. */
    private static Activator plugin;

    private static Set<Viewpoint> viewpoints;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework. BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        viewpoints = new HashSet<>();
        viewpoints
                .addAll(ViewpointRegistry.getInstance().registerFromPlugin(PLUGIN_ID + "/description/synthml.odesign"));

        // Automatically validate all diagrams on load and save
        SessionManager.INSTANCE.addSessionsListener(new SessionManagerListener.Stub() {
            @Override
            public void notify(Session updated, int notification) {
                if (notification == SessionListener.OPENED || notification == SessionListener.SYNC) {
                    Display.getDefault().asyncExec(() -> validateDiagramsInUIThread(updated));
                }
            }
        });
    }

    private void validateDiagramsInUIThread(Session session) {
        try {
            WorkspaceModifyDelegatingOperation operation = new WorkspaceModifyDelegatingOperation(
                    m -> validateDiagramsInWSOperation(session));
            operation.run(new NullProgressMonitor());
        } catch (Exception e) {
            getLog().error("Failed to validate diagrams: " + e.getLocalizedMessage(), e);
        }
    }

    private void validateDiagramsInWSOperation(Session session) {
        TransactionalEditingDomain txDomain = session.getTransactionalEditingDomain();
        if (txDomain == null) {
            SessionValidator.validateAllDiagrams(session);
        } else {
            try {
                txDomain.runExclusive(() -> SessionValidator.validateAllDiagrams(session));
            } catch (Exception e) {
                getLog().error("Failed to validate diagrams: " + e.getLocalizedMessage(), e);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        if (viewpoints != null) {
            for (final Viewpoint viewpoint: viewpoints) {
                ViewpointRegistry.getInstance().disposeFromPlugin(viewpoint);
            }
            viewpoints.clear();
            viewpoints = null;
        }
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance.
     */
    public static Activator getDefault() {
        return plugin;
    }
}
