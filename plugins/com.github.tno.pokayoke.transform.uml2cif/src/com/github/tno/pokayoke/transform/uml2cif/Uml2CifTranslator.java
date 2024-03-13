
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.AlgParameter;
import org.eclipse.escet.cif.metamodel.cif.ComponentDef;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.declarations.VariableValue;
import org.eclipse.escet.cif.metamodel.cif.expressions.AlgVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.ValueSpecification;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public abstract class Uml2CifTranslator {
    protected final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap = new LinkedHashMap<>();

    protected final Map<Enumeration, EnumDecl> enumMap = new LinkedHashMap<>();

    protected final Map<OpaqueBehavior, Event> eventMap = new LinkedHashMap<>();

    protected final Map<Property, DiscVariable> variableMap = new LinkedHashMap<>();

    public abstract Expression parseExpression(String expr);

    public abstract Update parseUpdate(String update);

    /**
     * Parses a given comma-separated sequence of updates into a list of CIF updates.
     *
     * @param updates The sequence of updates to parse.
     * @return The parsed updates.
     */
    public List<Update> parseUpdates(String updates) {
        return Arrays.stream(updates.split(",")).map(this::parseUpdate).toList();
    }

    /**
     * Translates an UML model to a CIF specification.
     *
     * @param umlModel The UML model to translate.
     * @return The translated CIF specification.
     */
    public Specification translateModel(Model umlModel) {
        Specification cifSpec = CifConstructors.newSpecification();

        // Add predefined components.
        cifSpec.getDefinitions().add(createPostconditionAutomaton());

        // Translate all enumerations.
        for (PackageableElement element: umlModel.getPackagedElements()) {
            if (element instanceof Enumeration umlEnumeration) {
                cifSpec.getDeclarations().add(translateEnumeration(umlEnumeration));
            }
        }

        // Translate all classes.
        for (PackageableElement element: umlModel.getPackagedElements()) {
            if (element instanceof Class umlClass) {
                cifSpec.getComponents().add(translateClass(umlClass));
            }
        }

        return cifSpec;
    }

    /**
     * Translates an UML enumeration to a CIF enumeration declaration.
     *
     * @param umlEnumeration The UML enumeration to translate.
     * @return The translated CIF enumeration declaration.
     */
    public EnumDecl translateEnumeration(Enumeration umlEnumeration) {
        String name = umlEnumeration.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Expected a non-empty enumeration name.");
        EnumDecl cifEnum = CifConstructors.newEnumDecl();
        cifEnum.setName(name);
        umlEnumeration.getOwnedLiterals().forEach(l -> cifEnum.getLiterals().add(translateEnumerationLiteral(l)));
        enumMap.put(umlEnumeration, cifEnum);
        return cifEnum;
    }

    /**
     * Translates an UML enumeration literal to a CIF enumeration literal.
     *
     * @param umlEnumLiteral The UML enumeration literal to translate.
     * @return The translated CIF enumeration literal.
     */
    public EnumLiteral translateEnumerationLiteral(EnumerationLiteral umlEnumLiteral) {
        String name = umlEnumLiteral.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Expected a non-empty literal name.");
        EnumLiteral cifEnumLiteral = CifConstructors.newEnumLiteral();
        cifEnumLiteral.setName(name);
        enumLiteralMap.put(umlEnumLiteral, cifEnumLiteral);
        return cifEnumLiteral;
    }

    /**
     * Translates a UML class to a CIF plant automaton.
     *
     * @param umlClass The UML class to translate.
     * @return The translated CIF plant automaton.
     */
    public Automaton translateClass(Class umlClass) {
        // Construct a CIF plant automaton for the specified UML class.
        String umlClassName = umlClass.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(umlClassName), "Expected a non-empty class name.");
        Automaton cifPlant = CifConstructors.newAutomaton();
        cifPlant.setKind(SupKind.PLANT);
        cifPlant.setName(umlClassName);

        // Define discrete CIF variable declarations for every UML class property.
        umlClass.getOwnedAttributes().forEach(p -> cifPlant.getDeclarations().add(translateProperty(p)));

        // Define CIF event declarations for every opaque UML class behavior.
        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                cifPlant.getDeclarations().add(translateOpaqueBehavior(umlOpaqueBehavior));
            }
        }

        // Create the single location within the CIF plant.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(createBoolExpression(true));
        cifLocation.getMarkeds().add(createBoolExpression(true));
        cifPlant.getLocations().add(cifLocation);

        // Create CIF edges for every opaque UML class behavior.
        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                // Obtain the CIF event that has been created for the current UML behavior.
                Event cifEvent = eventMap.get(umlOpaqueBehavior);
                Preconditions.checkNotNull(cifEvent, "Expected a non-null event.");

                // Obtain the guard and updates for the current UML behavior.
                List<Expression> guards = umlOpaqueBehavior.getBodies().stream().limit(1).map(this::parseExpression)
                        .toList();
                List<List<Update>> updateClauses = umlOpaqueBehavior.getBodies().stream().skip(1)
                        .map(this::parseUpdates).collect(Collectors.toCollection(LinkedList::new));

                // Ensure there is at least one update clause.
                if (updateClauses.isEmpty()) {
                    updateClauses.add(ImmutableList.of());
                }

                // Create a CIF edge for every update clause.
                for (List<Update> updates: updateClauses) {
                    EventExpression cifEventExpr = CifConstructors.newEventExpression();
                    cifEventExpr.setEvent(cifEvent);
                    cifEventExpr.setType(CifConstructors.newBoolType());
                    EdgeEvent cifEdgeEvent = CifConstructors.newEdgeEvent();
                    cifEdgeEvent.setEvent(cifEventExpr);
                    Edge cifEdge = CifConstructors.newEdge();
                    cifEdge.getEvents().add(cifEdgeEvent);
                    cifEdge.getGuards().addAll(EcoreUtil.copyAll(guards));
                    cifEdge.getUpdates().addAll(updates);
                    cifLocation.getEdges().add(cifEdge);
                }
            }
        }

        return cifPlant;
    }

    public Event translateOpaqueBehavior(OpaqueBehavior umlBehavior) {
        String umlBehaviorName = umlBehavior.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(umlBehaviorName), "Expected a non-empty behavior name.");
        Event cifEvent = CifConstructors.newEvent();

        // Populate the CIF event depending on whether the UML behavior is deterministic or not.
        if (umlBehavior.getBodies().size() > 2) {
            cifEvent.setControllable(false);
            cifEvent.setName("u_" + umlBehaviorName);
        } else {
            cifEvent.setControllable(true);
            cifEvent.setName("c_" + umlBehaviorName);
        }

        eventMap.put(umlBehavior, cifEvent);

        return cifEvent;
    }

    /**
     * Translates a UML class property to a CIF discrete variable.
     *
     * @param umlProperty The UML class property to translate.
     * @return The translated CIF discrete variable.
     */
    public DiscVariable translateProperty(Property umlProperty) {
        String umlPropertyName = umlProperty.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(umlPropertyName), "Expected a non-empty property name.");
        DiscVariable cifVariable = CifConstructors.newDiscVariable();
        cifVariable.setName(umlPropertyName);
        cifVariable.setType(translateType(umlProperty.getType()));

        // Translate the default value if set.
        ValueSpecification umlDefaultValue = umlProperty.getDefaultValue();
        if (umlDefaultValue != null) {
            VariableValue cifDefaultValue = CifConstructors.newVariableValue();
            cifDefaultValue.getValues().add(translateValueSpecification(umlDefaultValue));
            cifVariable.setValue(cifDefaultValue);
        }

        variableMap.put(umlProperty, cifVariable);

        return cifVariable;
    }

    public CifType translateType(Type umlType) {
        if (umlType instanceof Enumeration umlEnumeration) {
            return translateEnumerationType(umlEnumeration);
        } else if (umlType instanceof PrimitiveType umlPrimitiveType) {
            return translatePrimitiveType(umlPrimitiveType);
        } else {
            throw new RuntimeException("Unsupported type: " + umlType);
        }
    }

    public CifType translateEnumerationType(Enumeration umlEnumeration) {
        EnumDecl cifEnumDecl = enumMap.get(umlEnumeration);
        Preconditions.checkNotNull(cifEnumDecl, "Expected a non-null enumeration declaration.");
        EnumType cifEnumType = CifConstructors.newEnumType();
        cifEnumType.setEnum(cifEnumDecl);
        return cifEnumType;
    }

    public CifType translatePrimitiveType(PrimitiveType umlPrimitiveType) {
        if (umlPrimitiveType.getLabel().equals("Boolean")) {
            return CifConstructors.newBoolType();
        } else {
            throw new RuntimeException("Unsupported primitive type: " + umlPrimitiveType);
        }
    }

    public Expression translateValueSpecification(ValueSpecification umlSpec) {
        if (umlSpec instanceof InstanceValue umlValue) {
            return translateInstanceValue(umlValue);
        } else if (umlSpec instanceof LiteralBoolean umlLiteral) {
            return translateLiteralBoolean(umlLiteral);
        } else if (umlSpec instanceof OpaqueExpression umlExpr) {
            return translateOpaqueExpression(umlExpr);
        } else {
            throw new RuntimeException("Unsupported value specification: " + umlSpec);
        }
    }

    public Expression translateInstanceValue(InstanceValue umlValue) {
        return translateInstanceSpecification(umlValue.getInstance());
    }

    public Expression translateInstanceSpecification(InstanceSpecification umlInstance) {
        if (umlInstance instanceof EnumerationLiteral umlLiteral) {
            EnumLiteral cifLiteral = enumLiteralMap.get(umlLiteral);
            Preconditions.checkNotNull(cifLiteral, "Expected a non-null enumeration literal.");
            EnumLiteralExpression cifExpr = CifConstructors.newEnumLiteralExpression();
            cifExpr.setLiteral(cifLiteral);
            cifExpr.setType(translateEnumerationType(umlLiteral.getEnumeration()));
            return cifExpr;
        } else {
            throw new RuntimeException("Unsupported instance specification: " + umlInstance);
        }
    }

    public Expression translateLiteralBoolean(LiteralBoolean umlLiteral) {
        return createBoolExpression(umlLiteral.isValue());
    }

    public Expression translateOpaqueExpression(OpaqueExpression umlExpr) {
        Preconditions.checkArgument(umlExpr.getBodies().size() == 1, "Expected exactly one opaque expression body.");
        return parseExpression(umlExpr.getBodies().get(0));
    }

    protected BoolExpression createBoolExpression(boolean value) {
        BoolExpression cifBoolExpr = CifConstructors.newBoolExpression();
        cifBoolExpr.setType(CifConstructors.newBoolType());
        cifBoolExpr.setValue(value);
        return cifBoolExpr;
    }

    protected ComponentDef createPostconditionAutomaton() {
        ComponentDef component = CifConstructors.newComponentDef();

        // Create the component parameter.
        AlgParameter parameter = CifConstructors.newAlgParameter();
        component.getParameters().add(parameter);
        AlgVariable variable = CifConstructors.newAlgVariable();
        variable.setName("postcondition");
        variable.setType(CifConstructors.newBoolType());
        parameter.setVariable(variable);

        // Create the component body, which is the requirement automaton.
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setKind(SupKind.REQUIREMENT);
        automaton.setName("Postcondition");
        component.setBody(automaton);

        // Define the event that indicates that the postcondition is satisfied.
        Event event = CifConstructors.newEvent();
        event.setControllable(true);
        event.setName("c_satisfied");
        automaton.getDeclarations().add(event);

        // Define the two locations of the automaton.
        Location notSatisfied = CifConstructors.newLocation();
        notSatisfied.getInitials().add(createBoolExpression(true));
        notSatisfied.setName("NotSatisfied");
        automaton.getLocations().add(notSatisfied);
        Location satisfied = CifConstructors.newLocation();
        satisfied.setName("Satisfied");
        satisfied.getMarkeds().add(createBoolExpression(true));
        automaton.getLocations().add(satisfied);

        // Define the edge between the two locations defined above.
        EventExpression eventExpr = CifConstructors.newEventExpression();
        eventExpr.setEvent(event);
        eventExpr.setType(CifConstructors.newBoolType());
        EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
        edgeEvent.setEvent(eventExpr);
        AlgVariableExpression guard = CifConstructors.newAlgVariableExpression();
        guard.setType(CifConstructors.newBoolType());
        guard.setVariable(variable);
        Edge edge = CifConstructors.newEdge();
        edge.getEvents().add(edgeEvent);
        edge.getGuards().add(guard);
        edge.setTarget(satisfied);
        notSatisfied.getEdges().add(edge);

        return component;
    }
}
