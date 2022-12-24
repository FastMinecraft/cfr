package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Set;

public class EnumSuperRewriter extends RedundantSuperRewriter {
    @Override
    protected ObjectList<Expression> getSuperArgs(WildcardMatch wcm) {
        ObjectList<Expression> res = new ObjectArrayList<>();
        res.add(wcm.getExpressionWildCard("enum_a"));
        res.add(wcm.getExpressionWildCard("enum_b"));
        return res;
    }

    private static LValue getLValue(WildcardMatch wcm, String name) {
        Expression e = wcm.getExpressionWildCard(name).getMatch();
        while (e instanceof CastExpression) {
            e = ((CastExpression) e).getChild();
        }
        if (!(e instanceof LValueExpression)) {
            throw new IllegalStateException();
        }
        return ((LValueExpression) e).getLValue();
    }

    protected Set<LValue> getDeclarationsToNop(WildcardMatch wcm) {
        Set<LValue> res = new ObjectOpenHashSet<>();
        res.add(getLValue(wcm, "enum_a"));
        res.add(getLValue(wcm, "enum_b"));
        return res;
    }

    @Override
    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        return true;
    }
}
