package org.benf.cfr.reader.bytecode.analysis.stack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;
import org.benf.cfr.reader.util.ConfusedCFRException;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class StackSim {
    private final StackSim parent;
    private final StackEntryHolder stackEntryHolder;
    private final long depth;

    public StackSim() {
        this.depth = 0;
        this.parent = null;
        this.stackEntryHolder = null;
    }

    private StackSim(StackSim parent, StackType stackType) {
        this.parent = parent;
        this.depth = parent.depth + 1;
        this.stackEntryHolder = new StackEntryHolder(stackType);
    }

    public StackEntry getEntry(int depth) {
        StackSim thisSim = this;
        while (depth > 0) {
            thisSim = thisSim.getParent();
            depth--;
        }
        if (thisSim.stackEntryHolder == null) {
            throw new ConfusedCFRException("Underrun type stack");
        }
        return thisSim.stackEntryHolder.getStackEntry();
    }

    public ObjectList<StackEntryHolder> getHolders(int offset, long num) {
        StackSim thisSim = this;
        ObjectList<StackEntryHolder> res = new ObjectArrayList<>();
        while (num > 0) {
            if (offset > 0) {
                offset--;
            } else {
                res.add(thisSim.stackEntryHolder);
                num--;
            }
            thisSim = thisSim.getParent();
        }
        return res;
    }

    public long getDepth() {
        return depth;
    }

    public StackSim getChange(StackDelta delta, ObjectList<StackEntryHolder> consumed, ObjectList<StackEntryHolder> produced, Op02WithProcessedDataAndRefs instruction) {
        if (delta.isNoOp()) {
            return this;
        }
        try {
            StackSim thisSim = this;
            StackTypes consumedStack = delta.consumed();
            for (StackType stackType : consumedStack) {
                consumed.add(thisSim.stackEntryHolder);
                thisSim = thisSim.getParent();
            }
            StackTypes producedStack = delta.produced();
            for (int x = producedStack.size() - 1; x >= 0; --x) {
                thisSim = new StackSim(thisSim, producedStack.get(x));
            }
            StackSim thatSim = thisSim;
            for (StackType stackType : producedStack) {
                produced.add(thatSim.stackEntryHolder);
                thatSim = thatSim.getParent();
            }
            return thisSim;
        } catch (ConfusedCFRException e) {
            throw new ConfusedCFRException("While processing " + instruction + " : " + e.getMessage());
        }
    }

    private StackSim getParent() {
        if (parent == null) {
            throw new ConfusedCFRException("Stack underflow");
        }
        return parent;
    }

    @Override
    public String toString() {
        StackSim next = this;
        StringBuilder sb = new StringBuilder();
        while (next != null) {
            if (next.stackEntryHolder == null) break;
            StackEntry stackEntry = next.stackEntryHolder.getStackEntry();
            sb.append(stackEntry).append('[').append(stackEntry.getType()).append("] ");
            next = next.parent;
        }
        return sb.toString();
    }
}
