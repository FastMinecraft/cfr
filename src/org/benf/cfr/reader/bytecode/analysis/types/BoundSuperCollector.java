package org.benf.cfr.reader.bytecode.analysis.types;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.benf.cfr.reader.entities.ClassFile;

import java.util.Map;

public class BoundSuperCollector {


    private final ClassFile classFile;
    private final Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSupers;
    private final Map<JavaRefTypeInstance, BindingSuperContainer.Route> boundSuperRoute;

    public BoundSuperCollector(ClassFile classFile) {
        this.classFile = classFile;
        this.boundSupers = new Object2ObjectLinkedOpenHashMap<>();
        this.boundSuperRoute = new Object2ObjectLinkedOpenHashMap<>();
    }

    public BindingSuperContainer getBoundSupers() {
        return new BindingSuperContainer(classFile, boundSupers, boundSuperRoute);
    }

    public void collect(JavaGenericRefTypeInstance boundBase, BindingSuperContainer.Route route) {
        JavaRefTypeInstance key = boundBase.getDeGenerifiedType();
        JavaGenericRefTypeInstance prev = boundSupers.put(key, boundBase);
        boundSuperRoute.put(key, route);

    }

    public void collect(JavaRefTypeInstance boundBase, BindingSuperContainer.Route route) {
        JavaGenericRefTypeInstance prev = boundSupers.put(boundBase, null);
        boundSuperRoute.put(boundBase, route);
    }
}
