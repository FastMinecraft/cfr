package org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.*;

/**
 * These are the tries we've identified as being connected via a finally.
 * <p/>
 * So
 * <p/>
 * try {
 * if (a ) return 1;
 * if ( b) return 2
 * } finally {
 * x
 * }
 * <p/>
 * would become
 * <p/>
 * try {
 * if (!a) jump l2:
 * }
 * x
 * return 1;
 * l2:
 * try {
 * if (!b) jump l3
 * jump after catch
 * }
 * x
 * return 2;
 * catch (Throwable ) {
 * x;
 * }
 */
public class PeerTries {
    private final Op03SimpleStatement possibleFinallyCatch;

    private final ObjectSet<Op03SimpleStatement> seenEver = new ObjectLinkedOpenHashSet<>();

    private final LinkedList<Op03SimpleStatement> toProcess = new LinkedList<>();
    private int nextIdx;

    /*
     * Best guess using reverse information from the catch block.
     */
    private final ObjectSet<BlockIdentifier> guessPeerTryBlocks = new ObjectLinkedOpenHashSet<>();
    private final Map<BlockIdentifier, Op03SimpleStatement> guessPeerTryMap = new Object2ObjectLinkedOpenHashMap<>();
    private final ObjectSet<Op03SimpleStatement> guessPeerTryStarts = new ObjectLinkedOpenHashSet<>();

    private final Map<CompositeBlockIdentifierKey, PeerTrySet> triesByLevel = MapFactory.newLazyMap(
        new TreeMap<>(),
        (arg) -> new PeerTrySet(nextIdx++)
    );

    PeerTries(Op03SimpleStatement possibleFinallyCatch) {
        this.possibleFinallyCatch = possibleFinallyCatch;

        for (Op03SimpleStatement source : possibleFinallyCatch.getSources()) {
            Statement statement = source.getStatement();
            if (statement instanceof TryStatement tryStatement) {
                BlockIdentifier blockIdentifier = tryStatement.getBlockIdentifier();
                guessPeerTryBlocks.add(blockIdentifier);
                guessPeerTryMap.put(blockIdentifier, source);
                guessPeerTryStarts.add(source);
            }
        }
    }

    Op03SimpleStatement getOriginalFinally() {
        return possibleFinallyCatch;
    }

    ObjectSet<BlockIdentifier> getGuessPeerTryBlocks() {
        return guessPeerTryBlocks;
    }

    Map<BlockIdentifier, Op03SimpleStatement> getGuessPeerTryMap() {
        return guessPeerTryMap;
    }

    ObjectSet<Op03SimpleStatement> getGuessPeerTryStarts() {
        return guessPeerTryStarts;
    }

    public void add(Op03SimpleStatement tryStatement) {
        if (!(tryStatement.getStatement() instanceof TryStatement)) {
            throw new IllegalStateException();
        }
        if (seenEver.contains(tryStatement)) return;

        toProcess.add(tryStatement);
        triesByLevel.get(new CompositeBlockIdentifierKey(tryStatement)).add(tryStatement);
    }

    public boolean hasNext() {
        return !toProcess.isEmpty();
    }

    Op03SimpleStatement removeNext() {
        return toProcess.removeFirst();
    }

    ObjectList<PeerTrySet> getPeerTryGroups() {
        return new ObjectArrayList<>(triesByLevel.values());
    }

    public static final class PeerTrySet {
        private final ObjectSet<Op03SimpleStatement> content = new ObjectLinkedOpenHashSet<>();
        private final int idx;

        private PeerTrySet(int idx) {
            this.idx = idx;
        }

        public void add(Op03SimpleStatement op) {
            content.add(op);
        }

        Collection<Op03SimpleStatement> getPeerTries() {
            return content;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PeerTrySet that = (PeerTrySet) o;

            return idx == that.idx;
        }

        @Override
        public int hashCode() {
            return idx;
        }
    }

}
