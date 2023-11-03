/**
 *
 */

package com.github.tno.pokayoke.transform.app;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.google.common.base.Preconditions;

/** Helper methods for executables. */
public class ExecutableHelper {
    private ExecutableHelper() {
    }

    /**
     * Get absolute path to executable, to be found in a certain plugin, in a certain folder.
     *
     * @param executableName The executable name.
     * @param pluginName The plugin name.
     * @param folderName The folder name.
     * @return The absolute path, or {@code null} if not available.
     */
    public static String getExecutable(String executableName, String pluginName, String folderName) {
        if (Platform.isRunning() && SystemUtils.IS_OS_WINDOWS) {
            Bundle bundle = Platform.getBundle(pluginName);
            Preconditions.checkNotNull(bundle, pluginName + " bundle/plugin could not be found.");

            Path bundledCifPath = new Path(folderName + "/" + executableName + ".exe");

            try {
                URL fileURL = FileLocator.find(bundle, bundledCifPath);
                URL executableURL = FileLocator.resolve(fileURL);
                if (executableURL != null) {
                    File executable = new File(executableURL.toURI());
                    executable.setExecutable(true);
                    return executable.getAbsolutePath();
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to get " + executableName + " path from " + pluginName + " plugin.",
                        e);
            }
        }
        throw new RuntimeException("Failed to get " + executableName + " path from " + pluginName + " plugin.");
    }
}
