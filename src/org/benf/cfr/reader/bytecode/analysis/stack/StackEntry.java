package org.benf.cfr.reader.bytecode.analysis.stack;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.concurrent.atomic.AtomicLong;

public class StackEntry {
    // Should really be a 'static state per cfr driver', but the content won't
    // affect a decompilation.
    // see bug #250.
    private final static AtomicLong sid = new AtomicLong(0);

    private final long id0;
    private final LongSet ids = new LongOpenHashSet();
    private int artificalSourceCount = 0;
    private final StackSSALabel lValue;
    private long usageCount = 0;
    private final StackType stackType;
    private final InferredJavaType inferredJavaType = new InferredJavaType();

    StackEntry(StackType stackType) {
        id0 = sid.getAndIncrement();
        ids.add(id0);
        this.lValue = new StackSSALabel(id0, this);
        this.stackType = stackType;
    }

    public void incrementUsage() {
        ++usageCount;
    }

    public void decrementUsage() {
        --usageCount;
    }

    public void forceUsageCount(long newCount) {
        usageCount = newCount;
    }

    void mergeWith(StackEntry other, ObjectSet<DecompilerComment> comments) {
        if (other.stackType != this.stackType) {
            comments.add(DecompilerComment.UNVERIFIABLE_BYTECODE_BAD_MERGE);
        }
        ids.addAll(other.ids);
        usageCount += other.usageCount;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public int getSourceCount() {
        return ids.size() + artificalSourceCount;
    }

    public void incSourceCount() {
        artificalSourceCount++;
    }

    public void decSourceCount() {
        artificalSourceCount--;
    }

    public LongList getSources() {
        return new LongArrayList(ids);
    }

    public void removeSource(long x) {
        if (!ids.remove(x)) {
            throw new ConfusedCFRException("Attempt to remove non existent id");
        }
    }

    @Override
    public String toString() {
        return "" + id0;
    }

    public StackSSALabel getLValue() {
        return lValue;
    }

    public StackType getType() {
        return stackType;
    }

    public InferredJavaType getInferredJavaType() {
        return inferredJavaType;
    }

    @Override
    public int hashCode() {
        return (int) id0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof StackEntry)) return false;
        return id0 == ((StackEntry) o).id0;
    }
}
