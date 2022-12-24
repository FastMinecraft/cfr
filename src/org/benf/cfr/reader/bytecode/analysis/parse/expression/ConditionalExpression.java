package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface ConditionalExpression extends Expression {
    ConditionalExpression getNegated();

    int getSize(Precedence outerPrecedence);

    ConditionalExpression getDemorganApplied(boolean amNegating);

    /*
     * Normalise tree layout so ((a || b) || c) --> (a || (b || c)).
     * This is useful so any patterns can know what they're matching against.
     */
    ConditionalExpression getRightDeep();

    ObjectSet<LValue> getLoopLValues();

    ConditionalExpression optimiseForType();

    ConditionalExpression simplify();
}
