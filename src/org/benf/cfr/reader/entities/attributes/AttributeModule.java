package org.benf.cfr.reader.entities.attributes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryModuleInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Set;
import java.util.TreeSet;

public class AttributeModule extends Attribute {
    public static final String ATTRIBUTE_NAME = "Module";

    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2;
    private static final long OFFSET_OF_MODULE_NAME = 6;
    private static final long OFFSET_OF_MODULE_FLAGS = 8;
    private static final long OFFSET_OF_MODULE_VERSION = 10;
    private static final long OFFSET_OF_DYNAMIC_INFO = 12;
    private final int nameIdx;
    private final int flags;
    private final int versionIdx;
    private final ObjectList<Require> requires;
    private final ObjectList<ExportOpen> exports;
    private final ObjectList<ExportOpen> opens;
    private final ObjectList<Use> uses;
    private final ObjectList<Provide> provides;

    public Set<ModuleFlags> getFlags() {
        return ModuleFlags.build(flags);
    }

    public enum ModuleFlags {
        OPEN("open"),
        SYNTHETIC("/* synthetic */"),
        MANDATED("/* mandated */");

        private final String comment;

        ModuleFlags(String comment) {
            this.comment = comment;
        }

        public static Set<ModuleFlags> build(int raw)
        {
            Set<ModuleFlags> res = new TreeSet<>();
            if (0 != (raw & 0x20)) res.add(OPEN);
            if (0 != (raw & 0x1000)) res.add(SYNTHETIC);
            if (0 != (raw & 0x8000)) res.add(MANDATED);
            return res;
        }

        @Override
        public String toString() {
            return comment;
        }
    }

    public enum ModuleContentFlags {
        TRANSITIVE("transitive"),
        STATIC_PHASE("static"),
        SYNTHETIC("/* synthetic */"),
        MANDATED("/* mandated */");

        private final String comment;

        ModuleContentFlags(String comment) {
            this.comment = comment;
        }

        public static Set<ModuleContentFlags> build(int raw)
        {
            Set<ModuleContentFlags> res = new TreeSet<>();
            if (0 != (raw & 0x20)) res.add(TRANSITIVE);
            if (0 != (raw & 0x40)) res.add(STATIC_PHASE);
            if (0 != (raw & 0x1000)) res.add(SYNTHETIC);
            if (0 != (raw & 0x8000)) res.add(MANDATED);
            return res;
        }

        @Override
        public String toString() {
            return comment;
        }
    }

    public static class Require {
        private final int index;
        private final int flags;
        private final int version_index;

        /**
         * Gets the index of the referenced {@link ConstantPoolEntryModuleInfo}.
         */
        public int getIndex() {
            return index;
        }

        public Set<ModuleContentFlags> getFlags() {
            return ModuleContentFlags.build(flags);
        }

        /**
         * Gets the index of the referenced {@link ConstantPoolEntryUTF8}, or 0 if no
         * version information is specified.
         */
        public int getVersionIndex() {
            return version_index;
        }

        private Require(int index, int flags, int version_index) {
            this.index = index;
            this.flags = flags;
            this.version_index = version_index;
        }

        private static long read(ByteData raw, long offset, ObjectList<Require> tgt) {
            int num = raw.getU2At(offset);
            offset += 2;
            for (int x=0;x<num;++x) {
                tgt.add(new Require(raw.getU2At(offset), raw.getU2At(offset+2), raw.getU2At(offset+4)));
                offset += 6;
            }
            return offset;
        }
    }

    public static class ExportOpen {
        private final int index;
        private final int flags;
        private final int[] to_index;

        private ExportOpen(int index, int flags, int[] to_index) {
            this.index = index;
            this.flags = flags;
            this.to_index = to_index;
        }

        public Set<ModuleContentFlags> getFlags() {
            return ModuleContentFlags.build(flags);
        }

        public int getIndex() {
            return index;
        }

        public int[] getToIndex() {
            return to_index;
        }

        private static long read(ByteData raw, long offset, ObjectList<ExportOpen> tgt) {
            int num = raw.getU2At(offset);
            offset += 2;
            for (int x=0;x<num;++x) {
                int index = raw.getU2At(offset);
                int flags = raw.getU2At(offset+2);
                int count = raw.getU2At(offset+4);
                offset += 6;
                int[] indices = new int[count];
                for (int y=0;y<count;++y) {
                    indices[y] = raw.getU2At(offset);
                    offset += 2;
                }
                tgt.add(new ExportOpen(index, flags, indices));
            }
            return offset;
        }

    }

    public static class Use {
        final int index;

        private Use(int index) {
            this.index = index;
        }

        private static long read(ByteData raw, long offset, ObjectList<Use> tgt) {
            int num = raw.getU2At(offset);
            offset += 2;
            for (int x=0;x<num;++x) {
                int index = raw.getU2At(offset);
                tgt.add(new Use(index));
                offset += 2;
            }
            return offset;
        }

        public int getIndex() {
            return index;
        }
    }

    public static class Provide {
        private final int index;
        private final int[] with_index;

        private Provide(int index, int[] with_index) {
            this.index = index;
            this.with_index = with_index;
        }

        public int getIndex() {
            return index;
        }

        public int[] getWithIndex() {
            return with_index;
        }

        private static void read(ByteData raw, long offset, ObjectList<Provide> tgt) {
            int num = raw.getU2At(offset);
            offset += 2;
            for (int x=0;x<num;++x) {
                int index = raw.getU2At(offset);
                int count = raw.getU2At(offset+2);
                offset += 4;
                int[] indices = new int[count];
                for (int y=0;y<count;++y) {
                    indices[y] = raw.getU2At(offset);
                    offset += 2;
                }
                tgt.add(new Provide(index, indices));
            }
        }

    }

    // requires, exports, opens, uses and provides are dynamically sized.

    private final int length;
    private final ConstantPool cp;

    public AttributeModule(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(OFFSET_OF_ATTRIBUTE_LENGTH);
        this.cp = cp;
        this.nameIdx = raw.getU2At(OFFSET_OF_MODULE_NAME);
        this.flags = raw.getU2At(OFFSET_OF_MODULE_FLAGS);
        this.versionIdx = raw.getU2At(OFFSET_OF_MODULE_VERSION);
        long offset = OFFSET_OF_DYNAMIC_INFO;
        this.requires = new ObjectArrayList<>();
        this.exports = new ObjectArrayList<>();
        this.opens = new ObjectArrayList<>();
        this.uses = new ObjectArrayList<>();
        this.provides = new ObjectArrayList<>();
        offset = Require.read(raw, offset, this.requires);
        offset = ExportOpen.read(raw, offset, this.exports);
        offset = ExportOpen.read(raw, offset, this.opens);
        offset = Use.read(raw, offset, this.uses);
        Provide.read(raw, offset, this.provides);
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
        return OFFSET_OF_MODULE_FLAGS + length;
    }

    @Override
    public String toString() {
        return ATTRIBUTE_NAME;
    }

    public ObjectList<Require> getRequires() {
        return requires;
    }

    public ObjectList<ExportOpen> getExports() {
        return exports;
    }

    public ObjectList<ExportOpen> getOpens() {
        return opens;
    }

    public ObjectList<Use> getUses() {
        return uses;
    }

    public ObjectList<Provide> getProvides() {
        return provides;
    }

    public ConstantPool getCp() {
        return cp;
    }

    public String getModuleName() {
        return ((ConstantPoolEntryModuleInfo)cp.getEntry(nameIdx)).getName().getValue();
    }

    /**
     * Gets the module version, or {@code null} if not specified.
     */
    public String getModuleVersion() {
        if (versionIdx == 0) {
            return null;
        }
        return cp.getUTF8Entry(versionIdx).getValue();
    }
}
