package org.benf.cfr.reader.entities.attributes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;

public abstract class AttributeTypeAnnotations extends Attribute {

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_REMAINDER = 6;
    private static final long OFFSET_OF_NUMBER_OF_ANNOTATIONS = 6;
    private static final long OFFSET_OF_ANNOTATION_TABLE = 8;
    private final Map<TypeAnnotationEntryValue, ObjectList<AnnotationTableTypeEntry>> annotationTableEntryData = MapFactory.newMap();

    private final int length;


    AttributeTypeAnnotations(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        int numAnnotations = raw.getU2At(OFFSET_OF_NUMBER_OF_ANNOTATIONS);
        long offset = OFFSET_OF_ANNOTATION_TABLE;

        Map<TypeAnnotationEntryValue, ObjectList<AnnotationTableTypeEntry>> entryData = MapFactory.newLazyMap(annotationTableEntryData,
            arg -> new ObjectArrayList<>()
        );

        for (int x = 0; x < numAnnotations; ++x) {
            Pair<Long, AnnotationTableTypeEntry> ape = AnnotationHelpers.getTypeAnnotation(raw, offset, cp);
            offset = ape.getFirst();
            AnnotationTableTypeEntry entry = ape.getSecond();
            entryData.get(entry.getValue()).add(entry);
        }
    }

    @Override
    public Dumper dump(Dumper d) {
        for (ObjectList<AnnotationTableTypeEntry> annotationTableEntryList : annotationTableEntryData.values()) {
            for (AnnotationTableTypeEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.dump(d);
                d.newln();
            }
        }
        return d;
    }


    @Override
    public long getRawByteLength() {
        return OFFSET_OF_REMAINDER + length;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ObjectList<AnnotationTableTypeEntry> annotationTableEntryList : annotationTableEntryData.values()) {
            for (AnnotationTableTypeEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.collectTypeUsages(collector);
            }
        }
    }

    public ObjectList<AnnotationTableTypeEntry> getAnnotationsFor(TypeAnnotationEntryValue ... types) {
        ObjectList<AnnotationTableTypeEntry> res = null;
        boolean orig = true;
        for (TypeAnnotationEntryValue type : types) {
            ObjectList<AnnotationTableTypeEntry> items = annotationTableEntryData.get(type);
            if (items == null) {
                continue;
            }
            if (orig) {
                res = items;
                orig = false;
            } else {
                res = new ObjectArrayList<>(res);
                res.addAll(items);
            }
        }
        return res;
    }
}
