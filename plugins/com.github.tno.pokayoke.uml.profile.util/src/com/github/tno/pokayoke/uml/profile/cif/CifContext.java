package com.github.tno.pokayoke.uml.profile.cif;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Collects basic typing information from a model that can be queried. */
public class CifContext {
	private final Map<String, Enumeration> enums = new LinkedHashMap<>();

	private final Map<String, EnumerationLiteral> enumLiterals = new LinkedHashMap<>();

	private final Map<String, Property> variables = new LinkedHashMap<>();

	private final PrimitiveType booleanType;

	private final PrimitiveType integerType;

	public CifContext(Element element) {
		populateFrom(element.getModel());

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

	public Enumeration getEnumeration(String name) {
		return enums.get(name);
	}

	public EnumerationLiteral getEnumerationLiteral(String name) {
		return enumLiterals.get(name);
	}

	public Property getVariable(String name) {
		return variables.get(name);
	}

	public boolean isDeclared(String name) {
		return isEnumeration(name) || isEnumerationLiteral(name) || isVariable(name);
	}

	public boolean isEnumeration(String name) {
		return enums.containsKey(name);
	}

	public boolean isEnumerationLiteral(String name) {
		return enumLiterals.containsKey(name);
	}

	public boolean isVariable(String name) {
		return variables.containsKey(name);
	}

	private void ensureUndeclared(String name) {
		Verify.verify(!isDeclared(name), String.format("Symbol %s already exists.", name));
	}

	private void populateFrom(Model model) {
		for (PackageableElement element : model.getPackagedElements()) {
			if (element instanceof Model modelElement) {
				populateFrom(modelElement);
			} else if (element instanceof Class classElement) {
				populateFrom(classElement);
			} else if (element instanceof Enumeration enumElement) {
				populateFrom(enumElement);
			} else {
				throw new RuntimeException("Unsupported packaged element type: " + element);
			}
		}
	}

	private void populateFrom(Class classElement) {
		classElement.getOwnedAttributes().forEach(this::populateFrom);
	}

	private void populateFrom(Enumeration enumeration) {
		String enumName = enumeration.getName();
		ensureUndeclared(enumName);
		enums.put(enumName, enumeration);
		enumeration.getOwnedLiterals().forEach(this::populateFrom);
	}

	private void populateFrom(EnumerationLiteral enumLiteral) {
		String name = enumLiteral.getName();
		ensureUndeclared(name);
		enumLiterals.put(name, enumLiteral);
	}

	private void populateFrom(Property property) {
		String name = property.getName();
		ensureUndeclared(name);
		variables.put(name, property);
	}
}
