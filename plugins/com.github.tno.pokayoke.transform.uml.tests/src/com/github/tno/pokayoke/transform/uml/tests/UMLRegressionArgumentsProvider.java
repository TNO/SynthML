
package com.github.tno.pokayoke.transform.uml.tests;

import com.github.tno.pokayoke.transform.tests.common.RegressionArgumentsProvider;

/**
 * UML Regression Arguments Provider.
 */
public class UMLRegressionArgumentsProvider extends RegressionArgumentsProvider {
    public static final String INPUTFILEEXTENSION = "uml";

    @Override
    public String getInputFileExtension() {
        return INPUTFILEEXTENSION;
    }

    public static final String OUTPUTFILEEXTENSION = "umltst";

    @Override
    public String getOutputFileExtension() {
        return OUTPUTFILEEXTENSION;
    }
}
