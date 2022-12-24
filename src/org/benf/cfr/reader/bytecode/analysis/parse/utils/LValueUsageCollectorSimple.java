package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class LValueUsageCollectorSimple implements LValueUsageCollector {
    private final ObjectSet<LValue> used = new ObjectOpenHashSet<>();

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        used.add(lValue);
    }

    public Collection<LValue> getUsedLValues() {
        return used;
    }

    public boolean isUsed(LValue lValue) {
        return used.contains(lValue);
    }
}
