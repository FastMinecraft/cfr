package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Objects;

public class NewObjectArray extends AbstractNewArray {
    private final ObjectList<Expression> dimSizes;
    private final JavaTypeInstance allocatedType;
    private final JavaTypeInstance resultType;
    private final int numDims;

    public NewObjectArray(BytecodeLoc loc, ObjectList<Expression> dimSizes, JavaTypeInstance resultInstance) {
        super(loc, new InferredJavaType(resultInstance, InferredJavaType.Source.EXPRESSION, true));
        this.dimSizes = dimSizes;
        this.allocatedType = resultInstance.getArrayStrippedType();
        this.resultType = resultInstance;
        this.numDims = resultInstance.getNumArrayDimensions();
        for (Expression size : dimSizes) {
            size.getInferredJavaType().useAsWithoutCasting(RawJavaType.INT);
        }
    }

    private NewObjectArray(BytecodeLoc loc, InferredJavaType inferredJavaType, JavaTypeInstance resultType, int numDims, JavaTypeInstance allocatedType, ObjectList<Expression> dimSizes) {
        super(loc, inferredJavaType);
        this.resultType = resultType;
        this.numDims = numDims;
        this.allocatedType = allocatedType;
        this.dimSizes = dimSizes;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, dimSizes);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new NewObjectArray(getLoc(), getInferredJavaType(), resultType, numDims, allocatedType, cloneHelper.replaceOrClone(dimSizes));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(allocatedType);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("new ").dump(allocatedType);
        for (Expression dimSize : dimSizes) {
            d.separator("[").dump(dimSize).separator("]");
        }
        for (int x = dimSizes.size(); x < numDims; ++x) {
            d.separator("[]");
        }
        return d;
    }

    @Override
    public int getNumDims() {
        return numDims;
    }

    @Override
    public int getNumSizedDims() {
        return dimSizes.size();
    }

    @Override
    public Expression getDimSize(int dim) {
        if (dim >= dimSizes.size()) throw new ConfusedCFRException("Out of bounds");
        return dimSizes.get(dim);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, dimSizes);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression dimSize : dimSizes) {
            dimSize.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewObjectArray that = (NewObjectArray) o;

        if (numDims != that.numDims) return false;
        if (!Objects.equals(allocatedType, that.allocatedType))
            return false;
        if (!Objects.equals(dimSizes, that.dimSizes)) return false;
        return Objects.equals(resultType, that.resultType);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        NewObjectArray other = (NewObjectArray) o;
        if (numDims != other.numDims) return false;
        if (!constraint.equivalent(dimSizes, other.dimSizes)) return false;
        if (!constraint.equivalent(allocatedType, other.allocatedType)) return false;
        return constraint.equivalent(resultType, other.resultType);
    }


}
