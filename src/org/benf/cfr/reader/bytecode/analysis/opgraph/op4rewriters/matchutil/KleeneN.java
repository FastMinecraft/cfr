package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public class KleeneN implements Matcher<StructuredStatement> {
    private final Matcher<StructuredStatement> inner;
    private final int nRequired;

    public KleeneN(int nRequired, Matcher<StructuredStatement> inner) {
        this.inner = inner;
        this.nRequired = nRequired;
    }

    @SafeVarargs
    public KleeneN(int nRequired, Matcher<StructuredStatement>... matchers) {
        this.inner = new MatchSequence(matchers);
        this.nRequired = nRequired;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        MatchIterator<StructuredStatement> mi = matchIterator.copy();

        int nMatches = 0;
        while (inner.match(mi, matchResultCollector)) {
            nMatches++;
        }

        if (nMatches >= nRequired) {
            matchIterator.advanceTo(mi);
            return true;
        }
        return false;
    }
}
