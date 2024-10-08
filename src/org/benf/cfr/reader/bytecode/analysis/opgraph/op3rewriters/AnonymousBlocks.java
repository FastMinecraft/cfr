package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AnonBreakTarget;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.util.collections.Functional;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class AnonymousBlocks {
    public static void labelAnonymousBlocks(ObjectList<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory) {
        ObjectList<Op03SimpleStatement> anonBreaks = Functional.filter(statements, in -> {
            Statement statement = in.getStatement();
            if (!(statement instanceof JumpingStatement)) return false;
            JumpType jumpType = ((JumpingStatement) statement).getJumpType();
            return jumpType == JumpType.BREAK_ANONYMOUS;
        });
        if (anonBreaks.isEmpty()) return;

        /*
         * Collect the unique set of targets for the anonymous breaks.
         */
        ObjectSet<Op03SimpleStatement> targets = new ObjectLinkedOpenHashSet<>();
        for (Op03SimpleStatement anonBreak : anonBreaks) {
            JumpingStatement jumpingStatement = (JumpingStatement) anonBreak.getStatement();
            Op03SimpleStatement anonBreakTarget = (Op03SimpleStatement) jumpingStatement.getJumpTarget().getContainer();
            if (anonBreakTarget.getStatement() instanceof AnonBreakTarget) continue;
            targets.add(anonBreakTarget);
        }

        for (Op03SimpleStatement target : targets) {
            BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.ANONYMOUS);
            InstrIndex targetIndex = target.getIndex();
            Op03SimpleStatement anonTarget = new Op03SimpleStatement(
                    target.getBlockIdentifiers(), new AnonBreakTarget(blockIdentifier), targetIndex.justBefore());
            Collection<Op03SimpleStatement> original = target.getSources();
            ObjectList<Op03SimpleStatement> sources = new ObjectArrayList<>(original);
            for (Op03SimpleStatement source : sources) {
                if (targetIndex.isBackJumpTo(source)) {
                    target.removeSource(source);
                    source.replaceTarget(target, anonTarget);
                    anonTarget.addSource(source);
                }
            }
            target.addSource(anonTarget);
            anonTarget.addTarget(target);
            int pos = statements.indexOf(target);
            statements.add(pos, anonTarget);
        }
    }


}
