package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class BooleanOperation extends AbstractExpression implements ConditionalExpression {
    private ConditionalExpression lhs;
    private ConditionalExpression rhs;
    private final BoolOp op;

    public BooleanOperation(BytecodeLoc loc, ConditionalExpression lhs, ConditionalExpression rhs, BoolOp op) {
        super(loc, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION));
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new BooleanOperation(
                getLoc(),
                (ConditionalExpression) cloneHelper.replaceOrClone(lhs),
                (ConditionalExpression) cloneHelper.replaceOrClone(rhs),
                op);

    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, lhs, rhs);
    }

    public ConditionalExpression getLhs() {
        return lhs;
    }

    public ConditionalExpression getRhs() {
        return rhs;
    }

    public BoolOp getOp() {
        return op;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        lhs.collectTypeUsages(collector);
        rhs.collectTypeUsages(collector);
    }

    @Override
    public int getSize(Precedence outerPrecedence) {
        Precedence precedence = getPrecedence();
        int initial = outerPrecedence.compareTo(precedence) < 0 ? 2 : 0;
        return initial + lhs.getSize(precedence) + 2 + rhs.getSize(precedence);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (lValueRewriter.needLR()) {
            lhs = (ConditionalExpression) lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            rhs = (ConditionalExpression) rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        } else {
            rhs = (ConditionalExpression) rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            lhs = (ConditionalExpression) lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        rhs = expressionRewriter.rewriteExpression(rhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        rhs = expressionRewriter.rewriteExpression(rhs, ssaIdentifiers, statementContainer, flags);
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public Expression applyLHSOnlyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Precedence getPrecedence() {
        return op.getPrecedence();
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        lhs.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.TRUE);
        d.print(" ").operator(op.getShowAs()).print(" ");
        rhs.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.FALSE);
        return d;
    }

    @Override
    public ConditionalExpression getNegated() {
        return new NotOperation(getLoc(),this);
    }

    @Override
    public ConditionalExpression getDemorganApplied(boolean amNegating) {
        return new BooleanOperation(getLoc(), lhs.getDemorganApplied(amNegating), rhs.getDemorganApplied(amNegating), amNegating ? op.getDemorgan() : op);
    }

    @Override
    public ConditionalExpression getRightDeep() {
        // transform (a x1 b) x2 c (where this is x2) to a x2 (b x1 c)
        // todo - no allocations here but this feels like there might be a n^2 case.
        while (lhs instanceof BooleanOperation lbool) {
            if (lbool.op == op) {
                ConditionalExpression a = lbool.lhs;
                ConditionalExpression b = lbool.rhs;
                ConditionalExpression c = rhs;
                lhs = a;
                lbool.lhs = b;
                lbool.rhs = c;
                rhs = lbool;
            } else {
                break;
            }
        }
        lhs = lhs.getRightDeep();
        rhs = rhs.getRightDeep();
        return this;
    }

    public static ConditionalExpression makeRightDeep(ObjectList<ConditionalExpression> c, BoolOp op) {
        ConditionalExpression res = c.get(c.size()-1);
        for (int x=c.size()-2; x>=0;x--) {
            res = new BooleanOperation(BytecodeLoc.NONE, /* lose info :( */ c.get(x), res, op);
        }
        return res;
    }

    @Override
    public ObjectSet<LValue> getLoopLValues() {
        ObjectSet<LValue> res = new ObjectOpenHashSet<>();
        res.addAll(lhs.getLoopLValues());
        res.addAll(rhs.getLoopLValues());
        return res;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        lhs.collectUsedLValues(lValueUsageCollector);
        rhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public ConditionalExpression optimiseForType() {
        lhs = lhs.optimiseForType();
        rhs = rhs.optimiseForType();
        return this;
    }

    @Override
    public ConditionalExpression simplify() {
        return ConditionalUtils.simplify(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BooleanOperation that)) return false;

        if (!lhs.equals(that.lhs)) return false;
        if (op != that.op) return false;
        return rhs.equals(that.rhs);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        BooleanOperation other = (BooleanOperation) o;
        if (op != other.op)
            if (!constraint.equivalent(lhs, other.lhs)) return false;
        return constraint.equivalent(rhs, other.rhs);
    }

    private static Boolean getComputed(Expression e, Map<LValue, Literal> display) {
        Literal lv = e.getComputedLiteral(display);
        if (lv == null) return null;
        return lv.getValue().getMaybeBoolValue();
    }

    /*
     * Be careful to short circuit the computation correctly.
     */
    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        Boolean lb = getComputed(lhs, display);
        if (lb == null) return null;
        switch (op) {
            case AND -> {
                Boolean rb = getComputed(rhs, display);
                if (rb == null) return null;
                return (lb && rb) ? Literal.TRUE : Literal.FALSE;
            }
            case OR -> {
                if (lb) return Literal.TRUE;
                Boolean rb = getComputed(rhs, display);
                if (rb == null) return null;
                return (rb) ? Literal.TRUE : Literal.FALSE;
            }
            default -> {
                return null;
            }
        }
    }
}
