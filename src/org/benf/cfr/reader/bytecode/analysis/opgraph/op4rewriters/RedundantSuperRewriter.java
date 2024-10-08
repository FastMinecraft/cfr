package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.util.collections.Functional;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class RedundantSuperRewriter implements Op04Rewriter {

    public RedundantSuperRewriter() {
    }

    protected ObjectList<Expression> getSuperArgs(WildcardMatch wcm) {
        return null;
    }

    protected ObjectSet<LValue> getDeclarationsToNop(WildcardMatch wcm) {
        return null;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        ObjectList<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        WildcardMatch wcm1 = new WildcardMatch();

        Matcher<StructuredStatement> m = new CollectMatch("ass1", new StructuredExpressionStatement(BytecodeLoc.NONE, wcm1.getSuperFunction("s1", getSuperArgs(wcm1)), false));


        MatchIterator<StructuredStatement> mi = new MatchIterator<>(structuredStatements);
        MatchResultCollector collector = new SuperResultCollector(wcm1, structuredStatements);
        while (mi.hasNext()) {
            mi.advance();
            if (m.match(mi, collector)) {
                return;
            }
        }
    }

    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        // Does the super function have no arguments, after we ignore synthetic parameters?
        return superInvokation.isEmptyIgnoringSynthetics();
    }

    private class SuperResultCollector extends AbstractMatchResultIterator {

        private final WildcardMatch wcm;
        private final ObjectList<StructuredStatement> structuredStatements;

        private SuperResultCollector(WildcardMatch wcm, ObjectList<StructuredStatement> structuredStatements) {
            this.wcm = wcm;
            this.structuredStatements = structuredStatements;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            SuperFunctionInvokation superInvokation = wcm.getSuperFunction("s1").getMatch();

            if (canBeNopped(superInvokation)) {
                statement.getContainer().nopOut();
                ObjectSet<LValue> declarationsToNop = getDeclarationsToNop(wcm);
                if (declarationsToNop != null) {
                    ObjectList<StructuredStatement> decls = Functional.filter(structuredStatements,
                        in -> (in instanceof StructuredDefinition)
                    );
                    for (StructuredStatement decl : decls) {
                        StructuredDefinition defn = (StructuredDefinition) decl;
                        if (declarationsToNop.contains(defn.getLvalue())) {
                            defn.getContainer().nopOut();
                        }
                    }
                }
            }

        }
    }
}
