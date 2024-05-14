
package com.github.tno.pokayoke.transform.petrify2uml;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Factory for UML test resources.
 */
public class UMLTestFactory implements Resource.Factory {
    @Override
    public UMLTestResourceImpl createResource(URI uri) {
        return new UMLTestResourceImpl(uri);
    }
}
