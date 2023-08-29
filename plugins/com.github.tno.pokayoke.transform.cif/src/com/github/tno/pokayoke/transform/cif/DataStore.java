
package com.github.tno.pokayoke.transform.cif;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

/** Store data (i.e., variables and events) defined in the CIF automaton model.*/
public class DataStore {
    private final Map<String, EnumLiteral> enumLiterals = new LinkedHashMap<>();

    private final Map<String, DiscVariable> variables = new LinkedHashMap<>();

    private final Map<String, EnumDecl> enumVariables = new LinkedHashMap<>();

    private final Map<String, Event> events = new LinkedHashMap<>();

    public DataStore() {
    }

    public EnumDecl getEnumeration(String name) {
        return enumVariables.get(name);
    }

    public void addEnumeration(String name, EnumDecl enumVar) {
        enumVariables.put(name, enumVar);
    }

    public boolean isEnumeration(String name) {
        return enumVariables.containsKey(name);
    }

    public EnumLiteral getEnumerationLiteral(String name) {
        return enumLiterals.get(name);
    }

    public void addEnumerationLiteral(String name, EnumLiteral enumLiteral) {
        enumLiterals.put(name, enumLiteral);
    }

    public DiscVariable getVariable(String name) {
        return variables.get(name);
    }

    public void addVariable(String name, DiscVariable variable) {
        variables.put(name, variable);
    }

    public Event getEvent(String name) {
        return events.get(name);
    }

    public void addEvent(String name, Event variable) {
        events.put(name, variable);
    }

    public boolean isDeclared(String name) {
        return isEnumeration(name) || isEnumerationLiteral(name) || isVariable(name);
    }

    public boolean isEnumerationLiteral(String name) {
        return enumLiterals.containsKey(name);
    }

    public boolean isVariable(String name) {
        return variables.containsKey(name);
    }
}
