package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class Cleaner {
    public static ObjectList<Op03SimpleStatement> removeUnreachableCode(final ObjectList<Op03SimpleStatement> statements, final boolean checkBackJumps) {
        final ObjectSet<Op03SimpleStatement> reachable = new ObjectOpenHashSet<>();
        reachable.add(statements.get(0));
        GraphVisitor<Op03SimpleStatement> gv = new GraphVisitorDFS<>(
            statements.get(0),
            (arg1, arg2) -> {
                reachable.add(arg1);
//                if (!statements.contains(arg1)) {
//                    throw new IllegalStateException("Statement missing");
//                }
                arg2.enqueue(arg1.getTargets());
                for (Op03SimpleStatement source : arg1.getSources()) {
//                    if (!statements.contains(source)) {
//                        throw new IllegalStateException("Source not in graph!");
//                    }
                    if (!source.getTargets().contains(arg1)) {
                        throw new IllegalStateException("Inconsistent graph " + source + " does not have a target of " + arg1);
                    }
                }
                for (Op03SimpleStatement test : arg1.getTargets()) {
                    // Also, check for backjump targets on non jumps.
                    Statement argContained = arg1.getStatement();
                    if (checkBackJumps) {
                        if (!(argContained instanceof JumpingStatement || argContained instanceof WhileStatement)) {
                            if (test.getIndex().isBackJumpFrom(arg1)) {
                                throw new IllegalStateException("Backjump on non jumping statement " + arg1);
                            }
                        }
                    }
                    if (!test.getSources().contains(arg1)) {
                        throw new IllegalStateException("Inconsistent graph " + test + " does not have a source " + arg1);
                    }
                }
            }
        );
        gv.process();

        ObjectList<Op03SimpleStatement> result = new ObjectArrayList<>();
        for (Op03SimpleStatement statement : statements) {
            if (reachable.contains(statement)) {
                result.add(statement);
            }
        }
        // Too expensive....
        for (Op03SimpleStatement res1 : result) {
            Collection<Op03SimpleStatement> original = res1.getSources();
            ObjectList<Op03SimpleStatement> sources = new ObjectArrayList<>(original);
            for (Op03SimpleStatement source : sources) {
                if (!reachable.contains(source)) {
                    res1.removeSource(source);
                }
            }
        }
        return result;
    }

    /*
* Filter out nops (where appropriate) and renumber.  For display purposes.
*/
    public static ObjectList<Op03SimpleStatement> sortAndRenumber(ObjectList<Op03SimpleStatement> statements) {
        boolean nonNopSeen = false;
        ObjectList<Op03SimpleStatement> result = new ObjectArrayList<>();
        for (Op03SimpleStatement statement : statements) {
            boolean thisIsNop = statement.isAgreedNop();
            if (!nonNopSeen) {
                result.add(statement);
                if (!thisIsNop) nonNopSeen = true;
            } else {
                if (!thisIsNop) {
                    result.add(statement);
                }
            }
        }
        // Sort result by existing index.
        sortAndRenumberInPlace(result);
        return result;
    }

    static void sortAndRenumberFromInPlace(ObjectList<Op03SimpleStatement> statements, InstrIndex start) {
        statements.sort(new CompareByIndex());
        for (Op03SimpleStatement statement : statements) {
            statement.setIndex(start);
            start = start.justAfter();
        }
    }

    static void sortAndRenumberInPlace(ObjectList<Op03SimpleStatement> statements) {
        // Sort result by existing index.
        statements.sort(new CompareByIndex());
        reindexInPlace(statements);
    }

    public static void reindexInPlace(ObjectList<Op03SimpleStatement> statements) {
        int newIndex = 0;
        Op03SimpleStatement prev = null;
        for (Op03SimpleStatement statement : statements) {
            statement.setLinearlyPrevious(prev);
            statement.setLinearlyNext(null);
            if (prev != null) prev.setLinearlyNext(statement);
            statement.setIndex(new InstrIndex(newIndex++));
            prev = statement;
        }
    }

    public static void reLinkInPlace(ObjectList<Op03SimpleStatement> statements) {
        Op03SimpleStatement prev = null;
        for (Op03SimpleStatement statement : statements) {
            statement.setLinearlyPrevious(prev);
            statement.setLinearlyNext(null);
            if (prev != null) prev.setLinearlyNext(statement);
            prev = statement;
        }
    }
}
