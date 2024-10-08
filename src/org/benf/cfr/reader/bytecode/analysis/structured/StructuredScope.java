package org.benf.cfr.reader.bytecode.analysis.structured;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;

import java.util.LinkedList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StructuredScope {

    private final LinkedList<AtLevel> scope = new LinkedList<>();

    public void add(StructuredStatement statement) {
        scope.addFirst(new AtLevel(statement));
    }

    public void remove(StructuredStatement statement) {
        AtLevel old = scope.removeFirst();
        if (statement != old.statement) {
            throw new IllegalStateException();
        }
    }

    public List<Op04StructuredStatement> getPrecedingInblock(int skipN, int back) {
        if (skipN >= scope.size()) return null;
        AtLevel level = scope.get(skipN);
        StructuredStatement stm = level.statement;
        if (stm instanceof Block block) {
            int end = level.next - 1;
            int start = Math.max(end - back, 0);
            return block.getBlockStatements().subList(start, end);
        }
        return new ObjectArrayList<>();
    }

    public StructuredStatement get(int skipN) {
        if (skipN >= scope.size()) return null;
        return scope.get(skipN).statement;
    }

    public ObjectList<StructuredStatement> getAll() {
        ObjectList<StructuredStatement> ret = new ObjectArrayList<>();
        for (AtLevel atLevel : scope) {
            ret.add(atLevel.statement);
        }
        return ret;
    }

    public void setNextAtThisLevel(StructuredStatement statement, int next) {
        AtLevel atLevel = scope.getFirst();
        if (atLevel.statement != statement) {
            throw new IllegalStateException();
        }
        atLevel.next = next;
    }

    public BlockIdentifier getContinueBlock() {
        for (AtLevel atLevel : scope) {
            Op04StructuredStatement stm = atLevel.statement.getContainer();
            StructuredStatement stmt = stm.getStatement();
            if (stmt.supportsBreak()) {
                if (!stmt.supportsContinueBreak()) return null;
                return stmt.getBreakableBlockOrNull();
            }
        }
        return null;
    }

    public ObjectSet<Op04StructuredStatement> getNextFallThrough(StructuredStatement structuredStatement) {
        Op04StructuredStatement current = structuredStatement.getContainer();
        ObjectSet<Op04StructuredStatement> res = new ObjectOpenHashSet<>();
        int idx = -1;
        for (AtLevel atLevel : scope) {
            idx++;
            if (idx == 0 && atLevel.statement == structuredStatement) continue;
            if (atLevel.statement instanceof Block) {
                if (atLevel.next != -1) {
                    res.addAll(((Block) atLevel.statement).getNextAfter(atLevel.next, false));
                }
                if (((Block) atLevel.statement).statementIsLast(current)) {
                    current = atLevel.statement.getContainer();
                    continue;
                }
                break;
            } else if (atLevel.statement.fallsNopToNext()) {
                current = atLevel.statement.getContainer();
                continue;
            }
            break;
        }
        return res;
    }

    public ObjectSet<Op04StructuredStatement> getDirectFallThrough() {
        AtLevel atLevel = scope.getFirst();
        if (atLevel.statement instanceof Block) {
            if (atLevel.next != -1) {
                return (((Block) atLevel.statement).getNextAfter(atLevel.next, false));
            }
        }
        return new ObjectOpenHashSet<>();
    }

    // Check if, in the enclosing scope, this statement is the last one (i.e. can a break be dropped)?
    public boolean statementIsLast(StructuredStatement statement) {
        AtLevel atLevel = scope.getFirst();
        int x = 1;
        StructuredStatement s = atLevel.statement;
        if (s instanceof Block) {
            return ((Block) s).statementIsLast(statement.getContainer());
        } else {
            return statement == s; // object.
        }
    }

    protected static class AtLevel {
        final StructuredStatement statement;
        int next;

        private AtLevel(StructuredStatement statement) {
            this.statement = statement;
            this.next = 0;
        }

        @Override
        public String toString() {
            return statement.toString();
        }
    }
}
