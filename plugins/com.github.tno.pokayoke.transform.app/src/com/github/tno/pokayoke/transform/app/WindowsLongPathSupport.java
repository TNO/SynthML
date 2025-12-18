////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.app;

import org.apache.commons.lang3.SystemUtils;

/**
 * Provide support to enable long paths on Windows.
 */
public class WindowsLongPathSupport {
    private WindowsLongPathSupport() {
    }

    public static final String LONG_PATH_PREFIX = "\\\\?\\";

    public static boolean hasLongPathPrefix(String path) {
        return path.startsWith(LONG_PATH_PREFIX);
    }

    public static String ensureLongPathPrefix(String path) {
        return (!hasLongPathPrefix(path) && SystemUtils.IS_OS_WINDOWS) ? LONG_PATH_PREFIX + path : path;
    }
}
