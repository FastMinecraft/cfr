package org.benf.cfr.reader.state;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;

import java.util.List;
import java.util.function.Function;

public interface ObfuscationTypeMap {
    boolean providesInnerClassInfo();

    JavaTypeInstance get(JavaTypeInstance type);

    Function<JavaTypeInstance, JavaTypeInstance> getter();

    List<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType);
}
