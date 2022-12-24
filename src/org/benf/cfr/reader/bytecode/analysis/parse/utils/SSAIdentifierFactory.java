package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.Map;
import java.util.function.Function;

public class SSAIdentifierFactory<KEYTYPE, CMPTYPE> {
    private final Map<KEYTYPE, Integer> nextIdentFor = MapFactory.newLazyMap(
            MapFactory.newOrderedMap(),
        ignore -> 0
    );

    private final Function<KEYTYPE, CMPTYPE> typeComparisonFunction;

    public SSAIdentifierFactory(Function<KEYTYPE, CMPTYPE> typeComparisonFunction) {
        this.typeComparisonFunction = typeComparisonFunction;
    }

    public SSAIdent getIdent(KEYTYPE lValue) {
        int val = nextIdentFor.get(lValue);
        nextIdentFor.put(lValue, val + 1);
        return new SSAIdent(val, typeComparisonFunction == null ? null : typeComparisonFunction.apply(lValue));
    }
}
