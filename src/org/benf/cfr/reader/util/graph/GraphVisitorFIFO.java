package org.benf.cfr.reader.util.graph;

import java.util.function.BiConsumer;

public class GraphVisitorFIFO<T> extends AbstractGraphVisitorFI<T> {
    public GraphVisitorFIFO(T first, BiConsumer<T, GraphVisitor<T>> callee) {
        super(first, callee);
    }
}
