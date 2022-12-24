package org.benf.cfr.reader.entities.attributes;

public record LocalVariableEntry(int startPc, int length, int nameIndex, int descriptorIndex, int index) {

    public int getEndPc() {
        return startPc + length;
    }

}
