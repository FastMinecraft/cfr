package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class TypeUsageInformationEmpty implements TypeUsageInformation {
    public static final TypeUsageInformation INSTANCE = new TypeUsageInformationEmpty();

    @Override
    public JavaRefTypeInstance getAnalysisType() {
        return null;
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return null;
    }

    @Override
    public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
        return false;
    }

    @Override
    public ObjectSet<DetectedStaticImport> getDetectedStaticImports() {
        return ObjectSets.emptySet();
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getUsedClassTypes() {
        return new ObjectLinkedOpenHashSet<>();
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return new ObjectLinkedOpenHashSet<>();
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getShortenedClassTypes() {
        return new ObjectLinkedOpenHashSet<>();
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        return type.getRawName();
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return false;
    }

    @Override
    public String generateOverriddenName(JavaRefTypeInstance clazz) {
        return clazz.getRawName();
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return clazz.getRawName();
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return false;
    }
}
