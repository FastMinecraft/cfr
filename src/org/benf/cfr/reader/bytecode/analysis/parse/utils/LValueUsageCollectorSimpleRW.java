package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public class LValueUsageCollectorSimpleRW implements LValueUsageCollector {
    private final ObjectSet<LValue> read = new ObjectOpenHashSet<>();
    private final ObjectSet<LValue> write = new ObjectOpenHashSet<>();

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

    public ObjectSet<LValue> getRead() {
        return read;
    }

    public ObjectSet<LValue> getWritten() {
        return write;
    }
}
