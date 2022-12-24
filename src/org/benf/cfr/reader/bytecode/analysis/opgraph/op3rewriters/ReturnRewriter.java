package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.*;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

class ReturnRewriter {
    private static void replaceReturningIf(Op03SimpleStatement ifStatement, boolean aggressive) {
        if (!(ifStatement.getStatement().getClass() == IfStatement.class)) return;
        IfStatement innerIf = (IfStatement) ifStatement.getStatement();
        Op03SimpleStatement tgt = ifStatement.getTargets().get(1);
        final Op03SimpleStatement origtgt = tgt;
        boolean requireJustOneSource = !aggressive;
        do {
            Op03SimpleStatement next = Misc.followNopGoto(tgt, requireJustOneSource, aggressive);
            if (next == tgt) break;
            tgt = next;
        } while (true);
        Statement tgtStatement = tgt.getStatement();
        if (tgtStatement instanceof ReturnStatement) {
            ifStatement.replaceStatement(new IfExitingStatement(innerIf.getLoc(), innerIf.getCondition(), tgtStatement));
            Op03SimpleStatement origfall = ifStatement.getTargets().get(0);
            origfall.setFirstStatementInThisBlock(null);
            BlockIdentifier ifBlock = innerIf.getKnownIfBlock();
            Pair<ObjectSet<Op03SimpleStatement>, ObjectSet<Op03SimpleStatement>> blockReachableAndExits = Misc.GraphVisitorBlockReachable.getBlockReachableAndExits(origfall, ifBlock);
            for (Op03SimpleStatement stm : blockReachableAndExits.getFirst()) {
                stm.getBlockIdentifiers().remove(ifBlock);
            }
        } else {
            return;
        }
        origtgt.removeSource(ifStatement);
        ifStatement.removeTarget(origtgt);
    }

    static void replaceReturningIfs(ObjectList<Op03SimpleStatement> statements, boolean aggressive) {
        ObjectList<Op03SimpleStatement> ifStatements = Functional.filter(statements, new TypeFilter<>(IfStatement.class));
        for (Op03SimpleStatement ifStatement : ifStatements) {
            replaceReturningIf(ifStatement, aggressive);
        }
    }

    static void propagateToReturn2(ObjectList<Op03SimpleStatement> statements) {
        boolean success = false;
        for (Op03SimpleStatement stm : statements) {
            Statement inner = stm.getStatement();

            if (inner instanceof ReturnStatement) {
                /*
                 * Another very aggressive operation - find any goto which directly jumps to a return, and
                 * place a copy of the return in the goto.
                 *
                 * This will interfere with returning a ternary, however because it's an aggressive option, it
                 * won't be used unless needed.
                 *
                 * We look for returns rather than gotos, as returns are less common.
                 */
                success |= pushReturnBack(stm);
            }
        }
        if (success) Op03Rewriters.replaceReturningIfs(statements, true);
    }

    private static boolean pushReturnBack(final Op03SimpleStatement returnStm) {

        ReturnStatement returnStatement = (ReturnStatement) returnStm.getStatement();
        final ObjectList<Op03SimpleStatement> replaceWithReturn = new ObjectArrayList<>();

        new GraphVisitorDFS<Op03SimpleStatement>(returnStm.getSources(), (arg1, arg2) -> {
            Class<?> clazz = arg1.getStatement().getClass();
            if (clazz == CommentStatement.class ||
                clazz == Nop.class ||
                clazz == DoStatement.class) {
                arg2.enqueue(arg1.getSources());
            } else if (clazz == WhileStatement.class) {
                // only if it's 'while true'.
                WhileStatement whileStatement = (WhileStatement) arg1.getStatement();
                if (whileStatement.getCondition() == null) {
                    arg2.enqueue(arg1.getSources());
                    replaceWithReturn.add(arg1);
                }
            } else if (clazz == GotoStatement.class) {
                arg2.enqueue(arg1.getSources());
                replaceWithReturn.add(arg1);
            }
        }).process();

        if (replaceWithReturn.isEmpty()) return false;

        CloneHelper cloneHelper = new CloneHelper();
        for (Op03SimpleStatement remove : replaceWithReturn) {
            remove.replaceStatement(returnStatement.deepClone(cloneHelper));
            for (Op03SimpleStatement tgt : remove.getTargets()) {
                tgt.removeSource(remove);
            }
            remove.clearTargets();
        }

        return true;
    }


}
