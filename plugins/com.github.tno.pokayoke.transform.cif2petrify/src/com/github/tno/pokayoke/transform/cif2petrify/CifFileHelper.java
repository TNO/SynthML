////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.cif2petrify;

import java.nio.file.Path;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;

public class CifFileHelper {
    private CifFileHelper() {
    }

    public static Specification loadCifSpec(Path sourcePath) {
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(sourcePath.toString(), sourcePath.toAbsolutePath().toString(), false);
        return reader.read();
    }
}
