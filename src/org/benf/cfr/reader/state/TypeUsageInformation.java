package org.benf.cfr.reader.state;

import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface TypeUsageInformation {
    JavaRefTypeInstance getAnalysisType();

    ObjectSet<JavaRefTypeInstance> getShortenedClassTypes();

    ObjectSet<JavaRefTypeInstance> getUsedClassTypes();

    ObjectSet<JavaRefTypeInstance> getUsedInnerClassTypes();

    boolean hasLocalInstance(JavaRefTypeInstance type);

    String getName(JavaTypeInstance type, TypeContext typeContext);

    boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext);

    String generateInnerClassShortName(JavaRefTypeInstance clazz);

    String generateOverriddenName(JavaRefTypeInstance clazz);

    IllegalIdentifierDump getIid();

    boolean isStaticImport(JavaTypeInstance clazz, String fixedName);

    ObjectSet<DetectedStaticImport> getDetectedStaticImports();
}
