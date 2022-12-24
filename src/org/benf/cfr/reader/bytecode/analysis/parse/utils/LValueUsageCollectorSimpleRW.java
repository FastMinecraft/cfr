package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;

import java.util.Set;

public class LValueUsageCollectorSimpleRW implements LValueUsageCollector {
    private final Set<LValue> read = new ObjectOpenHashSet<>();
    private final Set<LValue> write = new ObjectOpenHashSet<>();

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        switch (rw) {
            case READ:
                read.add(lValue);
                break;
            case READ_WRITE:
                read.add(lValue);
            case WRITE:
                write.add(lValue);
                break;
        }
    }

    public Set<LValue> getRead() {
        return read;
    }

    public Set<LValue> getWritten() {
        return write;
    }
}
