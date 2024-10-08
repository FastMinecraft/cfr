package org.benf.cfr.reader.bytecode.analysis.stack;

import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.util.DecompilerComment;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StackEntryHolder {
    private StackEntry stackEntry;

    StackEntryHolder(StackType stackType) {
        stackEntry = new StackEntry(stackType);
    }

    public void mergeWith(StackEntryHolder other, ObjectSet<DecompilerComment> comments) {
        stackEntry.mergeWith(other.stackEntry, comments);
        other.stackEntry = stackEntry;
    }

    @Override
    public String toString() {
        return stackEntry.toString();
    }

    public StackEntry getStackEntry() {
        return stackEntry;
    }
}
