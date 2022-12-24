package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

import java.util.Map;

public class ScopeDiscoverInfoCache {
    private final Map<StructuredStatement, Boolean> tests = new Reference2ObjectOpenHashMap<>();

    public Boolean get(StructuredStatement structuredStatement) {
        return tests.get(structuredStatement);
    }

    public void put(StructuredStatement structuredStatement, Boolean b) {
        tests.put(structuredStatement, b);
    }

    boolean anyFound() {
        for (Boolean value : tests.values()) {
            if (value) return true;
        }
        return false;
    }
}
