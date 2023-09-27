
package com.github.tno.pokayoke.transform.cif;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

/** Store data (e.g., variables and events) defined in the CIF automaton model. */
public class DataStore {
    private final Map<String, EnumLiteral> enumLiterals = new LinkedHashMap<>();

    private final Map<String, DiscVariable> variables = new LinkedHashMap<>();

    private final Map<String, EnumDecl> enumerations = new LinkedHashMap<>();

    private final Map<EnumLiteral, EnumDecl> enumLiteralToEnum = new LinkedHashMap<>();

    private final Map<String, Event> events = new LinkedHashMap<>();

    public DataStore() {
    }

    protected EnumDecl getEnumeration(String name) {
        return enumerations.get(name);
    }

    protected EnumDecl getEnumeration(EnumLiteral enumLiteral) {
        return enumLiteralToEnum.get(enumLiteral);
    }

    protected void addEnumeration(String name, EnumDecl enumDecl) {
        enumerations.put(name, enumDecl);
    }

    protected boolean isEnumeration(String name) {
        return enumerations.containsKey(name);
    }

    protected EnumLiteral getEnumerationLiteral(String name) {
        return enumLiterals.get(name);
    }

    protected void addEnumerationLiteral(String name, EnumLiteral enumLiteral, EnumDecl enumDecl) {
        enumLiterals.put(name, enumLiteral);

        // Map enum literal to enum. The diagram in the specification (https://www.omg.org/spec/UML/2.5.1/PDF page 209)
        // shows that one enum can have 0 or more enum literals and each enum literal is only associated with one enum.
        enumLiteralToEnum.put(enumLiteral, enumDecl);
    }

    protected boolean isEnumerationLiteral(String name) {
        return enumLiterals.containsKey(name);
    }

    protected DiscVariable getVariable(String name) {
        return variables.get(name);
    }

    protected void addVariable(String name, DiscVariable variable) {
        variables.put(name, variable);
    }

    protected boolean isVariable(String name) {
        return variables.containsKey(name);
    }

    protected Event getEvent(String name) {
        return events.get(name);
    }

    protected void addEvent(String name, Event variable) {
        events.put(name, variable);
    }

}
