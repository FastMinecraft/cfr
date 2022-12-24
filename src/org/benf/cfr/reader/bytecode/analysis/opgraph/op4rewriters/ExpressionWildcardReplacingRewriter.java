package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;

import java.util.function.Supplier;

public class ExpressionWildcardReplacingRewriter extends AbstractExpressionRewriter {
    private final WildcardMatch wildcardMatch;
    private final Expression search;
    private final Supplier<Expression> replacementFunction;

    ExpressionWildcardReplacingRewriter(WildcardMatch wildcardMatch, Expression search, Supplier<Expression> replacementFunction) {
        this.wildcardMatch = wildcardMatch;
        this.search = search;
        this.replacementFunction = replacementFunction;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression == null) return null;
        if (search.equals(expression)) {
            Expression replacement = replacementFunction.get();
            if (replacement != null) {
                wildcardMatch.reset();
                return replacement;
            }
        }
        wildcardMatch.reset();
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

}
