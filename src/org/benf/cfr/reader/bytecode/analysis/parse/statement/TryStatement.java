package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredTry;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class TryStatement extends AbstractStatement {
    private final ExceptionGroup exceptionGroup;
    // This is a hack. :(
    // We keep track of what mutexes this finally leaves.
    private final ObjectSet<Expression> monitors = new ObjectOpenHashSet<>();

    public TryStatement(BytecodeLoc loc, ExceptionGroup exceptionGroup) {
        super(loc);
        this.exceptionGroup = exceptionGroup;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    public void addExitMutex(Expression e) {
        monitors.add(e);
    }

    public ObjectSet<Expression> getMonitors() {
        return monitors;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        TryStatement res = new TryStatement(getLoc(), exceptionGroup);
        for (Expression monitor : monitors) {
            res.monitors.add(cloneHelper.replaceOrClone(monitor));
        }
        return res;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("try { ").print(exceptionGroup.getTryBlockIdentifier().toString()).newln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredTry(exceptionGroup);
    }

    public BlockIdentifier getBlockIdentifier() {
        return exceptionGroup.getTryBlockIdentifier();
    }

    public ObjectList<ExceptionGroup.Entry> getEntries() {
        return exceptionGroup.getEntries();
    }

    public boolean equivalentUnder(Object other, EquivalenceConstraint constraint) {
        return this.getClass() == other.getClass();
    }
}
