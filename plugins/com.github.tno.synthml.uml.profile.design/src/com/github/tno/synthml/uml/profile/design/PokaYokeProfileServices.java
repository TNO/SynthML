////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.sirius.diagram.DSemanticDiagram;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ParameterableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.RedefinableTemplateSignature;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.TemplateParameter;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.Type;
import org.obeonetwork.dsl.uml2.core.api.services.ReusedDescriptionServices;
import org.obeonetwork.dsl.uml2.core.internal.services.DirectEditLabelSwitch;
import org.obeonetwork.dsl.uml2.core.internal.services.DisplayLabelSwitch;
import org.obeonetwork.dsl.uml2.core.internal.services.EditLabelSwitch;
import org.obeonetwork.dsl.uml2.core.internal.services.LabelServices;

import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.synthml.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import SynthML.FormalCallBehaviorAction;
import SynthML.FormalControlFlow;
import SynthML.FormalElement;

/**
 * The services class used by VSM.
 * <p>
 * All setters in this class should {@link PokaYokeUmlProfileUtil#applyPokaYokeProfile(org.eclipse.uml2.uml.Element)
 * apply the Poka Yoka profile}.
 * </p>
 */
public class PokaYokeProfileServices {
    private static final String GUARD_EFFECTS_LAYER = "PY_GuardsEffects";

    /**
     * Regex pattern for extracting the signature from the label of a potentially parameterized {@link Activity}. Group
     * 1 captures the main label, matching as broadly as the UML Designer label expression. Group 2 (optional) captures
     * the content enclosed within angle brackets (< >), if present.
     */
    private static final String LABEL_SIGNATURE_PATTERN = "^(.*?)(?:<(.*)>)?$";

    /**
     * Regex pattern for extracting potential arguments from the label of a {@link CallBehaviorAction}. Group 1 captures
     * the main label, matching as broadly as the UML Designer label expression. Group 2 (optional) captures the content
     * enclosed within angle brackets (< >), if present.
     */
    private static final String LABEL_ARGUMENTS_PATTERN = LABEL_SIGNATURE_PATTERN;

    private static final String EFFECTS_SEPARATOR = System.lineSeparator() + "~~~" + System.lineSeparator();

    /**
     * Returns {@code true} if {@link FormalElement} stereotype is applied on {@link RedefinableElement element} while
     * 'guard and effects' layer is not enabled.
     *
     * @param element The element to interrogate.
     * @param diagram The diagram containing the element.
     * @return {@code true} if {@link FormalElement} stereotype is applied on element.
     */
    public boolean decorateFormalElement(RedefinableElement element, DSemanticDiagram diagram) {
        if (diagram.getActivatedLayers().stream().anyMatch(l -> GUARD_EFFECTS_LAYER.equals(l.getName()))) {
            return false;
        }
        return PokaYokeUmlProfileUtil.isFormalElement(element);
    }

    public static boolean isFormalElement(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isFormalElement(element);
    }

    /**
     * Returns the {@link FormalElement#getGuard() guard} property value if {@code element} is stereotyped, {@code null}
     * otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#getGuard() guard} property value if {@code element} is stereotyped, {@code null}
     *     otherwise.
     */
    public String getGuard(RedefinableElement element) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use incoming or outgoing guard getter.");
        }
        return PokaYokeUmlProfileUtil.getGuard(element);
    }

    /**
     * Returns the control flow {@link PokaYokeUmlProfileUtil#getIncomingGuard incoming guard}.
     *
     * @param controlFlow The control flow to interrogate.
     * @return The {@link FormalControlFlow#getOutgoingGuard() guard} property value if {@code controlFlow} is
     *     stereotyped, {@code null} otherwise.
     */
    public String getIncomingGuard(ControlFlow controlFlow) {
        return PokaYokeUmlProfileUtil.getIncomingGuard(controlFlow);
    }

    /**
     * Returns the {@link FormalControlFlow#getOutgoingGuard() outgoing guard} property value if {@code controlFlow} is
     * stereotyped, {@code null} otherwise.
     *
     * @param controlFlow The control flow to interrogate.
     * @return The {@link FormalControlFlow#getOutgoingGuard() guard} property value if {@code controlFlow} is
     *     stereotyped, {@code null} otherwise.
     */
    public String getOutgoingGuard(ControlFlow controlFlow) {
        return PokaYokeUmlProfileUtil.getOutgoingGuard(controlFlow);
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setGuard(String) guard} property
     * for {@code element}.
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@link String#isEmpty()
     * empty} and {@link #getEffects(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty} and
     * {@link #isAtomic(RedefinableElement)} is {@code false}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setGuard(RedefinableElement element, String newValue) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use incoming or outgoing guard setter.");
        }

        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);

        // Empty values are not allowed, so reset the value.
        PokaYokeUmlProfileUtil.setGuard(element, Strings.emptyToNull(newValue));

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger a refresh here.
        refresh(element);
    }

    /**
     * Sets the {@link ControlFlow#getGuard guard} as an opaque expression with one body containing {@code newValue}.
     *
     * @param controlFlow The control flow to set the property on.
     * @param newValue The new property value.
     */
    public void setIncomingGuard(ControlFlow controlFlow, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(controlFlow);
        PokaYokeUmlProfileUtil.setIncomingGuard(controlFlow, Strings.emptyToNull(newValue));
    }

    /**
     * Returns the activity name, and optionally a string representation of the template signature if {@code activity}
     * is stereotyped.
     *
     * @param activity The element to interrogate.
     * @return The activity name, and optionally a string representation of the template signature if {@code activity}
     *     is stereotyped.
     */
    public String getActivityLabel(Activity activity) {
        // The switch is an implementation of the visitor pattern without double dispatch. It uses pattern matching
        // to forward a 'DisplayLabelSwitch.doSwitch' call to specialized methods.
        DisplayLabelSwitch displaySwitch = new DisplayLabelSwitch();
        String activityLabel = displaySwitch.caseBehavior(activity);

        TemplateSignature ownedTemplateSignature = activity.getOwnedTemplateSignature();
        List<TemplateParameter> templateParameters = (ownedTemplateSignature != null)
                ? ownedTemplateSignature.getOwnedParameters() : new ArrayList<>();
        List<String> parameterSignatures = new ArrayList<>();
        for (TemplateParameter templateParameter: templateParameters) {
            // Extract the name of the element.
            ParameterableElement parameterableElement = templateParameter.getOwnedDefault();
            if (!(parameterableElement instanceof NamedElement namedParameterableElement)) {
                continue;
            }

            // Add the type signature if it can be determined.
            String parameterSignature = namedParameterableElement.getName();
            if (templateParameter instanceof ClassifierTemplateParameter classifier) {
                var firstClassifier = classifier.getConstrainingClassifiers().stream().findFirst();
                if (firstClassifier.isPresent()) {
                    parameterSignature += ":" + firstClassifier.get().getName();
                }
            }

            parameterSignatures.add(parameterSignature);
        }

        if (parameterSignatures.isEmpty()) {
            return activityLabel;
        }

        return activityLabel + "<" + String.join(",", parameterSignatures) + ">";
    }

    /**
     * Inspired by {@link LabelServices#editUmlLabel(Element, String) editUMLLabel}. Updates an {@link Activity}s label
     * and adds, modifies or removes the {@link RedefinableTemplateSignature}.
     *
     * @param activity The element to interrogate.
     * @param editedLabelContent The new label content, which may include template signature information.
     */
    public void setActivityLabel(Activity activity, String editedLabelContent) {
        // Parse the label.
        Pattern pattern = Pattern.compile(LABEL_SIGNATURE_PATTERN);
        Matcher matcher = pattern.matcher(editedLabelContent);

        if (matcher.matches()) {
            String baseLabel = matcher.group(1);
            String generics = matcher.group(2) != null ? matcher.group(2) : "";

            List<Type> dataTypes = PokaYokeTypeUtil.getSupportedTypes(activity);

            // Map the parameter names to corresponding types.
            Map<String, DataType> parameterNameToType = new LinkedHashMap<>();

            for (String part: generics.split(",")) {
                part = part.trim();

                if (part.isEmpty()) {
                    continue;
                }

                String[] split = part.split(":");
                if (split.length == 2) {
                    String name = split[0].trim();
                    String typeName = split[1].trim();

                    Optional<Type> type = dataTypes.stream().filter(dt -> dt.getName().equals(typeName)).findFirst();

                    if (type.isPresent() && type.get() instanceof DataType dataType) {
                        parameterNameToType.put(name, dataType);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }

            // The switch is an implementation of the visitor pattern without double dispatch. It uses pattern matching
            // to forward an 'EditLabelSwitch.doSwitch' call to specialized methods. We directly call these
            // specialized methods.
            EditLabelSwitch editLabel = new EditLabelSwitch();

            // Generate a label without type information. This allows generating a 'RedefinableTemplateSignature' using
            // methods built into UML designer.
            String genericString = parameterNameToType.isEmpty() ? ""
                    : ("<" + String.join(", ", parameterNameToType.keySet()) + ">");
            editLabel.setEditedLabelContent(baseLabel + genericString);

            // Below mimics how UML designer sets template parameters for classes.
            // First 'EditLabelSwitch.parseInputLabel' is called. This method parses the label and updates the
            // underlying 'TemplateableElement'. Since this method is private we proxy it by calling
            // 'EditLabelSwitch.caseTemplateableElement'. Lastly call 'EditLabelSwitch.caseNamedElement' as normal.
            editLabel.caseTemplateableElement(activity);
            editLabel.caseNamedElement(activity);

            // Add type information to the newly created template parameters.
            for (TemplateParameter parameter: activity.getOwnedTemplateSignature().getParameters()) {
                if (parameter instanceof ClassifierTemplateParameter classifier) {
                    String name = ((NamedElement)parameter.getParameteredElement()).getName();

                    EList<Classifier> constrainingClassifiers = classifier.getConstrainingClassifiers();

                    // Clear any pre-existing type information from the parameter.
                    constrainingClassifiers.clear();
                    constrainingClassifiers.add(parameterNameToType.get(name));
                }
            }
        }
    }

    /**
     * Inspired by {@link DisplayLabelSwitch#caseClass(org.eclipse.uml2.uml.Class) caseClass}. Returns the name of the
     * activity, and optionally, if {@code callAction} is stereotyped, the arguments passed to the activity.
     *
     * @param callAction The element to interrogate.
     * @return The name of the activity, and optionally, if {@code callAction} is stereotyped, the arguments passed to
     *     the activity.
     */
    public String getCallBehaviorActionLabel(CallBehaviorAction callAction) {
        // The switch is an implementation of the visitor pattern without double dispatch. It uses pattern matching to
        // forward a 'DisplayLabelSwitch.doSwitch' call to specialized methods. We call those specialized methods
        // directly.
        DisplayLabelSwitch displaySwitch = new DisplayLabelSwitch();

        String arguments = PokaYokeUmlProfileUtil.getArguments(callAction);
        arguments = arguments.replaceAll("\s", "").replace("\n", "");

        if (arguments.isEmpty()) {
            return displaySwitch.caseCallBehaviorAction(callAction);
        } else {
            return displaySwitch.caseCallBehaviorAction(callAction) + "<" + arguments + ">";
        }
    }

    /**
     * Applies the {@link FormalControlFlow} stereotype and sets the {@link FormalControlFlow#setOutgoingGuard(String)
     * guard} property for {@code controlFlow}.
     * <p>
     * The {@link FormalControlFlow} stereotype is removed if {@code newValue} is {@code null} or
     * {@link String#isEmpty() empty}.
     * </p>
     *
     * @param controlFlow The control flow to set the property on.
     * @param newValue The new property value.
     */
    public void setOutgoingGuard(ControlFlow controlFlow, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(controlFlow);
        PokaYokeUmlProfileUtil.setOutgoingGuard(controlFlow, Strings.emptyToNull(newValue));

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger a refresh here.
        refresh(controlFlow);
    }

    public void unsetGuard(RedefinableElement element) {
        setGuard(element, null);
    }

    public boolean isSetGuard(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isSetGuard(element);
    }

    /**
     * Returns the {@link FormalElement#getEffects() effects} property value if {@code element} is stereotyped,
     * {@code null} otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#getEffects() effects} property value if {@code element} is stereotyped,
     *     {@code null} otherwise.
     */
    public String getEffects(RedefinableElement element) {
        return Joiner.on(EFFECTS_SEPARATOR).join(PokaYokeUmlProfileUtil.getEffects(element));
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#getEffects() effects} property for
     * {@code element}.
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@link String#isEmpty()
     * empty} and {@link #getGuard(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty} and
     * {@link #isAtomic(RedefinableElement)} is {@code false}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setEffects(RedefinableElement element, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        if (Strings.isNullOrEmpty(newValue)) {
            // Empty values are not allowed, so reset the value.
            PokaYokeUmlProfileUtil.setEffects(element, null);
        } else {
            PokaYokeUmlProfileUtil.setEffects(element, Splitter.on(EFFECTS_SEPARATOR).splitToList(newValue));
        }

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger the refresh here.
        refresh(element);
    }

    public void unsetEffects(RedefinableElement element) {
        setEffects(element, null);
    }

    public boolean isSetEffects(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isSetEffects(element);
    }

    /**
     * Returns the {@link FormalCallBehaviorAction#getArguments() arguments} property value if {@code element} is
     * stereotyped, an empty string otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalCallBehaviorAction#getArguments() arguments} property value if {@code element} is
     *     stereotyped, an empty string otherwise.
     */
    public String getArguments(CallBehaviorAction element) {
        return PokaYokeUmlProfileUtil.getArguments(element);
    }

    /**
     * Applies the {@link FormalCallBehaviorAction} stereotype and sets the
     * {@link FormalCallBehaviorAction#getArguments() arguments} property for {@code element}.
     * <p>
     * The {@link FormalCallBehaviorAction} stereotype is removed if {@code newValue} is {@code null} or
     * {@link String#isEmpty() empty}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setArguments(CallBehaviorAction element, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        if (Strings.isNullOrEmpty(newValue)) {
            // Empty values are not allowed, so reset the value.
            PokaYokeUmlProfileUtil.setArguments(element, null);
        } else {
            PokaYokeUmlProfileUtil.setArguments(element, newValue);
        }

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger the refresh here.
        refresh(element);
    }

    public void unsetArguments(CallBehaviorAction element) {
        setArguments(element, null);
    }

    /**
     * Returns the {@link FormalElement#isAtomic() atomic} property value if {@code element} is stereotyped,
     * {@code false} otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#isAtomic() atomic} property value if {@code element} is stereotyped,
     *     {@code false} otherwise.
     */
    public boolean isAtomic(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isAtomic(element);
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setAtomic(boolean) atomic}
     * property for {@code element}.
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@code false} and
     * {@link #getGuard(RedefinableElement)} is {@code null} or {@link String#isEmpty() empty} and
     * {@link #getEffects(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setAtomic(RedefinableElement element, Boolean newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        PokaYokeUmlProfileUtil.setAtomic(element, newValue);

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger the refresh here.
        refresh(element);
    }

    public void setPropertyName(Property property, String newValue) {
        setName(property, newValue);
        setPropertyBounds(property);
    }

    /**
     * Returns all Poka Yoke supported types for {@code property}.
     *
     * @param property The property (i.e. context) for which the supported types are queried.
     * @return All Poka Yoke supported types for {@code property}.
     * @see PokaYokeTypeUtil#getSupportedTypes(org.eclipse.uml2.uml.Element)
     */
    public List<Type> getSupportedPropertyTypes(Property property) {
        return PokaYokeTypeUtil.getSupportedTypes(property);
    }

    public void setPropertyType(Property property, Type newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(property);
        property.setType(newValue);
        setPropertyBounds(property);
    }

    public String getControlFlowLabel(ControlFlow controlFlow) {
        String label = controlFlow.getName();
        label = Strings.nullToEmpty(label);

        // If at least one guard is present, visualize both.
        String guards = label.isEmpty() ? "" : System.getProperty("line.separator");
        String incomingGuard = getIncomingGuard(controlFlow);
        String outgoingGuard = getOutgoingGuard(controlFlow);
        if (!(Strings.isNullOrEmpty(outgoingGuard) || outgoingGuard.equals("true"))
                || !(Strings.isNullOrEmpty(incomingGuard) || incomingGuard.equals("true")))
        {
            if (Strings.isNullOrEmpty(incomingGuard)) {
                incomingGuard = "true";
            }
            incomingGuard = "In: " + incomingGuard;

            if (Strings.isNullOrEmpty(outgoingGuard)) {
                outgoingGuard = "true";
            }
            outgoingGuard = "Out: " + outgoingGuard;

            guards += incomingGuard + System.getProperty("line.separator") + outgoingGuard;
        }

        label += guards;
        return label;
    }

    /**
     * Overrides the {@link LabelServices#editUmlLabel(Element, String) editUmlLabel} method in UML Designer. This
     * implementation allows arguments to be changed for {@link CallBehaviorAction} to parameterized activities. It also
     * changes the name of an {@link ActivityEdge activity edge} without altering its guard. The override occurs
     * implicitly because {@link PokaYokeProfileServices} is added to the viewpoint. This method is called through
     * Activity Diagram defined in the uml2core.odesign file in the UML Designer project.
     *
     * @param context The UML element to be edited.
     * @param editedLabelContent The new label content.
     * @return The edited UML element.
     */
    public Element editUmlLabel(Element context, String editedLabelContent) {
        if (context instanceof ActivityEdge edge) {
            // The implementation in UML Designer uses 'doSwitch' to change both the guard and the name of an
            // 'ActivityEdge'. This implementation only changes the name.
            setName(edge, editedLabelContent);
            return edge;
        } else if (context instanceof CallBehaviorAction callAction) {
            // Parse the colon to specify the type.
            Pattern pattern = Pattern.compile(LABEL_ARGUMENTS_PATTERN);
            Matcher matcher = pattern.matcher(editedLabelContent);

            // Translate the 'CallBehaviorAction' argument notation to standard CIF notation.
            if (matcher.matches()) {
                editedLabelContent = matcher.group(1);
                String generics = matcher.group(2) != null ? matcher.group(2) : "";
                setArguments(callAction, generics.replace(",", ",\n"));
            } else {
                setArguments(callAction, "");
            }
        }

        EditLabelSwitch editLabel = new EditLabelSwitch();
        editLabel.setEditedLabelContent(editedLabelContent);
        return editLabel.doSwitch(context);
    }

    /**
     * Compute the label of the given element for direct edit. Overrides the
     * {@link ReusedDescriptionServices#computeUmlDirectEditLabel(Element) computeUmlDirectEditLabel} method in UML
     * Designer. It adds the arguments to the label of {@link CallBehaviorAction}, and it returns the name of an
     * {@link ActivityEdge} without adding the stereotype name within angle brackets before it. The override occurs
     * implicitly because {@link PokaYokeProfileServices} is added to the viewpoint. This method is called through
     * Activity Diagram defined in the uml2core.odesign file in the UML Designer project.
     *
     * @param element The {@link Element} for which to retrieve a label.
     * @return The computed label.
     */
    public String computeUmlDirectEditLabel(Element element) {
        if (element instanceof ActivityEdge edge) {
            // The implementation in UML Designer uses 'doSwitch' to return both the name of an 'ActivityEdge' and
            // and the stereotypes applied to it. This implementation only returns the name.
            return edge.getName();
        } else if (element instanceof CallBehaviorAction callAction) {
            return getCallBehaviorActionLabel(callAction);
        }

        final DirectEditLabelSwitch directEditLabel = new DirectEditLabelSwitch();
        return directEditLabel.doSwitch(element);
    }

    /**
     * Applies the Poka Yoke UML Profile and set the {@link Property#setDefault(String) default value} property for
     * {@code property}.
     *
     * @param property The property to set the default value on.
     * @param newValue The new default value of the property.
     */
    public void setPropertyDefaultValue(Property property, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(property);
        PokaYokeUmlProfileUtil.setDefaultValue(property, newValue);
        setPropertyBounds(property);
    }

    private void setPropertyBounds(Property property) {
        property.setLower(1);
        property.setUpper(1);
    }

    /**
     * Returns the supported super types for the primitive {@code type}.
     * <p>
     * Currently only {@link UmlPrimitiveType#INTEGER primitive integer} is a supported super type.
     * </p>
     *
     * @param type The type context for resolving the available super types.
     * @return The supported super types for {@code type}.
     */
    public List<PrimitiveType> getSupportedSuperTypes(PrimitiveType type) {
        return Arrays.asList(UmlPrimitiveType.INTEGER.load(type));
    }

    public PrimitiveType getSuperType(PrimitiveType type) {
        return type.getGeneralizations().stream().map(Generalization::getGeneral)
                .filter(PrimitiveType.class::isInstance).map(PrimitiveType.class::cast).findAny().orElse(null);
    }

    public void setSuperType(PrimitiveType type, PrimitiveType superType) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(type);
        if (superType == null) {
            type.getGeneralizations().clear();
        } else if (type.getGeneralization(superType) == null) {
            type.getGeneralizations().clear();
            type.createGeneralization(superType);
        }
    }

    public String getMinValue(PrimitiveType type) {
        Integer minValue = PokaYokeTypeUtil.getMinValue(type);
        return minValue == null ? null : minValue.toString();
    }

    public void setMinValue(PrimitiveType type, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(type);
        try {
            Integer intValue = Strings.isNullOrEmpty(newValue) ? null : Integer.parseInt(newValue);
            PokaYokeTypeUtil.setMinValue(type, intValue);
        } catch (NumberFormatException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, getClass(),
                    "Failed to parse integer value: " + e.getLocalizedMessage(), e));
        }
    }

    public String getMaxValue(PrimitiveType type) {
        Integer maxValue = PokaYokeTypeUtil.getMaxValue(type);
        return maxValue == null ? null : maxValue.toString();
    }

    public void setMaxValue(PrimitiveType type, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(type);
        try {
            Integer intValue = Strings.isNullOrEmpty(newValue) ? null : Integer.parseInt(newValue);
            PokaYokeTypeUtil.setMaxValue(type, intValue);
        } catch (NumberFormatException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, getClass(),
                    "Failed to parse integer value: " + e.getLocalizedMessage(), e));
        }
    }

    public void setName(NamedElement element, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        element.setName(newValue);
    }

    /**
     * Workaround for triggering a viewer refresh, for a {@link NamedElement}.
     *
     * @param element The element to refresh.
     */
    private void refresh(NamedElement element) {
        if (element.isSetName()) {
            String name = element.getName();
            element.unsetName();
            element.setName(name);
        } else {
            element.setName("");
            element.unsetName();
        }
    }

    public Stereotype getConstraintFirstStereotype(Constraint constraint) {
        return PokaYokeUmlProfileUtil.getConstraintFirstStereotype(constraint);
    }

    public List<Stereotype> getSupportedConstraintStereotypes(Constraint constraint) {
        return PokaYokeUmlProfileUtil.getSupportedConstraintStereotypes(constraint);
    }

    public void setConstraintStereotype(Constraint constraint, Stereotype stereotype) {
        PokaYokeUmlProfileUtil.setConstraintStereotype(constraint, stereotype);

        // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
        // is used in the odesign file. So we trigger a refresh here.
        refresh(constraint);
    }

    public String getConstraintExpression(Constraint constraint) {
        return PokaYokeUmlProfileUtil.getConstraintExpression(constraint);
    }

    public void setConstraintExpression(Constraint constraint, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(constraint);
        PokaYokeUmlProfileUtil.setConstraintExpression(constraint, Strings.emptyToNull(newValue));
    }

    public String getStereotypeName(Stereotype st) {
        return PokaYokeUmlProfileUtil.getStereotypeName(st);
    }
}
