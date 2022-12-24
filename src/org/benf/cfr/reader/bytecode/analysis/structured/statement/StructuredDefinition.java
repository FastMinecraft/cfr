package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class StructuredDefinition extends AbstractStructuredStatement {

    private final LValue scopedEntity;

    public StructuredDefinition(LValue scopedEntity) {
        super(BytecodeLoc.NONE);
        this.scopedEntity = scopedEntity;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        scopedEntity.collectTypeUsages(collector);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        Class<?> clazz = scopedEntity.getClass();
        if (clazz == LocalVariable.class) {
            return LValue.Creation.dump(dumper, scopedEntity).endCodeln();
        } else if (clazz == SentinelLocalClassLValue.class) {
            JavaTypeInstance type = ((SentinelLocalClassLValue) scopedEntity).getLocalClassType().getDeGenerifiedType();
            if (type instanceof JavaRefTypeInstance) {
                ClassFile classFile = ((JavaRefTypeInstance) type).getClassFile();
                if (classFile != null) {
                    return classFile.dumpAsInlineClass(dumper);
                }
            }
        }
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
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
    }

    public LValue getLvalue() {
        return scopedEntity;
    }

    @Override
    public ObjectList<LValue> findCreatedHere() {
        return ObjectList.of(new LValue[]{ scopedEntity });
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
        if (!(o instanceof StructuredDefinition other)) return false;
        return scopedEntity.equals(other.scopedEntity);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

}

