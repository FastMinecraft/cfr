package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.DeclarationAnnotationHelper.DeclarationAnnotationsInfo;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class MethodPrototypeAnnotationsHelper {
    private final AttributeMap attributeMap;
    private final TypeAnnotationHelper typeAnnotationHelper;

    public MethodPrototypeAnnotationsHelper(AttributeMap attributes) {
        this.attributeMap = attributes;
        this.typeAnnotationHelper = TypeAnnotationHelper.create(attributes,
                TypeAnnotationEntryValue.type_generic_method_constructor,
                TypeAnnotationEntryValue.type_ret_or_new,
                TypeAnnotationEntryValue.type_receiver,
                TypeAnnotationEntryValue.type_throws,
                TypeAnnotationEntryValue.type_formal
                );
    }

    static void dumpAnnotationTableEntries(ObjectList<? extends AnnotationTableEntry> annotationTableEntries, Dumper d) {
        for (AnnotationTableEntry annotation : annotationTableEntries) {
            annotation.dump(d).print(' ');
        }
    }

    public ObjectList<AnnotationTableTypeEntry> getMethodReturnAnnotations() {
        return getTypeTargetAnnotations(TypeAnnotationEntryValue.type_ret_or_new);
    }

    // TODO: Linear scans here, could be replaced with index.
    public ObjectList<AnnotationTableTypeEntry> getTypeTargetAnnotations(final TypeAnnotationEntryValue target) {
        if (typeAnnotationHelper == null) return null;
        ObjectList<AnnotationTableTypeEntry> res = Functional.filter(typeAnnotationHelper.getEntries(),
            in -> in.getValue() == target
        );
        if (res.isEmpty()) return null;
        return res;
    }

    public ObjectList<AnnotationTableEntry> getMethodAnnotations() {
        return MiscAnnotations.BasicAnnotations(attributeMap);
    }

    private ObjectList<AnnotationTableEntry> getParameterAnnotations(int idx) {
        AttributeRuntimeVisibleParameterAnnotations a1 = attributeMap.getByName(AttributeRuntimeVisibleParameterAnnotations.ATTRIBUTE_NAME);
        AttributeRuntimeInvisibleParameterAnnotations a2 = attributeMap.getByName(AttributeRuntimeInvisibleParameterAnnotations.ATTRIBUTE_NAME);
        ObjectList<AnnotationTableEntry> e1 = a1 == null ? null : a1.getAnnotationsForParamIdx(idx);
        ObjectList<AnnotationTableEntry> e2 = a2 == null ? null : a2.getAnnotationsForParamIdx(idx);
        return ListFactory.combinedOptimistic(e1,e2);
    }

    private ObjectList<AnnotationTableTypeEntry> getTypeParameterAnnotations(final int paramIdx) {
        ObjectList<AnnotationTableTypeEntry> typeEntries = getTypeTargetAnnotations(TypeAnnotationEntryValue.type_formal);
        if (typeEntries == null) return null;
        typeEntries = Functional.filter(typeEntries,
            in -> ((TypeAnnotationTargetInfo.TypeAnnotationFormalParameterTarget)in.getTargetInfo()).getIndex() == paramIdx
        );
        if (typeEntries.isEmpty()) return null;
        return typeEntries;
    }

    public void dumpParamType(JavaTypeInstance arg, final int paramIdx, Dumper d) {
        ObjectList<AnnotationTableEntry> entries = getParameterAnnotations(paramIdx);
        ObjectList<AnnotationTableTypeEntry> typeEntries = getTypeParameterAnnotations(paramIdx);
        DeclarationAnnotationsInfo annotationsInfo = DeclarationAnnotationHelper.getDeclarationInfo(arg, entries, typeEntries);
        /*
         * TODO: This is incorrect, but currently cannot easily influence whether the dumped type is admissible
         * Therefore assume it is always admissible unless required not to
         * (even though then the dumped type might still be admissible)
         */
        boolean usesAdmissibleType = !annotationsInfo.requiresNonAdmissibleType();
        ObjectList<AnnotationTableEntry> declAnnotationsToDump = annotationsInfo.getDeclarationAnnotations(usesAdmissibleType);
        ObjectList<AnnotationTableTypeEntry> typeAnnotationsToDump = annotationsInfo.getTypeAnnotations(usesAdmissibleType);

        dumpAnnotationTableEntries(declAnnotationsToDump, d);

        if (typeAnnotationsToDump.isEmpty()) {
            d.dump(arg);
        } else {
            JavaAnnotatedTypeInstance jat = arg.getAnnotatedInstance();
            DecompilerComments comments = new DecompilerComments();
            TypeAnnotationHelper.apply(jat, typeAnnotationsToDump, comments);
            d.dump(comments);
            d.dump(jat);
        }
    }
}
