package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpressionDefining;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.Map;

public class InstanceofMatchTidyingRewriter {
    private final Map<LocalVariable, Integer> locals = MapFactory.newMap();
    private final ObjectSet<LocalVariable> removeCandidates = new ObjectLinkedOpenHashSet<>();
    private final Map<LValue, ObjectList<StructuredStatement>> definitions = new Object2ObjectLinkedOpenHashMap<>();
    private StructuredStatement last;

    public static void rewrite(Op04StructuredStatement block) {
        new InstanceofMatchTidyingRewriter().doRewrite(block);
    }

    private void doRewrite(Op04StructuredStatement block) {
        ExpressionRewriterTransformer et = new SearchPass(new SearchPassRewriter());
        et.transform(block);
        removeCandidates.removeAll(locals.keySet());
        //noinspection SuspiciousMethodCalls
        removeCandidates.retainAll(definitions.keySet());
        if (removeCandidates.isEmpty()) return;
        et = new ExpressionRewriterTransformer(new AssignRemover());
        et.transform(block);
        for (ObjectList<StructuredStatement> definitionList : definitions.values()) {
            for (StructuredStatement definition : definitionList) {
                definition.getContainer().nopOut();
            }
        }
    }

    private class SearchPass extends ExpressionRewriterTransformer {
        SearchPass(ExpressionRewriter expressionRewriter) {
            super(expressionRewriter);
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (in instanceof StructuredDefinition) {
                LValue lvalue = ((StructuredDefinition) in).getLvalue();
                addDefinition(in, lvalue);
            }
            StructuredStatement res = super.transform(in, scope);
            last = res;
            return res;
        }
    }

    private void addDefinition(StructuredStatement in, LValue lvalue) {
        ObjectList<StructuredStatement> defl = definitions.get(lvalue);
        //noinspection Java8MapApi
        if (defl == null) {
            defl = new ObjectArrayList<>();
            definitions.put(lvalue, defl);
        }
        defl.add(in);
    }

    private class SearchPassRewriter extends AbstractExpressionRewriter {
        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof InstanceOfExpressionDefining expressionDefining) {
                Expression lhs = expressionDefining.getLhs();
                if (lhs instanceof AssignmentExpression && ((AssignmentExpression) lhs).getlValue() instanceof LocalVariable) {
                    ((AssignmentExpression) lhs).getrValue().applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
                    removeCandidates.add((LocalVariable) ((AssignmentExpression) lhs).getlValue());
                    return expression;
                } else if (last != null && lhs instanceof LValueExpression && ((LValueExpression) lhs).getLValue() instanceof LocalVariable lValue) {
                    if (last instanceof StructuredAssignment assigment) {
                        // We can only remove at ths point if Locals (lValue) == 1, in which case we substitute
                        // rValue for lValue in the instanceof, and decrement the usage to 0.
                        Expression rhs = assigment.getRvalue();
                        if (rhs instanceof LValueExpression &&
                            ((LValueExpression) rhs).getLValue() instanceof LocalVariable &&
                            locals.get(lValue) == 1 &&
                            lValue.equals(assigment.getLvalue())) {
                            removeCandidates.add(lValue);
                            locals.remove(lValue);
                            addDefinition(InstanceofMatchTidyingRewriter.this.last, lValue);
                            last = null;
                            return ((InstanceOfExpressionDefining) expression).withReplacedExpression(rhs);
                        }
                    }
                }

            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (lValue instanceof LocalVariable) {
                Integer prev = locals.get(lValue);
                locals.put((LocalVariable)lValue, prev == null ? 1 : prev+1);
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }
    }

    private class AssignRemover extends AbstractExpressionRewriter {
        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof InstanceOfExpressionDefining defining) {
                Expression lhs = defining.getLhs();
                //noinspection SuspiciousMethodCalls
                if (lhs instanceof AssignmentExpression && removeCandidates.contains(((AssignmentExpression) lhs).getlValue())) {
                    return defining.withReplacedExpression(((AssignmentExpression) lhs).getrValue());
                }
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}
