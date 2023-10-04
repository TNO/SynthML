
package com.github.tno.pokayoke.transform.uml.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Factory for UML resources in tests.
 */
public class TestFactory implements Resource.Factory {
    @Override
    public Resource createResource(URI uri) {
        return new TestResource(uri);
    }
}
