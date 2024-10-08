package org.benf.cfr.reader.bytecode.analysis.stack;

import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;

public interface StackDelta {
    boolean isNoOp();

    StackTypes consumed();

    StackTypes produced();

    long getChange();
}
