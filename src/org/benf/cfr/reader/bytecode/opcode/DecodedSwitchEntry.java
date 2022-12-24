package org.benf.cfr.reader.bytecode.opcode;

import it.unimi.dsi.fastutil.ints.IntList;
import org.benf.cfr.reader.util.StringUtils;

public class DecodedSwitchEntry {
    private final IntList value;
    // TODO : Not useful past 0p01->Op02 stage.  Create a different interface.
    private final int bytecodeTarget;

    public DecodedSwitchEntry(IntList value, int bytecodeTarget) {
        this.bytecodeTarget = bytecodeTarget;
        this.value = value;
    }

    public IntList getValue() {
        return value;
    }

    int getBytecodeTarget() {
        return bytecodeTarget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("case ");
        for (int i = 0, valueSize = value.size(); i < valueSize; i++) {
            int val = value.getInt(i);
            first = StringUtils.comma(first, sb);
            sb.append(val == Integer.MIN_VALUE ? "default" : val);
        }
        sb.append(" -> ").append(bytecodeTarget);
        return sb.toString();
    }

    public boolean hasDefault() {
        return value.contains(Integer.MIN_VALUE);
    }
}
