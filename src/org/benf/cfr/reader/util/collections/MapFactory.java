package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.*;
import java.util.function.Function;

public class MapFactory {
    public static <X, Y> Map<X, Y> newMap() {
        return new HashMap<>();
    }

    public static <X, Y> Map<X, Y> newIdentityMap() {
        return new IdentityHashMap<>();
    }

    public static <X, Y> Map<X, Y> newIdentityLazyMap(Function<X, Y> factory) {
        return new LazyMap<>(MapFactory.newIdentityMap(), factory);
    }

    public static <X, Y> TreeMap<X, Y> newTreeMap() {
        return new TreeMap<>();
    }

    public static <X, Y> LazyMap<X, Y> newLazyMap(Function<X, Y> factory) {
        return new LazyMap<>(MapFactory.newMap(), factory);
    }

    public static <X, Y> Map<X, Y> newLinkedLazyMap(Function<X, Y> factory) {
        return new LazyMap<>(new Object2ObjectLinkedOpenHashMap<X, Y>(), factory);
    }

    public static <X, Y> Map<X, Y> newLazyMap(Map<X, Y> base, Function<X, Y> factory) {
        return new LazyMap<>(base, factory);
    }

    public static <X, Y> Map<X, Y> newExceptionRetainingLazyMap(Function<X, Y> factory) {
        return new LazyExceptionRetainingMap<>(MapFactory.newMap(), factory);
    }

}
