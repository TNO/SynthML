
package com.github.tno.pokayoke.transform.gal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import fr.lip6.move.gal.ConstParameter;
import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.GALTypeDeclaration;
import fr.lip6.move.gal.Specification;
import fr.lip6.move.gal.TypedefDeclaration;

/** Builder to conveniently construct {@link Specification GAL specifications}. */
public class GalSpecificationBuilder {
    private final Specification specification = GalTranslationHelper.FACTORY.createSpecification();

    private final Map<String, ConstParameter> paramMapping = new LinkedHashMap<>();

    private final Map<String, GALTypeDeclaration> typeMapping = new LinkedHashMap<>();

    private final Map<String, TypedefDeclaration> typedefMapping = new LinkedHashMap<>();

    public ConstParameter addParam(String name, int defaultValue) {
        Preconditions.checkNotNull(name, "Expected a non-null parameter name.");
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        ConstParameter param = GalTranslationHelper.FACTORY.createConstParameter();
        param.setName("$" + name);
        param.setValue(defaultValue);
        return addParam(param);
    }

    public ConstParameter addParam(ConstParameter param) {
        Preconditions.checkNotNull(param, "Expected a non-null parameter.");
        Preconditions.checkArgument(!specification.getParams().contains(param), "Parameter already declared: " + param);
        String name = param.getName();
        Preconditions.checkArgument(!paramMapping.containsKey(name), "Duplicate parameter name: " + name);
        specification.getParams().add(param);
        paramMapping.put(name, param);
        return param;
    }

    public GALTypeDeclaration addType(GALTypeDeclaration typeDecl) {
        Preconditions.checkNotNull(typeDecl, "Expected a non-null type.");
        Preconditions.checkArgument(!specification.getTypes().contains(typeDecl), "Type already declared: " + typeDecl);
        String name = typeDecl.getName();
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        Preconditions.checkArgument(!typeMapping.containsKey(name), "Duplicate type name: " + name);
        specification.getTypes().add(typeDecl);
        typeMapping.put(name, typeDecl);
        return typeDecl;
    }

    public TypedefDeclaration addTypedef(String name, int minValue, int maxValue) {
        Preconditions.checkNotNull(name, "Expected a non-null typedef name.");
        Preconditions.checkArgument(minValue <= maxValue, "Expected the given min value to not exceed the max value.");
        TypedefDeclaration typedef = GalTranslationHelper.FACTORY.createTypedefDeclaration();
        typedef.setName(name);
        Constant typedefMinValue = GalTranslationHelper.FACTORY.createConstant();
        typedefMinValue.setValue(minValue);
        typedef.setMin(typedefMinValue);
        Constant typedefMaxValue = GalTranslationHelper.FACTORY.createConstant();
        typedefMaxValue.setValue(maxValue);
        typedef.setMax(typedefMaxValue);
        return addTypedef(typedef);
    }

    public TypedefDeclaration addTypedef(TypedefDeclaration typedef) {
        Preconditions.checkNotNull(typedef, "Expected a non-null typedef.");
        Preconditions.checkArgument(!specification.getTypedefs().contains(typedef),
                "Typedef already declared: " + typedef);
        String name = typedef.getName();
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        Preconditions.checkArgument(!typedefMapping.containsKey(name), "Duplicate typedef name: " + name);
        specification.getTypedefs().add(typedef);
        typedefMapping.put(name, typedef);
        return typedef;
    }

    public ConstParameter getParam(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null parameter name.");
        GalTranslationHelper.ensureNameDoesNotContainDollarSign(name);
        return paramMapping.get("$" + name);
    }

    public GALTypeDeclaration getType(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null type name.");
        return typeMapping.get(name);
    }

    public TypedefDeclaration getTypedef(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null typedef name.");
        return typedefMapping.get(name);
    }

    public void setMain(GALTypeDeclaration typeDecl) {
        Preconditions.checkNotNull(typeDecl, "Expected a non-null type.");
        specification.setMain(typeDecl);
    }

    public Specification build() {
        return specification;
    }
}
