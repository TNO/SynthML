package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/** Collects basic typing information from a model that can be queried. */
public class CifContext {
	/**
	 * All elements are {@link EClass#isSuperTypeOf(EClass) derived} from
	 * {@link UMLPackage.Literals#NAMED_ELEMENT}.
	 */
	private static final Set<EClass> CONTEXT_TYPES = Sets.newHashSet(UMLPackage.Literals.PACKAGE,
			UMLPackage.Literals.ENUMERATION, UMLPackage.Literals.ENUMERATION_LITERAL, UMLPackage.Literals.CLASS,
			UMLPackage.Literals.PROPERTY, UMLPackage.Literals.ACTIVITY);

	static {
		for (EClass contextType : CONTEXT_TYPES) {
			if (!UMLPackage.Literals.NAMED_ELEMENT.isSuperTypeOf(contextType)) {
				throw new IllegalArgumentException("Invalid context type: " + contextType.getName());
			}
		}
	}

	public static QueryableIterable<NamedElement> queryContextElements(Model model) {
		return QueryableIterable.from(model.eAllContents()).select(e -> CONTEXT_TYPES.contains(e.eClass()))
				.asType(NamedElement.class);
	}
	
	private final Map<String, NamedElement> contextElements;
	
	private final PrimitiveType booleanType;

	private final PrimitiveType integerType;

	public CifContext(Element element) {
		// Do not check duplicates here, as that is the responsibility of model validation
		contextElements = queryContextElements(element.getModel()).toMap(NamedElement::getName);

		// Use the resource set of the element to load the primitive types, such that they can be used for comparison
		Resource resource = element.eResource();
		ResourceSet resourceSet = resource == null ? null : resource.getResourceSet();
		Preconditions.checkNotNull(resourceSet, "Expected element to be contained by a resource set.");
		resource = resourceSet.getResource(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), true);
		Package primitivesPackage = (Package) EcoreUtil.getObjectByType(resource.getContents(),
				UMLPackage.Literals.PACKAGE);
		booleanType = (PrimitiveType) primitivesPackage.getOwnedType("Boolean");
		integerType = (PrimitiveType) primitivesPackage.getOwnedType("Integer");
	}

	public PrimitiveType getBooleanType() {
		return booleanType;
	}

	public PrimitiveType getIntegerType() {
		return integerType;
	}

	public boolean isDeclared(String name) {
		return contextElements.containsKey(name);
	}
	
	protected NamedElement getElement(String name) {
		return contextElements.get(name);
	}

	public boolean isEnumeration(String name) {
		return contextElements.get(name) instanceof Enumeration;
	}

	public Enumeration getEnumeration(String name) {
		if (contextElements.get(name) instanceof Enumeration enumeration) {
			return enumeration;
		}
		return null;
	}

	public boolean isEnumerationLiteral(String name) {
		return contextElements.get(name) instanceof EnumerationLiteral;
	}

	public EnumerationLiteral getEnumerationLiteral(String name) {
		if (contextElements.get(name) instanceof EnumerationLiteral literal) {
			return literal;
		}
		return null;
	}

	public boolean isVariable(String name) {
		return contextElements.get(name) instanceof Property;
	}

	public Property getVariable(String name) {
		if (contextElements.get(name) instanceof Property property) {
			return property;
		}
		return null;
	}
}
