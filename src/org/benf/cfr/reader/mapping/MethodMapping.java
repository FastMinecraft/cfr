package org.benf.cfr.reader.mapping;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class MethodMapping {
    private final String name;
    private final String rename;
    private final JavaTypeInstance res;
    private final ObjectList<JavaTypeInstance> argTypes;

    public MethodMapping(String rename, String name, JavaTypeInstance res, ObjectList<JavaTypeInstance> argTypes) {
        this.name = name;
        this.rename = rename;
        this.res = res;
        this.argTypes = argTypes;
    }

    public String getName() {
        return name;
    }

    public String getRename() {
        return rename;
    }

    public JavaTypeInstance getResultType() {
        return res;
    }

    public ObjectList<JavaTypeInstance> getArgTypes() {
        return argTypes;
    }
}
