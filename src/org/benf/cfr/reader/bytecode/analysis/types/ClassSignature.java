package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.TypeUsageCollectable;

import java.util.List;

public record ClassSignature(List<FormalTypeParameter> formalTypeParameters, JavaTypeInstance superClass,
                             List<JavaTypeInstance> interfaces) implements TypeUsageCollectable {

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(superClass);
        collector.collectFrom(formalTypeParameters);
        collector.collect(interfaces);
    }

    // TODO : This is pointless.
    public JavaTypeInstance getThisGeneralTypeClass(JavaTypeInstance nonGenericInstance, ConstantPool cp) {
        if (nonGenericInstance instanceof JavaGenericBaseInstance) return nonGenericInstance;
        if (formalTypeParameters == null || formalTypeParameters.isEmpty()) return nonGenericInstance;
        List<JavaTypeInstance> typeParameterNames = ListFactory.newList();
        for (FormalTypeParameter formalTypeParameter : formalTypeParameters) {
            typeParameterNames.add(new JavaGenericPlaceholderTypeInstance(formalTypeParameter.getName(), cp));
        }
        JavaTypeInstance res = new JavaGenericRefTypeInstance(nonGenericInstance, typeParameterNames);
        return res;
    }
}
