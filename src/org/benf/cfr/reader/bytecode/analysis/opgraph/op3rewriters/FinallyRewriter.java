package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.FinalAnalyzer;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class FinallyRewriter {
    public static void identifyFinally(Options options, Method method, ObjectList<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory) {
        if (!options.getOption(OptionsImpl.DECODE_FINALLY)) return;
        /* Get all the try statements, get their catches.  For all the EXIT points to the catches, try to identify
         * a common block of code (either before a throw, return or goto.)
         * Be careful, if a finally block contains a throw, this will mess up...
         */
        final ObjectSet<Op03SimpleStatement> analysedTries = new ObjectOpenHashSet<>();
        boolean continueLoop;
        do {
            ObjectList<Op03SimpleStatement> tryStarts = Functional.filter(in, in1 -> in1.getStatement() instanceof TryStatement && !analysedTries.contains(in1));
            for (Op03SimpleStatement tryS : tryStarts) {
                FinalAnalyzer.identifyFinally(method, tryS, in, blockIdentifierFactory, analysedTries);
            }
            /*
             * We may need to reloop, if analysis has created new tries inside finally handlers. (!).
             */
            continueLoop = (!tryStarts.isEmpty());
        } while (continueLoop);
    }

    static ObjectSet<BlockIdentifier> getBlocksAffectedByFinally(ObjectList<Op03SimpleStatement> statements) {
        ObjectSet<BlockIdentifier> res = new ObjectOpenHashSet<>();
        for (Op03SimpleStatement stm : statements) {
            if (stm.getStatement() instanceof TryStatement tryStatement) {
                ObjectSet<BlockIdentifier> newBlocks = new ObjectOpenHashSet<>();
                boolean found = false;
                newBlocks.add(tryStatement.getBlockIdentifier());
                for (Op03SimpleStatement tgt : stm.getTargets()) {
                    Statement inr = tgt.getStatement();
                    if (inr instanceof CatchStatement) {
                        newBlocks.add(((CatchStatement)inr).getCatchBlockIdent());
                    }
                    if (tgt.getStatement() instanceof FinallyStatement) {
                        found = true;
                    }
                }
                if (found) res.addAll(newBlocks);
            }
        }
        return res;
    }
}
