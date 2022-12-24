package org.benf.cfr.reader.util.collections;

import org.benf.cfr.reader.util.functors.UnaryFunction;

import java.util.*;

public class MapFactory {
    public static <X, Y> Map<X, Y> newMap() {
        return new HashMap<>();
    }

    public static <X, Y> Map<X, Y> newOrderedMap() {
        return new LinkedHashMap<>();
    }

    public static <X, Y> Map<X, Y> newIdentityMap() {
        return new IdentityHashMap<>();
    }

    public static <X, Y> Map<X, Y> newIdentityLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<>(MapFactory.<X, Y>newIdentityMap(), factory);
    }

    public static <X, Y> TreeMap<X, Y> newTreeMap() {
        return new TreeMap<>();
    }

    public static <X, Y> LazyMap<X, Y> newLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<>(MapFactory.<X, Y>newMap(), factory);
    }

    public static <X, Y> Map<X, Y> newLinkedLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<>(MapFactory.<X, Y>newOrderedMap(), factory);
    }

    public static <X, Y> Map<X, Y> newLazyMap(Map<X, Y> base, UnaryFunction<X, Y> factory) {
        return new LazyMap<>(base, factory);
    }

    public static <X, Y> Map<X, Y> newExceptionRetainingLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyExceptionRetainingMap<>(MapFactory.<X, Y>newMap(), factory);
    }

}
