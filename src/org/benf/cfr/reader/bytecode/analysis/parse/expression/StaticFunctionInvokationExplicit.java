package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * A static call that doesn't necessarily exist, for a type we don't necessarily have.
 */
public class StaticFunctionInvokationExplicit extends AbstractFunctionInvokationExplicit {
    public StaticFunctionInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, String method, ObjectList<Expression> args) {
        super(loc, res, clazz, method, args);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, getArgs());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof StaticFunctionInvokationExplicit other)) return false;
        return getClazz().equals(other.getClazz()) && getMethod().equals(other.getMethod()) && getArgs().equals(other.getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(getClazz()).separator(".").print(getMethod()).separator("(");
        boolean first = true;
        for (Expression arg : getArgs()) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof StaticFunctionInvokationExplicit other)) return false;
        if (!constraint.equivalent(getMethod(), other.getMethod())) return false;
        if (!constraint.equivalent(getClazz(), other.getClazz())) return false;
        return constraint.equivalent(getArgs(), other.getArgs());
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new StaticFunctionInvokationExplicit(getLoc(), getInferredJavaType(), getClazz(), getMethod(), cloneHelper.replaceOrClone(getArgs()));
    }
}
