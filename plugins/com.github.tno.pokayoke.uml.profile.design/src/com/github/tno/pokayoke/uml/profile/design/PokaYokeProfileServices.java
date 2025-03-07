
package com.github.tno.pokayoke.uml.profile.design;

import static com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil.FORMAL_ELEMENT_STEREOTYPE;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sirius.diagram.DSemanticDiagram;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.Type;
import org.obeonetwork.dsl.uml2.core.internal.services.EditLabelSwitch;
import org.obeonetwork.dsl.uml2.core.internal.services.LabelServices;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import PokaYoke.FormalElement;

/**
 * The services class used by VSM.
 * <p>
 * All setters in this class should {@link PokaYokeUmlProfileUtil#applyPokaYokeProfile(org.eclipse.uml2.uml.Element)
 * apply the Poka Yoka profile}.
 * </p>
 */
public class PokaYokeProfileServices {
    private static final String GUARD_EFFECTS_LAYER = "PY_GuardsEffects";

    private static final String EFFECTS_SEPARATOR = System.lineSeparator() + "~~~" + System.lineSeparator();

    /**
     * Returns <code>true</code> if {@link FormalElement} stereotype is applied on {@link RedefinableElement element}
     * while 'guard and effects' layer is not enabled.
     *
     * @param element The element to interrogate.
     * @param diagram The diagram containing the element.
     * @return <code>true</code> if {@link FormalElement} stereotype is applied on element.
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
     * Returns the {@link FormalElement#getGuard() guard} property value if <code>element</code> is stereotyped,
     * <code>null</code> otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#getGuard() guard} property value if <code>element</code> is stereotyped,
     *     <code>null</code> otherwise.
     */
    public String getGuard(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.getGuard(element);
    }

    /**
     * Returns the first body of the {@link ActivityEdge#getGuard() incoming guard} if <code>edge</code> is stereotyped,
     * <code>null</code> otherwise.
     *
     * @param edge The activity edge to interrogate.
     * @return The first body of the {@link ActivityEdge#getGuard() incoming guard} if <code>edge</code> is stereotyped,
     *     <code>null</code> otherwise.
     */
    public String getIncomingGuard(ActivityEdge edge) {
        return PokaYokeUmlProfileUtil.getIncomingGuard(edge);
    }

    /**
     * Returns the second body of the {@link ActivityEdge#getGuard() incoming guard} if <code>edge</code> is
     * stereotyped, <code>null</code> otherwise.
     *
     * @param edge The activity edge to interrogate.
     * @return The second body of the {@link ActivityEdge#getGuard() incoming guard} if <code>edge</code> is
     *     stereotyped, <code>null</code> otherwise.
     */
    public String getOutgoingGuard(ActivityEdge edge) {
        return PokaYokeUmlProfileUtil.getOutgoingGuard(edge);
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setGuard(String) guard} property
     * for <code>element</code>.
     * <p>
     * The {@link FormalElement} stereotype is removed if <code>newValue</code> is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #getEffects(RedefinableElement)} also is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #isAtomic(RedefinableElement)} is <code>false</code>.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setGuard(RedefinableElement element, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        if (Strings.isNullOrEmpty(newValue) && !(element instanceof ActivityEdge)) {
            String effects = getEffects(element);
            boolean atomic = isAtomic(element);
            if (Strings.isNullOrEmpty(effects) && !atomic) {
                PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
                // is used in the odesign file. So we trigger a refresh here.
                refresh(element);
                return;
            }
        }
        // Empty values are not allowed, so reset the value
        PokaYokeUmlProfileUtil.setGuard(element, Strings.emptyToNull(newValue));
    }

    public void setIncomingGuard(ActivityEdge edge, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(edge);

        // Empty values are not allowed, so reset the value.
        PokaYokeUmlProfileUtil.setIncomingGuard(edge, Strings.emptyToNull(newValue));
    }

    public void setOutgoingGuard(ActivityEdge edge, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(edge);

        // Empty values are not allowed, so reset the value to true.
        PokaYokeUmlProfileUtil.setOutgoingGuard(edge, Strings.emptyToNull(newValue));
    }

    public void unsetGuard(RedefinableElement element) {
        setGuard(element, null);
    }

    public boolean isSetGuard(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isSetGuard(element);
    }

    /**
     * Returns the {@link FormalElement#getEffects() effects} property value if <code>element</code> is stereotyped,
     * <code>null</code> otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#getEffects() effects} property value if <code>element</code> is stereotyped,
     *     <code>null</code> otherwise.
     */
    public String getEffects(RedefinableElement element) {
        return Joiner.on(EFFECTS_SEPARATOR).join(PokaYokeUmlProfileUtil.getEffects(element));
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#getEffects() effects} property for
     * <code>element</code>.
     * <p>
     * The {@link FormalElement} stereotype is removed if <code>newValue</code> is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #getGuard(RedefinableElement)} also is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #isAtomic(RedefinableElement)} is <code>false</code>.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setEffects(RedefinableElement element, String newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        if (Strings.isNullOrEmpty(newValue)) {
            String guard = getGuard(element);
            boolean atomic = isAtomic(element);
            if (Strings.isNullOrEmpty(guard) && !atomic) {
                PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
                // is used in the odesign file. So we trigger the refresh here.
                refresh(element);
                return;
            }
        }
        if (Strings.isNullOrEmpty(newValue)) {
            // Empty values are not allowed, so reset the value
            PokaYokeUmlProfileUtil.setEffects(element, null);
        } else {
            PokaYokeUmlProfileUtil.setEffects(element, Splitter.on(EFFECTS_SEPARATOR).splitToList(newValue));
        }
    }

    public void unsetEffects(RedefinableElement element) {
        setEffects(element, null);
    }

    public boolean isSetEffects(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isSetEffects(element);
    }

    /**
     * Returns the {@link FormalElement#isAtomic() atomic} property value if <code>element</code> is stereotyped,
     * <code>false</code> otherwise.
     *
     * @param element The element to interrogate.
     * @return The {@link FormalElement#isAtomic() atomic} property value if <code>element</code> is stereotyped,
     *     <code>false</code> otherwise.
     */
    public boolean isAtomic(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.isAtomic(element);
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setAtomic(boolean) atomic}
     * property for <code>element</code>.
     * <p>
     * The {@link FormalElement} stereotype is removed if <code>newValue</code> is <code>null</code> or
     * <code>false</code> and {@link #getGuard(RedefinableElement)} is <code>null</code> or {@link String#isEmpty()
     * empty} and {@link #getEffects(RedefinableElement)} also is <code>null</code> or {@link String#isEmpty() empty}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public void setAtomic(RedefinableElement element, Boolean newValue) {
        PokaYokeUmlProfileUtil.applyPokaYokeProfile(element);
        if (newValue == null || !newValue) {
            String guard = getGuard(element);
            String effects = getEffects(element);
            if (Strings.isNullOrEmpty(guard) && Strings.isNullOrEmpty(effects)) {
                PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                // Unapplying the stereotype does not refresh the viewer, not even when 'associated elements expression'
                // is used in the odesign file. So we trigger the refresh here.
                refresh(element);
                return;
            }
        }
        PokaYokeUmlProfileUtil.setAtomic(element, newValue);
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
        if (!label.isEmpty()) {
            label += System.getProperty("line.separator");
        }

        String incomingGuard = getIncomingGuard(controlFlow);
        if (Strings.isNullOrEmpty(incomingGuard)) {
            incomingGuard = "true";
        }
        String outgoingGuard = getOutgoingGuard(controlFlow);
        if (Strings.isNullOrEmpty(outgoingGuard)) {
            outgoingGuard = "true";
        }

        label += incomingGuard + " / " + outgoingGuard;
        return label;
    }

    /**
     * Overrides the {@link LabelServices#editUmlLabel(Element, String) editUmlLabel} method in UML Designer. This
     * implementation changes only the name of an 'ActivityEdge' without altering its guard. The override occurs
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
        }

        EditLabelSwitch editLabel = new EditLabelSwitch();
        editLabel.setEditedLabelContent(editedLabelContent);
        return editLabel.doSwitch(context);
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
}
