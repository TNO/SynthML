/**
 */

package PokaYoke.impl;

import PokaYoke.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc --> An implementation of the model <b>Factory</b>. <!-- end-user-doc -->
 * 
 * @generated
 */
public class PokaYokeFactoryImpl extends EFactoryImpl implements PokaYokeFactory {
    /**
     * Creates the default factory implementation. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    public static PokaYokeFactory init() {
        try {
            PokaYokeFactory thePokaYokeFactory = (PokaYokeFactory)EPackage.Registry.INSTANCE
                    .getEFactory(PokaYokePackage.eNS_URI);
            if (thePokaYokeFactory != null) {
                return thePokaYokeFactory;
            }
        } catch (Exception exception) {
            EcorePlugin.INSTANCE.log(exception);
        }
        return new PokaYokeFactoryImpl();
    }

    /**
     * Creates an instance of the factory. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    public PokaYokeFactoryImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public EObject create(EClass eClass) {
        switch (eClass.getClassifierID()) {
            case PokaYokePackage.GUARD_EFFECTS_ACTION:
                return createGuardEffectsAction();
            default:
                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public GuardEffectsAction createGuardEffectsAction() {
        GuardEffectsActionImpl guardEffectsAction = new GuardEffectsActionImpl();
        return guardEffectsAction;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public PokaYokePackage getPokaYokePackage() {
        return (PokaYokePackage)getEPackage();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @deprecated
     * @generated
     */
    @Deprecated
    public static PokaYokePackage getPackage() {
        return PokaYokePackage.eINSTANCE;
    }
} // PokaYokeFactoryImpl
