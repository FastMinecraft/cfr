package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Objects;

public class TernaryExpression extends AbstractExpression implements BoxingProcessor {
    private ConditionalExpression condition;
    private Expression lhs;
    private Expression rhs;

    public TernaryExpression(BytecodeLoc loc, ConditionalExpression condition, Expression lhs, Expression rhs) {
        this(loc, inferredType(lhs.getInferredJavaType(), rhs.getInferredJavaType()), condition, lhs, rhs);
    }

    public TernaryExpression(BytecodeLoc loc, InferredJavaType type, ConditionalExpression condition, Expression lhs, Expression rhs) {
        super(loc, type);
        this.condition = condition;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, condition, lhs, rhs);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        condition.collectTypeUsages(collector);
        lhs.collectTypeUsages(collector);
        rhs.collectTypeUsages(collector);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new TernaryExpression(getLoc(), (ConditionalExpression) cloneHelper.replaceOrClone(condition), cloneHelper.replaceOrClone(lhs), cloneHelper.replaceOrClone(rhs));
    }

    private static InferredJavaType inferredType(InferredJavaType a, InferredJavaType b) {
        // This is a hack - we should be able to avoid boxing issues.
        if (a.getJavaTypeInstance().getStackType() == StackType.REF ||
            b.getJavaTypeInstance().getStackType() == StackType.REF) {
            return InferredJavaType.combineOrClash(a, b).collapseTypeClash();
        } else {
            a.chain(b);
            return a;
        }
    }

    public ConditionalExpression getCondition() {
        return condition;
    }

    public Expression getLhs() {
        return lhs;
    }

    public Expression getRhs() {
        return rhs;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.CONDITIONAL;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        condition.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        d.operator(" ? ");
        lhs.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        d.operator(" : ");
        rhs.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        rhs = rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        lhs = lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        Expression replacementCondition = condition.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        if (replacementCondition != condition) throw new ConfusedCFRException("Can't yet support replacing conditions");

        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        condition = expressionRewriter.rewriteExpression(condition, ssaIdentifiers, statementContainer, flags);
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        rhs = expressionRewriter.rewriteExpression(rhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        rhs = expressionRewriter.rewriteExpression(rhs, ssaIdentifiers, statementContainer, flags);
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        condition = expressionRewriter.rewriteExpression(condition, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public Expression applyConditionOnlyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        condition = expressionRewriter.rewriteExpression(condition, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        condition.collectUsedLValues(lValueUsageCollector);
        lhs.collectUsedLValues(lValueUsageCollector);
        rhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        if (boxingRewriter.isUnboxedType(lhs)) {
            rhs = boxingRewriter.sugarUnboxing(rhs);
            return false;
        }
        if (boxingRewriter.isUnboxedType(rhs)) {
            lhs = boxingRewriter.sugarUnboxing(lhs);
            return false;
        }

        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TernaryExpression that = (TernaryExpression) o;

        if (!Objects.equals(condition, that.condition)) return false;
        if (!Objects.equals(lhs, that.lhs)) return false;
        return Objects.equals(rhs, that.rhs);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != getClass()) return false;
        TernaryExpression other = (TernaryExpression) o;
        if (!constraint.equivalent(condition, other.condition)) return false;
        if (!constraint.equivalent(lhs, other.lhs)) return false;
        return constraint.equivalent(rhs, other.rhs);
    }
}
