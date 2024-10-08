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
 * A constructor call that doesn't necessarily exist, for a type we don't necessarily have.
 */
public class ConstructorInvokationExplicit extends AbstractFunctionInvokationExplicit {

    ConstructorInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, ObjectList<Expression> args) {
        super(loc, res, clazz, null, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ConstructorInvokationExplicit other)) return false;
        return getClazz().equals(other.getClazz()) && getArgs().equals(other.getArgs());
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("new ").dump(getClazz()).separator("(");
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
        if (!(o instanceof ConstructorInvokationExplicit other)) return false;
        if (!constraint.equivalent(getClazz(), other.getClazz())) return false;
        return constraint.equivalent(getArgs(), other.getArgs());
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ConstructorInvokationExplicit(getLoc(), getInferredJavaType(), getClazz(), cloneHelper.replaceOrClone(getArgs()));
    }
}
