package org.benf.cfr.reader.bytecode.analysis.variables;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public record Slot(JavaTypeInstance javaTypeInstance, int idx) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slot slot = (Slot) o;

        if (idx != slot.idx) return false;

        return true;
    }

    @Override
    public String toString() {
        return "S{" +
            idx +
            '}';
    }

    @Override
    public int hashCode() {
        return idx;
    }
}
