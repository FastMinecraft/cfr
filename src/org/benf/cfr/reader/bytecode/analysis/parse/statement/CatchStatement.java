package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class CatchStatement extends AbstractStatement {
    private final ObjectList<ExceptionGroup.Entry> exceptions;
    private BlockIdentifier catchBlockIdent;
    private LValue catching;

    public CatchStatement(BytecodeLoc loc, ObjectList<ExceptionGroup.Entry> exceptions, LValue catching) {
        super(loc);
        this.exceptions = exceptions;
        this.catching = catching;
        if (!exceptions.isEmpty()) {
            JavaTypeInstance collapsedCatchType = determineType(exceptions);
            InferredJavaType catchType = new InferredJavaType(collapsedCatchType, InferredJavaType.Source.EXCEPTION, true);
            this.catching.getInferredJavaType().chain(catchType);
        }
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    private static JavaTypeInstance determineType(ObjectList<ExceptionGroup.Entry> exceptions) {
        InferredJavaType ijt = new InferredJavaType();
        ijt.chain(new InferredJavaType(exceptions.get(0).getCatchType(), InferredJavaType.Source.EXCEPTION));
        for (int x = 1, len = exceptions.size(); x < len; ++x) {
            ijt.chain(new InferredJavaType(exceptions.get(x).getCatchType(), InferredJavaType.Source.EXCEPTION));
        }
        if (ijt.isClash()) {
            ijt.collapseTypeClash();
        }
        return ijt.getJavaTypeInstance();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        // TODO: blockidents when cloning.
        CatchStatement res = new CatchStatement(getLoc(), exceptions, cloneHelper.replaceOrClone(catching));
        res.setCatchBlockIdent(catchBlockIdent);
        return res;
    }

    public void removeCatchBlockFor(final BlockIdentifier tryBlockIdent) {
        ObjectList<ExceptionGroup.Entry> toRemove = Functional.filter(exceptions,
            in -> in.getTryBlockIdentifier().equals(tryBlockIdent)
        );
        exceptions.removeAll(toRemove);
    }

    public boolean hasCatchBlockFor(final BlockIdentifier tryBlockIdent) {
        for (ExceptionGroup.Entry entry : exceptions) {
            if (entry.getTryBlockIdentifier().equals(tryBlockIdent)) return true;
        }
        return false;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("catch ").separator("( " + exceptions + " ").dump(catching).separator(" ) ").separator("{").newln();
    }

    public BlockIdentifier getCatchBlockIdent() {
        return catchBlockIdent;
    }

    public void setCatchBlockIdent(BlockIdentifier catchBlockIdent) {
        this.catchBlockIdent = catchBlockIdent;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        catching = expressionRewriter.rewriteExpression(catching, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.LVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
        if (catching instanceof LocalVariable) {
            lValueAssigmentCollector.collectLocalVariableAssignment((LocalVariable) catching, this.getContainer(), null);
        }
    }

    @Override
    public LValue getCreatedLValue() {
        return catching;
    }


    public ObjectList<ExceptionGroup.Entry> getExceptions() {
        return exceptions;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredCatch(exceptions, catchBlockIdent, catching);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        CatchStatement other = (CatchStatement) o;
        if (!constraint.equivalent(exceptions, other.exceptions)) return false;
        return constraint.equivalent(catching, other.catching);
    }

}
