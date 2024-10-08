package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Objects;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StructuredCatch extends AbstractStructuredStatement {
    private final ObjectList<JavaRefTypeInstance> catchTypes;
    private final Op04StructuredStatement catchBlock;
    private final LValue catching;
    private final ObjectSet<BlockIdentifier> possibleTryBlocks;

    public StructuredCatch(Collection<JavaRefTypeInstance> catchTypes, Op04StructuredStatement catchBlock, LValue catching, ObjectSet<BlockIdentifier> possibleTryBlocks) {
        super(BytecodeLoc.NONE);
        this.catchTypes = catchTypes == null ? null : new ObjectArrayList<>(catchTypes);
        this.catchBlock = catchBlock;
        this.catching = catching;
        this.possibleTryBlocks = possibleTryBlocks;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(catchTypes);
        if (!collector.isStatementRecursive()) return;
        catchBlock.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    public ObjectList<JavaRefTypeInstance> getCatchTypes() {
        return catchTypes;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        boolean first = true;
        dumper.keyword("catch ").separator("(");
        for (JavaRefTypeInstance catchType : catchTypes) {
            if (!first) dumper.operator(" | ");
            dumper.dump(catchType);
            first = false;
        }
        dumper.print(" ").dump(catching).separator(") ");
        catchBlock.dump(dumper);
        return dumper;
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        catchBlock.transform(transformer, scope);
    }

    @Override
    public void linearizeInto(ObjectList<StructuredStatement> out) {
        out.add(this);
        catchBlock.linearizeStatementsInto(out);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredCatch other)) return false;
        // we don't actually check any equality for a match.
        matchIterator.advance();
        return true;
    }

    public boolean isRethrow() {
        StructuredStatement statement = catchBlock.getStatement();
        if (!(statement instanceof Block block)) return false;
        Optional<Op04StructuredStatement> maybeStatement = block.getMaybeJustOneStatement();
        if (!maybeStatement.isSet()) return false;
        StructuredStatement inBlock = maybeStatement.getValue().getStatement();
        StructuredThrow test = new StructuredThrow(BytecodeLoc.NONE, new LValueExpression(catching));
        return (test.equals(inBlock));
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (catching instanceof LocalVariable) {
            scopeDiscoverer.collectLocalVariableAssignment((LocalVariable) catching, this.getContainer(), null);
        }
        scopeDiscoverer.processOp04Statement(catchBlock);
    }

    @Override
    public ObjectList<LValue> findCreatedHere() {
        return ObjectList.of(new LValue[]{ catching });
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(this.getContainer());
    }

    public ObjectSet<BlockIdentifier> getPossibleTryBlocks() {
        return possibleTryBlocks;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return catchBlock.isFullyStructured();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructuredCatch that = (StructuredCatch) o;

        return Objects.equals(catching, that.catching);
    }

}
