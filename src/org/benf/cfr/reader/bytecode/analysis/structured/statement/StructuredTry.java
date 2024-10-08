package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class StructuredTry extends AbstractStructuredStatement {
    private Op04StructuredStatement tryBlock;
    private final ObjectList<Op04StructuredStatement> catchBlocks = new ObjectArrayList<>();
    private Op04StructuredStatement finallyBlock;
    private final BlockIdentifier tryBlockIdentifier;
    private ObjectList<Op04StructuredStatement> resourceBlock;

    public StructuredTry(Op04StructuredStatement tryBlock, BlockIdentifier tryBlockIdentifier) {
        super(BytecodeLoc.NONE);
        this.tryBlock = tryBlock;
        this.finallyBlock = null;
        this.tryBlockIdentifier = tryBlockIdentifier;
        this.resourceBlock = null;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    public void addResources(ObjectList<Op04StructuredStatement> resources) {
        if (resourceBlock == null) resourceBlock = new ObjectArrayList<>();
        resourceBlock.addAll(resources);
    }

    public ObjectList<Op04StructuredStatement> getResources() {
        return resourceBlock;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasResources() {
        return resourceBlock != null;
    }

    public Op04StructuredStatement getTryBlock() {
        return tryBlock;
    }

    public ObjectList<Op04StructuredStatement> getCatchBlocks() {
        return catchBlocks;
    }

    public void clearCatchBlocks() {
        catchBlocks.clear();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("try ");
        if (resourceBlock != null) {
            dumper.separator("(");
            boolean first = true;
            for (Op04StructuredStatement resource : resourceBlock) {
                if (!first) dumper.print("     ");
                resource.dump(dumper);
                first = false;
            }
            dumper.removePendingCarriageReturn();
            dumper.separator(")");
        }
        tryBlock.dump(dumper);
        for (Op04StructuredStatement catchBlock : catchBlocks) {
            dumper.removePendingCarriageReturn().separator(" ");
            catchBlock.dump(dumper);
        }
        if (finallyBlock != null) {
            dumper.removePendingCarriageReturn().separator(" ");
            finallyBlock.dump(dumper);
        }
        return dumper;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(tryBlock);
        collector.collectFrom(catchBlocks);
        collector.collectFrom(finallyBlock);
        collector.collectFrom(resourceBlock);
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    void addCatch(Op04StructuredStatement catchStatement) {
        catchBlocks.add(catchStatement);
    }

    public void setFinally(Op04StructuredStatement finallyBlock) {
        this.finallyBlock = finallyBlock;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        if (resourceBlock != null) {
            for (Op04StructuredStatement resource : resourceBlock) {
                resource.transform(transformer, scope);
            }
        }
        tryBlock.transform(transformer, scope);
        for (Op04StructuredStatement catchBlock : catchBlocks) {
            catchBlock.transform(transformer, scope);
        }
        if (finallyBlock != null) {
            finallyBlock.transform(transformer, scope);
        }
    }

    @Override
    public void linearizeInto(ObjectList<StructuredStatement> out) {
        out.add(this);
        if (resourceBlock != null) {
            // TODO: Synthetic 'resource' markers?
            for (Op04StructuredStatement resource : resourceBlock) {
                out.add(resource.getStatement());
            }
        }
        tryBlock.linearizeStatementsInto(out);
        for (Op04StructuredStatement catchBlock : catchBlocks) {
            catchBlock.linearizeStatementsInto(out);
        }
        if (finallyBlock != null) {
            finallyBlock.linearizeStatementsInto(out);
        }

    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (resourceBlock != null) {
            scopeDiscoverer.enterBlock(this);
            for (Op04StructuredStatement resource : resourceBlock) {
                scopeDiscoverer.processOp04Statement(resource);
            }
        }
        scopeDiscoverer.processOp04Statement(tryBlock);
        for (Op04StructuredStatement catchBlock : catchBlocks) {
            scopeDiscoverer.processOp04Statement(catchBlock);
        }
        if (finallyBlock != null) {
            scopeDiscoverer.processOp04Statement(finallyBlock);
        }
        if (resourceBlock != null) {
            scopeDiscoverer.leaveBlock(this);
        }
    }

    @Override
    public boolean isRecursivelyStructured() {
        if (resourceBlock != null) {
            for (Op04StructuredStatement resource: resourceBlock) {
                if (!resource.isFullyStructured()) return false;
            }
        }
        if (!tryBlock.isFullyStructured()) return false;
        for (Op04StructuredStatement catchBlock : catchBlocks) {
            if (!catchBlock.isFullyStructured()) return false;
        }
        if (finallyBlock != null) {
            return finallyBlock.isFullyStructured();
        }
        return true;
    }

    public Op04StructuredStatement getFinallyBlock() {
        return finallyBlock;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredTry)) return false;
        // we don't actually check any equality for a match.
        matchIterator.advance();
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    private boolean isPointlessTry() {
        if (!catchBlocks.isEmpty()) return false;
        if (finallyBlock == null) return true;
        // If finally block is empty, we can remove.
        if (!(finallyBlock.getStatement() instanceof StructuredFinally structuredFinally)) return false;
        Op04StructuredStatement finallyCode = structuredFinally.getCatchBlock();
        if (!(finallyCode.getStatement() instanceof Block block)) return false;
        return block.isEffectivelyNOP();
    }

    private boolean isJustTryCatchThrow() {
        if (resourceBlock != null) return false;
        if (finallyBlock != null) return false;
        if (catchBlocks.size() != 1) return false;
        Op04StructuredStatement catchBlock = catchBlocks.get(0);
        StructuredStatement catchS = catchBlock.getStatement();
        if (!(catchS instanceof StructuredCatch structuredCatch)) return false;
        return structuredCatch.isRethrow();
    }


    @Override
    public boolean inlineable() {
        // split out for breakpointing.
        return isPointlessTry() || isJustTryCatchThrow();
    }

    public BlockIdentifier getTryBlockIdentifier() {
        return tryBlockIdentifier;
    }

    @Override
    public Op04StructuredStatement getInline() {
        return tryBlock;
    }

    public void setTryBlock(Op04StructuredStatement tryBlock) {
        this.tryBlock = tryBlock;
    }
}
