package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class StructuredAssignment extends AbstractStructuredStatement implements BoxingProcessor {

    private LValue lvalue;
    private Expression rvalue;
    private boolean creator;

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = false;
    }

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue, boolean creator) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = creator;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, rvalue);
    }

    public boolean isCreator(LValue lvalue) {
        return creator && this.lvalue.equals(lvalue);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        lvalue.collectTypeUsages(collector);
        collector.collectFrom(rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (creator) {
            if (lvalue.isFinal()) dumper.print("final ");
            LValue.Creation.dump(dumper, lvalue);
        } else {
            dumper.dump(lvalue);
        }
        dumper.operator(" = ").dump(rvalue).endCodeln();
        return dumper;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(ObjectList<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        rvalue.collectUsedLValues(scopeDiscoverer);
        // todo - what if rvalue is an assignment?
        lvalue.collectLValueAssignments(rvalue, getContainer(), scopeDiscoverer);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {

        if (scopedEntity instanceof LocalVariable localVariable) {
            if (!localVariable.equals(lvalue)) {
                throw new IllegalArgumentException("Being asked to mark creator for wrong variable");
            }
            creator = true;
            InferredJavaType inferredJavaType = localVariable.getInferredJavaType();
            if (inferredJavaType.isClash()) {
                inferredJavaType.collapseTypeClash();
            }
        }
    }

    @Override
    public ObjectList<LValue> findCreatedHere() {
        if (creator) {
            return ObjectList.of(new LValue[]{ lvalue });
        } else {
            return null;
        }
    }

    public LValue getLvalue() {
        return lvalue;
    }

    public Expression getRvalue() {
        return rvalue;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!this.equals(o)) return false;
        matchIterator.advance();
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof StructuredAssignment other)) return false;
        if (!lvalue.equals(other.lvalue)) return false;
        return rvalue.equals(other.rvalue);
//        if (isCreator != other.isCreator) return false;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(getContainer());
        lvalue = expressionRewriter.rewriteExpression(lvalue, null, this.getContainer(), null);
        rvalue = expressionRewriter.rewriteExpression(rvalue, null, this.getContainer(), null);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        rvalue = boxingRewriter.sugarNonParameterBoxing(rvalue, lvalue.getInferredJavaType().getJavaTypeInstance());
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lvalue = lvalue.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

}

