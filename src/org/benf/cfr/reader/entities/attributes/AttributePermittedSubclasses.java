package org.benf.cfr.reader.entities.attributes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class AttributePermittedSubclasses extends Attribute {
    public static final String ATTRIBUTE_NAME = "PermittedSubclasses";

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_REMAINDER = 6;
    private static final long OFFSET_OF_ENTRY_COUNT = 6;
    private static final long OFFSET_OF_ENTRIES = 8;

    private final int length;
    private final ObjectList<JavaTypeInstance> entries;

    public AttributePermittedSubclasses(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        int numEntries = raw.getU2At(OFFSET_OF_ENTRY_COUNT);
        this.entries = new ObjectArrayList<>();
        long offset = OFFSET_OF_ENTRIES;
        for (int x = 0; x < numEntries; ++x) {
            int entryIdx = raw.getU2At(offset);
            ConstantPoolEntryClass cpec = cp.getClassEntry(entryIdx);
            entries.add(cpec.getTypeInstance());
            offset += 2;
        }
    }

    public ObjectList<JavaTypeInstance> getPermitted() {
        return entries;
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }

    @Override
    public long getRawByteLength() {
        return OFFSET_OF_REMAINDER + length;
    }

    @Override
    public String toString() {
        return ATTRIBUTE_NAME;
    }
}
