package org.benf.cfr.reader.mapping;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageInformationImpl;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.DelegatingDumper;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class Mapping implements ObfuscationMapping {
    // NB: This is a map of *erased* types.  If they type we're reconstructing is generic, we
    // need to reconstruct it.
    private final Map<JavaTypeInstance, ClassMapping> erasedTypeMap = MapFactory.newMap();
    private final Function<JavaTypeInstance, JavaTypeInstance> getter = this::get;
    private final Options options;
    private final Map<JavaTypeInstance, ObjectList<InnerClassAttributeInfo>> innerInfo;

    Mapping(
        Options options,
        ObjectList<ClassMapping> classMappings,
        Map<JavaTypeInstance, ObjectList<InnerClassAttributeInfo>> innerInfo
    ) {
        this.options = options;
        this.innerInfo = innerInfo;
        for (ClassMapping cls : classMappings) {
            erasedTypeMap.put(cls.getObClass(), cls);
        }
    }

    @Override
    public Dumper wrap(Dumper d) {
        return new ObfuscationWrappingDumper(d);
    }

    @Override
    public boolean providesInnerClassInfo() {
        return true;
    }

    @Override
    public JavaTypeInstance get(JavaTypeInstance type) {
        if (type == null) return null;
        int numDim = type.getNumArrayDimensions();
        JavaTypeInstance strippedType = type.getArrayStrippedType();
        ClassMapping c = erasedTypeMap.get(strippedType);
        if (c == null) {
            return type;
        }
        JavaTypeInstance res = c.getRealClass();
        if (numDim > 0) {
            res = new JavaArrayTypeInstance(numDim, res);
        }
        return res;
    }

    @Override
    public ObjectList<JavaTypeInstance> get(ObjectList<JavaTypeInstance> types) {
        return Functional.map(types, this::get);
    }

    ClassMapping getClassMapping(JavaTypeInstance type) {
        return erasedTypeMap.get(type.getDeGenerifiedType());
    }

    @Override
    public ObjectList<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType) {
        return innerInfo.get(classType);
    }

    @Override
    public Function<JavaTypeInstance, JavaTypeInstance> getter() {
        return getter;
    }

    private class MappingTypeUsage implements TypeUsageInformation {
        private final TypeUsageInformation delegateRemapped;
        private final TypeUsageInformation delegateOriginal;

        private MappingTypeUsage(TypeUsageInformation delegateRemapped, TypeUsageInformation delegateOriginal) {
            this.delegateRemapped = delegateRemapped;
            this.delegateOriginal = delegateOriginal;
        }

        @Override
        public IllegalIdentifierDump getIid() {
            return delegateOriginal.getIid();
        }

        @Override
        public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
            return delegateOriginal.isStaticImport(clazz, fixedName);
        }

        @Override
        public ObjectSet<DetectedStaticImport> getDetectedStaticImports() {
            return delegateOriginal.getDetectedStaticImports();
        }

        @Override
        public JavaRefTypeInstance getAnalysisType() {
            return delegateRemapped.getAnalysisType();
        }

        @Override
        public ObjectSet<JavaRefTypeInstance> getShortenedClassTypes() {
            return delegateRemapped.getShortenedClassTypes();
        }

        @Override
        public ObjectSet<JavaRefTypeInstance> getUsedClassTypes() {
            return delegateOriginal.getUsedClassTypes();
        }

        @Override
        public ObjectSet<JavaRefTypeInstance> getUsedInnerClassTypes() {
            return delegateOriginal.getUsedClassTypes();
        }

        @Override
        public String getName(JavaTypeInstance type, TypeContext typeContext) {
            return delegateRemapped.getName(get(type), typeContext);
        }

        @Override
        public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
            return delegateRemapped.isNameClash(type, name, typeContext);
        }

        @Override
        public boolean hasLocalInstance(JavaRefTypeInstance type) {
            return delegateOriginal.hasLocalInstance(type);
        }

        @Override
        public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
            return delegateRemapped.generateInnerClassShortName((JavaRefTypeInstance) get(clazz));
        }

        @Override
        public String generateOverriddenName(JavaRefTypeInstance clazz) {
            return delegateRemapped.generateOverriddenName(clazz);
        }
    }

    private class ObfuscationWrappingDumper extends DelegatingDumper {
        private TypeUsageInformation mappingTypeUsage;

        private ObfuscationWrappingDumper(Dumper delegate) {
            super(delegate);
            this.mappingTypeUsage = null;
        }

        private ObfuscationWrappingDumper(Dumper delegate, TypeUsageInformation typeUsageInformation) {
            super(delegate);
            this.mappingTypeUsage = typeUsageInformation;
        }

        @Override
        public TypeUsageInformation getTypeUsageInformation() {
            if (mappingTypeUsage == null) {
                TypeUsageInformation dti = delegate.getTypeUsageInformation();
                Collection<JavaRefTypeInstance> content = Functional.map(
                    dti.getUsedClassTypes(),
                    arg -> (JavaRefTypeInstance) get(arg)
                );
                TypeUsageInformation dtr = new TypeUsageInformationImpl(
                    options,
                    (JavaRefTypeInstance) get(dti.getAnalysisType()),
                    new ObjectLinkedOpenHashSet<>(content),
                    new ObjectOpenHashSet<>()
                );
                mappingTypeUsage = new MappingTypeUsage(dtr, dti);
            }
            return mappingTypeUsage;
        }

        @Override
        public ObfuscationMapping getObfuscationMapping() {
            return Mapping.this;
        }

        @Override
        public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
            JavaTypeInstance classType = p == null ? null : p.getClassType();
            ClassMapping c = classType == null ? null : erasedTypeMap.get(classType.getDeGenerifiedType());
            if (c == null || special) {
                delegate.methodName(s, p, special, defines);
                return this;
            }

            delegate.methodName(
                c.getMethodName(s, p.getSignatureBoundArgs(), Mapping.this, delegate),
                p,
                special,
                defines
            );
            return this;
        }

        @Override
        public Dumper fieldName(
            String name,
            String descriptor,
            JavaTypeInstance owner,
            boolean hiddenDeclaration,
            boolean isStatic,
            boolean defines
        ) {
            JavaTypeInstance deGenerifiedType = owner.getDeGenerifiedType();
            ClassMapping c = erasedTypeMap.get(deGenerifiedType);
            if (c == null || hiddenDeclaration) {
                delegate.fieldName(name, descriptor, owner, hiddenDeclaration, isStatic, defines);
            } else {
                delegate.fieldName(
                    c.getFieldName(name, deGenerifiedType, this, Mapping.this, isStatic),
                    descriptor,
                    owner,
                    hiddenDeclaration,
                    isStatic,
                    defines
                );
            }
            return this;
        }

        @Override
        public Dumper packageName(JavaRefTypeInstance t) {
            JavaTypeInstance deGenerifiedType = t.getDeGenerifiedType();
            ClassMapping c = erasedTypeMap.get(deGenerifiedType);
            if (c == null) {
                delegate.packageName(t);
            } else {
                delegate.packageName(c.getRealClass());
            }
            return this;
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance) {
            dump(javaTypeInstance, TypeContext.None);
            return this;
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance, boolean defines) {
            return dump(javaTypeInstance);
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
            javaTypeInstance = javaTypeInstance.deObfuscate(Mapping.this);
            javaTypeInstance.dumpInto(this, getTypeUsageInformation(), typeContext);
            return this;
        }

        @Override
        public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
            return new ObfuscationWrappingDumper(delegate, innerclassTypeUsageInformation);
        }
    }
}
