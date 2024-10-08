package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Objects;

/**
 * Needs some work here to unify LambdaExpression and LambdaExpressionFallback.
 */
public class LambdaExpressionFallback extends AbstractExpression implements LambdaExpressionCommon {

    private final JavaTypeInstance callClassType;
    private final MethodPrototype lambdaFn;
    private final ObjectList<JavaTypeInstance> targetFnArgTypes;
    private final ObjectList<Expression> curriedArgs;
    private boolean instance;
    private final boolean methodRef;

    private String lambdaFnName() {
        String lambdaFnName = lambdaFn.getName();
        return lambdaFnName.equals(MiscConstants.INIT_METHOD) ? MiscConstants.NEW : lambdaFnName;
    }

    public LambdaExpressionFallback(BytecodeLoc loc, JavaTypeInstance callClassType, InferredJavaType castJavaType, MethodPrototype lambdaFn, ObjectList<JavaTypeInstance> targetFnArgTypes, ObjectList<Expression> curriedArgs, boolean instance) {
        super(loc, castJavaType);
        this.callClassType = callClassType;
        this.lambdaFn = lambdaFn;
        this.targetFnArgTypes = targetFnArgTypes;
        this.curriedArgs = curriedArgs;
        this.instance = instance;
        boolean isMethodRef = false;
        switch (curriedArgs.size()) {
            case 0 -> {
                isMethodRef = true;
                if (instance) {
                    /* Don't really understand what's going on here.... */
                    this.instance = false;
                }
            }
            case 1 -> {
                // we could just check if we're an instance function.  Be a bit more paranoid.
                if (instance && lambdaFn.isInstanceMethod()) {
                    // But use degenerified types, don't think we have to be THAT paranoid.
                    JavaTypeInstance thisType = lambdaFn.getClassType().getDeGenerifiedType();
                    JavaTypeInstance curriedType = curriedArgs.get(0).getInferredJavaType().getJavaTypeInstance().getDeGenerifiedType();
                    if (curriedType.implicitlyCastsTo(thisType, null)) {
                        isMethodRef = true;
                    }
                }
            }
        }
        this.methodRef = isMethodRef;
    }

    private LambdaExpressionFallback(BytecodeLoc loc, InferredJavaType inferredJavaType, boolean methodRef, boolean instance, ObjectList<Expression> curriedArgs, ObjectList<JavaTypeInstance> targetFnArgTypes, MethodPrototype lambdaFn, JavaTypeInstance callClassType) {
        super(loc, inferredJavaType);
        this.methodRef = methodRef;
        this.instance = instance;
        this.curriedArgs = curriedArgs;
        this.targetFnArgTypes = targetFnArgTypes;
        this.lambdaFn = lambdaFn;
        this.callClassType = callClassType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, curriedArgs);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LambdaExpressionFallback(getLoc(), getInferredJavaType(), methodRef, instance, cloneHelper.replaceOrClone(curriedArgs), targetFnArgTypes, lambdaFn, callClassType);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(targetFnArgTypes);
        collector.collectFrom(curriedArgs);
        collector.collect(callClassType);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(curriedArgs, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(curriedArgs, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    // Special precedence level - otherwise we lose the extra brackets in test Saturn.
    // ((Supplier<Saturn>)Saturn::new)::get;
    @Override
    public Precedence getPrecedence() {
        return Precedence.LAMBDA;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        String name = lambdaFnName();
        //noinspection StringEquality
        boolean special = name == MiscConstants.NEW;
        if (methodRef) {
            if (instance) {
                curriedArgs.get(0).dumpWithOuterPrecedence(d, getPrecedence(), Troolean.TRUE).print("::").methodName(name, lambdaFn, special, false);
            } else {
                d.dump(callClassType).print("::").methodName(name, lambdaFn, special, false);
            }
        } else {
            int n = targetFnArgTypes.size();
            boolean multi = n != 1;
            if (multi) {
                d.separator("(");
            }
            ObjectList<String> args = new ObjectArrayList<>(n);
            for (int x = 0; x < n; ++x) {
                if (x > 0) d.separator(", ");
                String arg = "arg_" + x;
                args.add(arg);
                d.parameterName(arg, arg, lambdaFn, x, true);
            }
            if (multi) {
                d.separator(")");
            }
            d.operator(" -> ");
            if (instance) {
                curriedArgs.get(0).dumpWithOuterPrecedence(d, getPrecedence(), Troolean.TRUE).separator(".").methodName(name, lambdaFn, special, false);
            } else {
                d.dump(callClassType).print('.').methodName(name, lambdaFn, special, false);
            }
            d.separator("(");
            boolean first = true;
            for (int x = instance ? 1 : 0, cnt = curriedArgs.size(); x < cnt; ++x) {
                Expression c = curriedArgs.get(x);
                first = StringUtils.comma(first, d);
                d.dump(c);
            }
            for (int x = 0; x < n; ++x) {
                first = StringUtils.comma(first, d);
                String arg = args.get(x);
                d.identifier(arg, arg, false);
            }
            d.separator(")");
        }
        return d;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaExpressionFallback that = (LambdaExpressionFallback) o;

        if (methodRef != that.methodRef) return false;
        if (instance != that.instance) return false;
        if (!Objects.equals(callClassType, that.callClassType))
            return false;
        if (!Objects.equals(curriedArgs, that.curriedArgs)) return false;
        if (!Objects.equals(lambdaFn, that.lambdaFn)) return false;
        return Objects.equals(targetFnArgTypes, that.targetFnArgTypes);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        LambdaExpressionFallback other = (LambdaExpressionFallback) o;
        if (instance != other.instance) return false;
        if (methodRef != other.methodRef) return false;
        if (!constraint.equivalent(lambdaFn, other.lambdaFn)) return false;
        return constraint.equivalent(curriedArgs, other.curriedArgs);
    }


}
