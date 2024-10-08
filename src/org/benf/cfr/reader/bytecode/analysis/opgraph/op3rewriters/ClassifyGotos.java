package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.*;

import java.util.Map;

class ClassifyGotos {
    static void classifyGotos(ObjectList<Op03SimpleStatement> in) {
        ObjectList<Pair<Op03SimpleStatement, Integer>> gotos = new ObjectArrayList<>();
        Object2ObjectMap<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock = new Object2ObjectOpenHashMap<>();
        Map<BlockIdentifier, ObjectList<BlockIdentifier>> catchStatementsByBlock = MapFactory.newMap();
        Map<BlockIdentifier, ObjectSet<BlockIdentifier>> catchToTries = MapFactory.newLazyMap(arg -> new ObjectLinkedOpenHashSet<>());
        for (int x = 0, len = in.size(); x < len; ++x) {
            Op03SimpleStatement stm = in.get(x);
            Statement statement = stm.getStatement();
            Class<?> clz = statement.getClass();
            if (clz == TryStatement.class) {
                TryStatement tryStatement = (TryStatement) statement;
                BlockIdentifier tryBlockIdent = tryStatement.getBlockIdentifier();
                tryStatementsByBlock.put(tryBlockIdent, stm);
                ObjectList<Op03SimpleStatement> targets = stm.getTargets();
                ObjectList<BlockIdentifier> catchBlocks = new ObjectArrayList<>();
                catchStatementsByBlock.put(tryStatement.getBlockIdentifier(), catchBlocks);
                for (int y = 1, len2 = targets.size(); y < len2; ++y) {
                    Statement statement2 = targets.get(y).getStatement();
                    if (statement2.getClass() == CatchStatement.class) {
                        BlockIdentifier catchBlockIdent = ((CatchStatement) statement2).getCatchBlockIdent();
                        catchBlocks.add(catchBlockIdent);
                        catchToTries.get(catchBlockIdent).add(tryBlockIdent);
                    }
                }
            } else if (clz == GotoStatement.class) {
                GotoStatement gotoStatement = (GotoStatement) statement;
                if (gotoStatement.getJumpType().isUnknown()) {
                    gotos.add(Pair.make(stm, x));
                }
            }
        }
        /*
         * Pass over try statements.  If there aren't any, don't bother.
         */
        if (!tryStatementsByBlock.isEmpty()) {
            for (Pair<Op03SimpleStatement, Integer> goto_ : gotos) {
                Op03SimpleStatement stm = goto_.getFirst();
                int idx = goto_.getSecond();
                if (classifyTryLeaveGoto(stm, idx, tryStatementsByBlock.keySet(), tryStatementsByBlock, catchStatementsByBlock, in)) {
                    continue;
                }
                classifyCatchLeaveGoto(stm, idx, tryStatementsByBlock.keySet(), tryStatementsByBlock, catchStatementsByBlock, catchToTries, in);
            }
        }
    }

    private static boolean classifyTryLeaveGoto(Op03SimpleStatement gotoStm, int idx, ObjectSet<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, ObjectList<BlockIdentifier>> catchStatementByBlock, ObjectList<Op03SimpleStatement> in) {
        ObjectSet<BlockIdentifier> blocks = gotoStm.getBlockIdentifiers();
        return classifyTryCatchLeaveGoto(gotoStm, blocks, idx, tryBlockIdents, tryStatementsByBlock, catchStatementByBlock, in);
    }

    private static void classifyCatchLeaveGoto(Op03SimpleStatement gotoStm, int idx, ObjectSet<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, ObjectList<BlockIdentifier>> catchStatementByBlock, Map<BlockIdentifier, ObjectSet<BlockIdentifier>> catchBlockToTryBlocks, ObjectList<Op03SimpleStatement> in) {
        ObjectSet<BlockIdentifier> inBlocks = gotoStm.getBlockIdentifiers();

        /*
         * Map blocks to the union of the TRY blocks we're in catch blocks of.
         */
        ObjectSet<BlockIdentifier> blocks = new ObjectLinkedOpenHashSet<>();
        for (BlockIdentifier block : inBlocks) {
            //
            // In case it's a lazy map, 2 stage lookup and fetch.
            if (catchBlockToTryBlocks.containsKey(block)) {
                ObjectSet<BlockIdentifier> catchToTries = catchBlockToTryBlocks.get(block);
                blocks.addAll(catchToTries);
            }
        }

        classifyTryCatchLeaveGoto(gotoStm, blocks, idx, tryBlockIdents, tryStatementsByBlock, catchStatementByBlock, in);
    }


    /*
     * Attempt to determine if a goto is jumping over catch blocks - if it is, we can mark it as a GOTO_OUT_OF_TRY
     * (the same holds for a goto inside a catch, we use the same marker).
     */
    private static boolean classifyTryCatchLeaveGoto(Op03SimpleStatement gotoStm, ObjectSet<BlockIdentifier> blocks, int idx, ObjectSet<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, ObjectList<BlockIdentifier>> catchStatementByBlock, ObjectList<Op03SimpleStatement> in) {
        if (idx >= in.size() - 1) return false;

        GotoStatement gotoStatement = (GotoStatement) gotoStm.getStatement();

        ObjectSet<BlockIdentifier> tryBlocks = SetUtil.intersectionOrNull(blocks, tryBlockIdents);
        if (tryBlocks == null) return false;


        Op03SimpleStatement after = in.get(idx + 1);
        ObjectSet<BlockIdentifier> afterBlocks = SetUtil.intersectionOrNull(after.getBlockIdentifiers(), tryBlockIdents);

        if (afterBlocks != null) tryBlocks.removeAll(afterBlocks);
        if (tryBlocks.size() != 1) return false;
        BlockIdentifier left = tryBlocks.iterator().next();

        // Ok, so we've jumped out of exactly one try block.  But where have we jumped to?  Is it to directly after
        // a catch block for that try block?
        Op03SimpleStatement tryStatement = tryStatementsByBlock.get(left);
        if (tryStatement == null) return false;

        ObjectList<BlockIdentifier> catchForThis = catchStatementByBlock.get(left);
        if (catchForThis == null) return false;

        /*
         * We require that gotoStm's one target is
         * /not in 'left'/
         * just after a catch block.
         * Not in any of the catch blocks.
         */
        Op03SimpleStatement gotoTgt = gotoStm.getTargets().get(0);
        ObjectSet<BlockIdentifier> gotoTgtIdents = gotoTgt.getBlockIdentifiers();
        if (SetUtil.hasIntersection(gotoTgtIdents, catchForThis)) return false;
        int idxtgt = in.indexOf(gotoTgt);
        if (idxtgt == 0) return false;
        Op03SimpleStatement prev = in.get(idxtgt - 1);
        if (!SetUtil.hasIntersection(prev.getBlockIdentifiers(), catchForThis)) {

            if (catchForThis.size() == 1 && after.getStatement() instanceof CatchStatement) {
                boolean emptyCatch = after == prev;
                if (!emptyCatch) {
                    Op03SimpleStatement catchSucc = after.getTargets().get(0);
                    if (Misc.followNopGotoChain(catchSucc, false, true) == Misc.followNopGotoChain(prev, false, true)) {
                        emptyCatch = true;
                    }
                }
                if (emptyCatch) {
                    CatchStatement catchTest = (CatchStatement) after.getStatement();
                    if (catchTest.getCatchBlockIdent() == catchForThis.get(0)) {
                        // Try statement jumping over an empty catch block - allowed!
                        gotoStatement.setJumpType(JumpType.GOTO_OUT_OF_TRY);
                        return true;
                    }
                }
            }
            return false;
        }
        // YAY!
        gotoStatement.setJumpType(JumpType.GOTO_OUT_OF_TRY);
        return true;
    }


    static void classifyAnonymousBlockGotos(ObjectList<Op03SimpleStatement> in, boolean agressive) {
        int agressiveOffset = agressive ? 1 : 0;

        /*
         * Now, finally, for each unclassified goto, see if we can mark it as a break out of an anonymous block.
         */
        for (Op03SimpleStatement statement : in) {
            Statement inner = statement.getStatement();
            if (inner instanceof JumpingStatement jumpingStatement) {
                JumpType jumpType = jumpingStatement.getJumpType();
                if (jumpType != JumpType.GOTO) continue;
                Op03SimpleStatement targetStatement = (Op03SimpleStatement) jumpingStatement.getJumpTarget().getContainer();
                boolean isForwardJump = targetStatement.getIndex().isBackJumpTo(statement);
                if (isForwardJump) {
                    ObjectSet<BlockIdentifier> targetBlocks = targetStatement.getBlockIdentifiers();
                    ObjectSet<BlockIdentifier> srcBlocks = statement.getBlockIdentifiers();
                    if (targetBlocks.size() < srcBlocks.size() + agressiveOffset  && srcBlocks.containsAll(targetBlocks)) {
                        /*
                         * Remove all the switch blocks from srcBlocks.
                         */
                        srcBlocks = Functional.filterSet(srcBlocks, in1 -> {
                            BlockType blockType = in1.getBlockType();
                            if (blockType == BlockType.CASE) return false;
                            return blockType != BlockType.SWITCH;
                        });
                        if (targetBlocks.size() < srcBlocks.size() + agressiveOffset && srcBlocks.containsAll(targetBlocks)) {
                            /*
                             * Break out of an anonymous block
                             */
                            /*
                             * Should we now be re-looking at ALL other forward jumps to this target?
                             */
                            jumpingStatement.setJumpType(JumpType.BREAK_ANONYMOUS);
                        }
                    }
                }
            }
        }

    }



}
