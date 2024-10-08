package org.benf.cfr.reader.util.graph;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.function.BiConsumer;

public class GraphVisitorDFS<T> implements GraphVisitor<T> {
    private final Collection<? extends T> start;
    private final Set<T> visited = new ObjectOpenHashSet<>();
    private final BiConsumer<T, GraphVisitor<T>> callee;
    private final LinkedList<T> pending = new LinkedList<>();
    private final LinkedList<T> enqueued = new LinkedList<>();
    private boolean aborted = false;

    public GraphVisitorDFS(T first, BiConsumer<T, GraphVisitor<T>> callee) {
        this.start = ObjectLists.singleton(first);
        this.callee = callee;
    }

    public GraphVisitorDFS(Collection<? extends T> first, BiConsumer<T, GraphVisitor<T>> callee) {
        this.start = new ObjectArrayList<>(first);
        this.callee = callee;
    }

    @Override
    public void enqueue(T next) {
        if (next == null) return;
        // These will be enqueued in the order they should be visited...
        enqueued.add(next);
    }

    @Override
    public void enqueue(Collection<? extends T> next) {
        for (T t : next) enqueue(t);
    }

    @Override
    public void abort() {
        enqueued.clear();
        pending.clear();
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
    public void process() {
        pending.clear();
        enqueued.clear();
        pending.addAll(start);
        while (!pending.isEmpty()) {
            T current = pending.removeFirst();
            if (!visited.contains(current)) {
                visited.add(current);
                callee.accept(current, this);
                // Prefix pending with enqueued.
                while (!enqueued.isEmpty()) pending.addFirst(enqueued.removeLast());
            }
        }

    }
}
