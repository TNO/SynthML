
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.uml2.uml.OpaqueAction;

public class ConvertExpressionToText {
    private Map<DiscVariable, EObject> discVariableToParent = new LinkedHashMap<>();

    private Map<EnumDecl, EObject> enumDeclToParent = new LinkedHashMap<>();

    /**
     * Convert CIF guard expressions into CIF expression texts.
     *
     * @param cifSpec The CIF specification.
     * @param actionToExpression The map from opaque actions to expressions.
     * @return A map from opaque actions to CIF expression texts.
     */
    public Map<OpaqueAction, String> convert(Specification cifSpec, Map<OpaqueAction, Expression> actionToExpression) {
        CheckGuard checkGuard = new CheckGuard();
        actionToExpression.values().stream().forEach(expression -> checkGuard.check(expression));

        // Move the declarations to the upper layer.
        moveVariables(cifSpec);

        // Convert the expressions to texts.
        Map<OpaqueAction, String> choiceActionToGuardText = new LinkedHashMap<>();
        actionToExpression.forEach(
                (action, expression) -> choiceActionToGuardText.put(action, CifTextUtils.exprToStr(expression)));

        // Move the declarations back to their original scopes. This may change the order of the declarations.
        revertVariableMove();

        return choiceActionToGuardText;
    }

    private void moveVariables(Specification cifSpec) {
        List<Declaration> declarations = CifCollectUtils.collectDeclarations(cifSpec, new ArrayList<>());

        declarations.stream().filter(DiscVariable.class::isInstance).map(DiscVariable.class::cast)
                .forEach(v -> discVariableToParent.put(v, v.eContainer()));

        declarations.stream().filter(EnumDecl.class::isInstance).map(EnumDecl.class::cast)
                .forEach(v -> enumDeclToParent.put(v, v.eContainer()));

        discVariableToParent.keySet().stream().forEach(decalaration -> cifSpec.getDeclarations().add(decalaration));
        enumDeclToParent.keySet().stream().forEach(decalaration -> cifSpec.getDeclarations().add(decalaration));
    }

    private void revertVariableMove() {
        discVariableToParent.entrySet()
                .forEach(e -> ((ComplexComponent)e.getValue()).getDeclarations().add(e.getKey()));
    }
}
