
package com.github.tno.pokayoke.transform.uml.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.internal.resource.UMLResourceImpl;

/**
 * Test resource.
 *
 * <p>
 * In tests, UML resources should be reproducible. Hence, they should not depend on random generated identifiers (UUIDs) but on
 * relative paths.
 * </p>
 */
@SuppressWarnings("restriction")
public class TestResource extends UMLResourceImpl {
    public TestResource(URI uri) {
        super(uri);
    }

    @Override
    protected boolean useUUIDs() {
        return false;
    }
}
