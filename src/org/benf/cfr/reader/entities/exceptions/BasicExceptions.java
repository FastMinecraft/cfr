package org.benf.cfr.reader.entities.exceptions;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/*
 * This defines the set of exceptions which can be thrown by jvm instructions natively.
 */
public class BasicExceptions {
    public static final ObjectSet<? extends JavaTypeInstance> instances;

    static {
        JavaRefTypeInstance[] content = new JavaRefTypeInstance[]{ JavaRefTypeInstance.createTypeConstant("java.lang.AbstractMethodError"), JavaRefTypeInstance.createTypeConstant("java.lang.ArithmeticException"), JavaRefTypeInstance.createTypeConstant("java.lang.ArrayIndexOutOfBoundsException"), JavaRefTypeInstance.createTypeConstant("java.lang.ArrayStoreException"), JavaRefTypeInstance.createTypeConstant("java.lang.ClassCastException"), JavaRefTypeInstance.createTypeConstant("java.lang.IllegalAccessError"), JavaRefTypeInstance.createTypeConstant("java.lang.IllegalMonitorStateException"), JavaRefTypeInstance.createTypeConstant("java.lang.IncompatibleClassChangeError"), JavaRefTypeInstance.createTypeConstant("java.lang.InstantiationError"), JavaRefTypeInstance.createTypeConstant("java.lang.NegativeArraySizeException"), JavaRefTypeInstance.createTypeConstant("java.lang.NullPointerException"), JavaRefTypeInstance.createTypeConstant("java.lang.UnsatisfiedLinkError") };
        instances = ObjectSet.of(content);
    }
}
