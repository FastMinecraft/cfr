package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Collection;
import java.util.LinkedList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

// This is more about being a queue than being a set, i.e. it's not convenient to use LinkedHashSet.
public class UniqueSeenQueue<T> {
    private final LinkedList<T> ll;
    private final ObjectSet<T> llItems;
    private final ObjectSet<T> seen;

    // Items in c should be unique.
    public UniqueSeenQueue(Collection<? extends T> c) {
        this.llItems = new ObjectOpenHashSet<>();
        this.ll = new LinkedList<>();
        this.seen = new ObjectOpenHashSet<>();
        ll.addAll(c);
        llItems.addAll(c);
        seen.addAll(c);
    }

    public boolean isEmpty() {
        return ll.isEmpty();
    }

    public T removeFirst() {
        T res = ll.removeFirst();
        llItems.remove(res);
        return res;
    }

    public boolean add(T c) {
        if (llItems.add(c)) {
            seen.add(c);
            ll.add(c);
            return true;
        }
        return false;
    }

    public boolean addIfUnseen(T c) {
        if (seen.add(c)) {
            llItems.add(c);
            ll.add(c);
            return true;
        }
        return false;
    }

    public boolean add(T c, boolean ifUnseen) {
        if (ifUnseen) return addIfUnseen(c);
        return add(c);
    }

    public void addAll(Collection<? extends T> ts) {
        for (T t : ts) {
            add(t);
        }
    }
}
