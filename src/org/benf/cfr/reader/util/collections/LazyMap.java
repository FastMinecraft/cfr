package org.benf.cfr.reader.util.collections;

import java.util.Collection;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Set;
import java.util.function.Function;

public class LazyMap<X, Y> implements Map<X, Y> {
    private final Map<X, Y> inner;
    private final Function<X, Y> factory;

    public LazyMap(Map<X, Y> inner, Function<X, Y> factory) {
        this.inner = inner;
        this.factory = factory;
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return inner.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return inner.containsValue(o);
    }

    @Override
    public Y get(Object o) {
        //noinspection unchecked
        return inner.computeIfAbsent((X) o, factory);
    }

    @Override
    public Y put(X x, Y y) {
        return inner.put(x, y);
    }

    @Override
    public Y remove(Object o) {
        return inner.remove(o);
    }

    @Override
    public void putAll(Map<? extends X, ? extends Y> map) {
        inner.putAll(map);
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public Set<X> keySet() {
        return inner.keySet();
    }

    @Override
    public Collection<Y> values() {
        return inner.values();
    }

    @Override
    public Set<Entry<X, Y>> entrySet() {
        return inner.entrySet();
    }

    public Y getWithout(X x) {
        return inner.get(x);
    }
}
