package org.benf.cfr.reader.mapping;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;
import java.util.function.Function;

public class NullMapping implements ObfuscationMapping {
    public static final NullMapping INSTANCE = new NullMapping();

    private static final Function<JavaTypeInstance, JavaTypeInstance> id = arg -> arg;

    @Override
    public Function<JavaTypeInstance, JavaTypeInstance> getter() {
        return id;
    }

    @Override
    public boolean providesInnerClassInfo() {
        return false;
    }

    @Override
    public Dumper wrap(Dumper d) {
        return d;
    }

    @Override
    public JavaTypeInstance get(JavaTypeInstance t) {
        return t;
    }

    @Override
    public List<JavaTypeInstance> get(List<JavaTypeInstance> types) {
        return types;
    }

    @Override
    public List<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType) {
        return null;
    }
}
