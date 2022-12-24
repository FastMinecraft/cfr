package org.benf.cfr.reader.bytecode.analysis.types;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;

import it.unimi.dsi.fastutil.objects.ObjectList;

public record ClassSignature(ObjectList<FormalTypeParameter> formalTypeParameters, JavaTypeInstance superClass,
                             ObjectList<JavaTypeInstance> interfaces) implements TypeUsageCollectable {

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
        ObjectList<JavaTypeInstance> typeParameterNames = new ObjectArrayList<>();
        for (FormalTypeParameter formalTypeParameter : formalTypeParameters) {
            typeParameterNames.add(new JavaGenericPlaceholderTypeInstance(formalTypeParameter.getName(), cp));
        }
        return new JavaGenericRefTypeInstance(nonGenericInstance, typeParameterNames);
    }
}
