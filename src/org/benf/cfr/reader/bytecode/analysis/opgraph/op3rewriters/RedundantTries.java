package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;

import java.util.Collections;
import java.util.LinkedList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class RedundantTries {
    public static ObjectList<Op03SimpleStatement> removeRedundantTries(ObjectList<Op03SimpleStatement> statements) {
        ObjectList<Op03SimpleStatement> tryStarts = Functional.filter(statements, new TypeFilter<>(TryStatement.class));
        /*
         * If the try doesn't point at a member of the try, it's been made redundant.
         * Verify that no other references to its' block exist, and remove it.
         * (Verification should be unneccesary)
         */
        boolean effect = false;
        Collections.reverse(tryStarts);
        LinkedList<Op03SimpleStatement> starts = new LinkedList<>();
        starts.addAll(tryStarts);
        while (!starts.isEmpty()) {
            Op03SimpleStatement trys = starts.removeFirst();
            Statement stm = trys.getStatement();
            if (!(stm instanceof TryStatement tryStatement)) continue;
            BlockIdentifier tryBlock = tryStatement.getBlockIdentifier();
            if (trys.getTargets().isEmpty() || !trys.getTargets().get(0).getBlockIdentifiers().contains(tryBlock)) {
                // Remove this try.
                Op03SimpleStatement codeTarget = trys.getTargets().get(0);

                for (Op03SimpleStatement target : trys.getTargets()) {
                    target.removeSource(trys);
                }
                trys.getTargets().clear();
                for (Op03SimpleStatement source : trys.getSources()) {
                    source.replaceTarget(trys, codeTarget);
                    codeTarget.addSource(source);
                }
                trys.getSources().clear();
                effect = true;
            }
        }

        if (effect) {
            statements = Cleaner.removeUnreachableCode(statements, false);
            statements = Cleaner.sortAndRenumber(statements);
        }

        return statements;
    }


}
