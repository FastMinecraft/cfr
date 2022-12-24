package org.benf.cfr.reader.bytecode.analysis.loc;

import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.benf.cfr.reader.entities.Method;

import java.util.Collection;
import java.util.Collections;

class BytecodeLocSpecific extends BytecodeLoc {
    enum Specific {
        DISABLED,
        TODO,
        NONE
    }

    private final Specific type;

    BytecodeLocSpecific(Specific type) {
        this.type = type;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
    }

    @Override
    public String toString() {
        return type.name();
    }

    @Override
    public Collection<Method> getMethods() {
        return ObjectLists.emptyList();
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return ObjectLists.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
