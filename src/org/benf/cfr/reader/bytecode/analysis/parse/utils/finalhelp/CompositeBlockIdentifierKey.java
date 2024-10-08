package org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class CompositeBlockIdentifierKey implements Comparable<CompositeBlockIdentifierKey> {
    private final String key;

    public CompositeBlockIdentifierKey(Op03SimpleStatement statement) {
        this(statement.getBlockIdentifiers());
    }

    public CompositeBlockIdentifierKey(ObjectSet<BlockIdentifier> blockIdentifiers) {
        ObjectList<BlockIdentifier> b = Functional.filter(blockIdentifiers, in -> switch (in.getBlockType()) {
            case TRYBLOCK, CATCHBLOCK -> true;
            default -> false;
        });
        Collections.sort(b);
        StringBuilder sb = new StringBuilder();
        for (BlockIdentifier blockIdentifier : b) {
            sb.append(blockIdentifier.getIndex()).append(".");
        }
        this.key = sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompositeBlockIdentifierKey that = (CompositeBlockIdentifierKey) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo(CompositeBlockIdentifierKey compositeBlockIdentifierKey) {
        if (compositeBlockIdentifierKey == this) return 0;
        if (this.key.length() < compositeBlockIdentifierKey.key.length()) return -1;
        return this.key.compareTo(compositeBlockIdentifierKey.key);
    }

    @Override
    public String toString() {
        return "CompositeBlockIdentifierKey{" +
                "key='" + key + '\'' +
                '}';
    }
}
