package org.benf.cfr.reader.bytecode.analysis.stack;

import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;
import org.benf.cfr.reader.util.ConfusedCFRException;

public record StackDeltaImpl(StackTypes consumed, StackTypes produced) implements StackDelta {
    public StackDeltaImpl {
        if (consumed == null || produced == null) {
            throw new ConfusedCFRException("Must not have null stackTypes");
        }
    }

    @Override
    public boolean isNoOp() {
        return consumed.isEmpty() && produced.isEmpty();
    }

    @Override
    public long getChange() {
        return produced.size() - consumed.size();
    }

    @Override
    public String toString() {
        return "Consumes " + consumed + ", Produces " + produced;
    }
}
