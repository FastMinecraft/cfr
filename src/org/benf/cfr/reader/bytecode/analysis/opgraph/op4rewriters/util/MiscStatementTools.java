package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.util.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.List;

public class MiscStatementTools {
    public static List<Op04StructuredStatement> getBlockStatements(Op04StructuredStatement code) {
        StructuredStatement topCode = code.getStatement();
        if (!(topCode instanceof Block block)) return null;

        return block.getBlockStatements();
    }

    public static boolean isDeadCode(Op04StructuredStatement code) {
        List<Op04StructuredStatement> statements = getBlockStatements(code);
        if (statements == null) return false;
        for (Op04StructuredStatement statement : statements) {
            if (!(statement.getStatement() instanceof StructuredComment)) return false;
        }
        return true;
    }

    public static @Nullable
    ObjectList<StructuredStatement> linearise(Op04StructuredStatement root) {
        ObjectList<StructuredStatement> structuredStatements = new ObjectArrayList<>();
        try {
            // This is being done multiple times, it's very inefficient!
            root.linearizeStatementsInto(structuredStatements);
        } catch (UnsupportedOperationException e) {
            // Todo : Should output something at the end about this failure.
            return null;
        }
        return structuredStatements;
    }

    public static void applyExpressionRewriter(Op04StructuredStatement root, ExpressionRewriter expressionRewriter) {
        ObjectList<StructuredStatement> statements = linearise(root);
        if (statements == null) return;
        for (StructuredStatement statement : statements) {
            statement.rewriteExpressions(expressionRewriter);
        }
    }
}
