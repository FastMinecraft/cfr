package org.benf.cfr.reader.entities.attributes;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entityfactories.AttributeFactory;
import org.benf.cfr.reader.entityfactories.ContiguousEntityFactory;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeRecord extends Attribute {
    public static final String ATTRIBUTE_NAME = "Record";

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_REMAINDER = 6;

    public record RecordComponentInfo(String name, String descriptor, ObjectList<Attribute> attributes) {

        @Override
        public ObjectList<Attribute> attributes() {
                // Prevent accidental modification
                return ObjectLists.unmodifiable(attributes);
            }
        }

    private final int length;
    private final ObjectList<RecordComponentInfo> componentInfos;

    public AttributeRecord(ByteData raw, ConstantPool cp, ClassFileVersion classFileVersion) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        int numComponents = raw.getU2At(OFFSET_OF_REMAINDER);
        long offset = OFFSET_OF_REMAINDER + 2;

        componentInfos = new ObjectArrayList<>();
        for (int i = 0; i < numComponents; i++) {
            int nameIndex = raw.getS2At(offset);
            offset += 2;
            String name = cp.getUTF8Entry(nameIndex).getValue();

            int descriptorIndex = raw.getS2At(offset);
            offset += 2;
            String descriptor = cp.getUTF8Entry(descriptorIndex).getValue();

            int attributesCount = raw.getS2At(offset);
            offset += 2;

            ObjectList<Attribute> attributes = new ObjectArrayList<>();
            raw = raw.getOffsetData(offset);
            offset = ContiguousEntityFactory.build(raw, attributesCount, attributes,
                    AttributeFactory.getBuilder(cp, classFileVersion));

            componentInfos.add(new RecordComponentInfo(name, descriptor, attributes));
        }
    }

    public ObjectList<Attribute> getRecordComponentAttributes(String componentName) {
        for (RecordComponentInfo componentInfo : componentInfos) {
            if (componentInfo.name().equals(componentName)) {
                return componentInfo.attributes();
            }
        }
        return ObjectLists.emptyList();
    }

    @Override
    public long getRawByteLength() {
        return OFFSET_OF_REMAINDER + length;
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }
}
