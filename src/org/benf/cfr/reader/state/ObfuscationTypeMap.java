package org.benf.cfr.reader.state;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.function.Function;

public interface ObfuscationTypeMap {
    boolean providesInnerClassInfo();

    JavaTypeInstance get(JavaTypeInstance type);

    Function<JavaTypeInstance, JavaTypeInstance> getter();

    ObjectList<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType);
}
