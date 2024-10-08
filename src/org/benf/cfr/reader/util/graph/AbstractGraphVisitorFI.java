package org.benf.cfr.reader.util.graph;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.BiConsumer;

public abstract class AbstractGraphVisitorFI<T> implements GraphVisitor<T> {
    private final LinkedList<T> toVisit = new LinkedList<>();
    private final ObjectSet<T> visited = new ObjectOpenHashSet<>();
    private final BiConsumer<T, GraphVisitor<T>> callee;
    private boolean aborted = false;

    AbstractGraphVisitorFI(T first, BiConsumer<T, GraphVisitor<T>> callee) {
        add(first);
        this.callee = callee;
    }

    private void add(T next) {
        if (next == null) return;
        if (!visited.contains(next)) {
            toVisit.add(next);
            visited.add(next);
        }
    }

    @Override
    public void abort() {
        toVisit.clear();
        aborted = true;
    }

    @Override
    public boolean wasAborted() {
        return aborted;
    }

    @Override
    public Collection<T> getVisitedNodes() {
        return visited;
    }

    @Override
    public void enqueue(T next) {
        add(next);
    }

    @Override
    public void enqueue(Collection<? extends T> next) {
        for (T t : next) enqueue(t);
    }

    @Override
    public void process() {
        do {
            T next = toVisit.removeFirst();
            callee.accept(next, this);
        } while (!toVisit.isEmpty());
    }
}
