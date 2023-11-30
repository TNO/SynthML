
package com.github.tno.pokayoke.transform.uml2gal;

import java.util.Collection;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.And;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.GalFactory;

public class Uml2GalTranslationHelper {
    static final GalFactory FACTORY = GalFactory.eINSTANCE;

    private Uml2GalTranslationHelper() {
    }

    static And combineAsAnd(BooleanExpression left, BooleanExpression right) {
        And conjunction = Uml2GalTranslationHelper.FACTORY.createAnd();
        conjunction.setLeft(left);
        conjunction.setRight(right);
        return conjunction;
    }

    static BooleanExpression combineAsAnd(Collection<BooleanExpression> exprs) {
        return exprs.stream().reduce(Uml2GalTranslationHelper::combineAsAnd)
                .orElse(Uml2GalTranslationHelper.FACTORY.createTrue());
    }

    static void ensureNameDoesNotContainDollarSign(String name) {
        Preconditions.checkArgument(!name.contains("$"), "Expected a name not containing '$', but got: " + name);
    }
}
