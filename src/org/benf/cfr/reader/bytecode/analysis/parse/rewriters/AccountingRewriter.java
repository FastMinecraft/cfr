package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.collections.LazyMap;

import java.util.Map;

public class AccountingRewriter implements ExpressionRewriter {

    private final Map<StackSSALabel, Long> count = new LazyMap<>(
        new Object2ObjectLinkedOpenHashMap<StackSSALabel, Long>(),
        arg -> 0L
    );

    @Override
    public void handleStatement(StatementContainer statementContainer) {

    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

//    @Override
//    public AbstractAssignmentExpression rewriteExpression(AbstractAssignmentExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
//        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
//        return (AbstractAssignmentExpression) res;
//    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (lValue instanceof StackSSALabel) {
            return rewriteExpression((StackSSALabel) lValue, ssaIdentifiers, statementContainer, flags);
        }
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (flags != ExpressionRewriterFlags.LVALUE) {
//            System.out.println("Use of [" + lValue + "] in " + statementContainer);
            count.put(lValue, count.get(lValue) + 1);
        }
        return lValue;
    }

    public void flush() {
        for (Map.Entry<StackSSALabel, Long> entry : count.entrySet()) {
//            System.out.println("Usage count of " + entry.getKey() + " = " + entry.getValue());
            StackSSALabel stackSSALabel = entry.getKey();
            stackSSALabel.getStackEntry().forceUsageCount(entry.getValue());
        }
    }
}
