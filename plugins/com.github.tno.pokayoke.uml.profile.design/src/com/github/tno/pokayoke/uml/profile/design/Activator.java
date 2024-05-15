
package com.github.tno.pokayoke.uml.profile.design;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionListener;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.business.api.session.SessionManagerListener;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {
    /** The plug-in ID. */
    public static final String PLUGIN_ID = "com.github.tno.pokayoke.uml.profile.design";

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
        viewpoints.addAll(
                ViewpointRegistry.getInstance().registerFromPlugin(PLUGIN_ID + "/description/pokayoke.odesign"));

        // Automatically validate all diagrams on load and save
        SessionManager.INSTANCE.addSessionsListener(new SessionManagerListener.Stub() {
            @Override
            public void notify(Session updated, int notification) {
                if (notification == SessionListener.OPENED || notification == SessionListener.SYNC) {
                    TransactionalEditingDomain txDomain = updated.getTransactionalEditingDomain();
                    if (txDomain != null) {
                        try {
                            txDomain.runExclusive(() -> SessionValidator.validateAllDiagrams(updated));
                        } catch (InterruptedException e) {
                            getLog().error("Failed to validate diagrams.", e);
                        }
                    } else {
                        SessionValidator.validateAllDiagrams(updated);
                    }
                }
            }
        });
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
