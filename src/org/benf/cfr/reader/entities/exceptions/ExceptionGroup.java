package org.benf.cfr.reader.entities.exceptions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.StringUtils;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.Objects;

public class ExceptionGroup {

    private final int bytecodeIndexFrom;        // [ a
    private int bytecodeIndexTo;          // ) b    st a <= x < b
    private int minHandlerStart = Short.MAX_VALUE;
    private final ObjectList<Entry> entries = new ObjectArrayList<>();
    private final BlockIdentifier tryBlockIdentifier;
    private final ConstantPool cp;

    public ExceptionGroup(int bytecodeIndexFrom, BlockIdentifier blockIdentifier, ConstantPool cp) {
        this.bytecodeIndexFrom = bytecodeIndexFrom;
        this.tryBlockIdentifier = blockIdentifier;
        this.cp = cp;
    }

    public void add(ExceptionTableEntry entry) {
        if (entry.getBytecodeIndexHandler() == entry.getBytecodeIndexFrom()) return;
        if (entry.getBytecodeIndexHandler() < minHandlerStart) minHandlerStart = entry.getBytecodeIndexHandler();
        this.entries.add(new Entry(entry));
        if (entry.getBytecodeIndexTo() > bytecodeIndexTo) bytecodeIndexTo = entry.getBytecodeIndexTo();
//        if (byteCodeIndexTo > minHandlerStart) byteCodeIndexTo = minHandlerStart;
    }

    public ObjectList<Entry> getEntries() {
        return entries;
    }

    public int getBytecodeIndexFrom() {
        return bytecodeIndexFrom;
    }

    public int getBytecodeIndexTo() {
        return bytecodeIndexTo;
    }

    public BlockIdentifier getTryBlockIdentifier() {
        return tryBlockIdentifier;
    }

    public void removeSynchronisedHandlers(final Map<Integer, Integer> lutByOffset,
                                           final Map<Integer, Integer> lutByIdx,
                                           ObjectList<Op01WithProcessedDataAndByteJumps> instrs) {
        entries.removeIf(entry -> isSynchronisedHandler(entry, lutByOffset, lutByIdx, instrs));
    }

    private boolean isSynchronisedHandler(Entry entry,
                                          final Map<Integer, Integer> lutByOffset,
                                          final Map<Integer, Integer> lutByIdx,
                                          ObjectList<Op01WithProcessedDataAndByteJumps> instrs) {
        /*
         * TODO : Type should be 'any'.
         */
        ExceptionTableEntry tableEntry = entry.entry;

        /*
         * We expect - astore X, (aload, monitorexit)+, aload X, athrow
         */
        Integer offset = lutByOffset.get(tableEntry.getBytecodeIndexHandler());
        if (offset == null) return false;

        int idx = offset;
        if (idx >= instrs.size()) return false;

        Op01WithProcessedDataAndByteJumps start = instrs.get(idx);
        Integer catchStore = start.getAStoreIdx();
        if (catchStore == null) return false;
        idx++;
        int nUnlocks = 0;
        do {
            if (idx + 1 >= instrs.size()) return false;
            Op01WithProcessedDataAndByteJumps load = instrs.get(idx);
            Integer loadIdx = load.getALoadIdx();
            if (loadIdx == null) {
                // One alternative - ldc.
                JVMInstr instr = load.getJVMInstr();
                if (instr != JVMInstr.LDC) {
                    break;
                }
            }
            Op01WithProcessedDataAndByteJumps next = instrs.get(idx + 1);
            if (next.getJVMInstr() != JVMInstr.MONITOREXIT) break;
            nUnlocks++;
            idx += 2;
        } while (true);
        if (nUnlocks == 0) return false;
        Integer catchLoad = instrs.get(idx).getALoadIdx();
        if (!catchStore.equals(catchLoad)) return false;
        idx++;
        return instrs.get(idx).getJVMInstr() == JVMInstr.ATHROW;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[egrp ").append(tryBlockIdentifier).append(" [");
        boolean bfirst = true;
        for (Entry e : entries) {
            bfirst = StringUtils.comma(bfirst, sb);
            sb.append(e.getPriority());
        }
        sb.append(" : ").append(bytecodeIndexFrom).append("->").append(bytecodeIndexTo).append(")]");
        return sb.toString();
    }

    public class Entry implements ComparableUnderEC {
        private final ExceptionTableEntry entry;
        private final JavaRefTypeInstance refType;

        public Entry(ExceptionTableEntry entry) {
            this.entry = entry;
            this.refType = entry.getCatchType(cp);
        }

        public int getBytecodeIndexTo() {
            return entry.getBytecodeIndexTo();
        }

        public int getBytecodeIndexHandler() {
            return entry.getBytecodeIndexHandler();
        }

        public boolean isJustThrowable() {
            JavaRefTypeInstance type = entry.getCatchType(cp);
            return type.getRawName().equals(TypeConstants.throwableName);
        }

        public int getPriority() {
            return entry.getPriority();
        }

        public JavaRefTypeInstance getCatchType() {
            return refType;
        }

        public ExceptionGroup getExceptionGroup() {
            return ExceptionGroup.this;
        }

        public BlockIdentifier getTryBlockIdentifier() {
            return ExceptionGroup.this.getTryBlockIdentifier();
        }

        @Override
        public String toString() {
            JavaRefTypeInstance name = getCatchType();
            return ExceptionGroup.this + " " + name.getRawName();
        }

        @Override
        public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
            if (o == null) return false;
            if (o == this) return true;
            if (getClass() != o.getClass()) return false;
            Entry other = (Entry) o;
            if (!constraint.equivalent(entry, other.entry)) return false;
            return constraint.equivalent(refType, other.refType);
        }

        public ExtenderKey getExtenderKey() {
            return new ExtenderKey(refType, entry.getBytecodeIndexHandler());
        }


    }

    public record ExtenderKey(JavaRefTypeInstance type, int handler) {

        @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ExtenderKey that = (ExtenderKey) o;

                if (handler != that.handler) return false;
                return Objects.equals(type, that.type);
            }

    }

}
