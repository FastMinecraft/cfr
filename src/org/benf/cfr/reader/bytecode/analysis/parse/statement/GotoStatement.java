package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public class GotoStatement extends JumpingStatement {

    private JumpType jumpType;

    public GotoStatement(BytecodeLoc loc) {
        super(loc);
        this.jumpType = JumpType.GOTO;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        try {
            return dumper.print("" + jumpType + " " + getJumpTarget().getContainer().getLabel() + ";").newln();
        } catch (Exception e) {
            return dumper.print("!!! " + jumpType + " bad target");
        }
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        GotoStatement res = new GotoStatement(getLoc());
        res.jumpType = jumpType;
        return res;
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
    public JumpType getJumpType() {
        return jumpType;
    }

    @Override
    public void setJumpType(JumpType jumpType) {
        this.jumpType = jumpType;
    }

    @Override
    public Statement getJumpTarget() {
        return getTargetStatement(0);
    }

    @Override
    public boolean isConditional() {
        return false;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    protected BlockIdentifier getTargetStartBlock() {
        Statement statement = getJumpTarget();
        if (statement instanceof WhileStatement whileStatement) {
            return whileStatement.getBlockIdentifier();
        } else if (statement instanceof ForStatement forStatement) {
            return forStatement.getBlockIdentifier();
        } else if (statement instanceof ForIterStatement forStatement) {
            return forStatement.getBlockIdentifier();
        } else {
            BlockIdentifier blockStarted = statement.getContainer().getBlockStarted();
            if (blockStarted != null) {
                switch (blockStarted.getBlockType()) {
                    case UNCONDITIONALDOLOOP, DOLOOP -> {
                        return blockStarted;
                    }
                }
            }
            throw new ConfusedCFRException("CONTINUE without a while " + statement.getClass());
        }
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        switch (jumpType) {
            case END_BLOCK, GOTO_OUT_OF_TRY -> {
                return StructuredComment.EMPTY_COMMENT;
            }
            case GOTO, GOTO_OUT_OF_IF -> {
                return new UnstructuredGoto(getLoc());
            }
            case CONTINUE -> {
                return new UnstructuredContinue(getLoc(), getTargetStartBlock());
            }
            case BREAK -> {
                return new UnstructuredBreak(getLoc(), getJumpTarget().getContainer().getBlocksEnded());
            }
            case BREAK_ANONYMOUS -> {
                Statement target = getJumpTarget();
                if (!(target instanceof AnonBreakTarget anonBreakTarget)) {
                    throw new IllegalStateException("Target of anonymous break unexpected.");
                }
                BlockIdentifier breakFrom = anonBreakTarget.getBlockIdentifier();
                return new UnstructuredAnonymousBreak(getLoc(), breakFrom);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GotoStatement that = (GotoStatement) o;

        return jumpType == that.jumpType;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GotoStatement that = (GotoStatement) o;
        return constraint.equivalent(jumpType, that.jumpType);
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }
}
