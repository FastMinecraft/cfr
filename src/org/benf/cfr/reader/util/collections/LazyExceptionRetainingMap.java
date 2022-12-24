package org.benf.cfr.reader.util.collections;

import java.util.Map;
import java.util.function.Function;

public class LazyExceptionRetainingMap<X, Y> extends LazyMap<X, Y> {
    private final Map<X, RuntimeException> exceptionMap = MapFactory.newMap();

    LazyExceptionRetainingMap(Map<X, Y> inner, Function<X, Y> factory) {
        super(inner, factory);
    }

    @Override
    public Y get(Object o) {
        RuntimeException exception = exceptionMap.get(o);
        if (exception == null) {
            try {
                return super.get(o);
            } catch (RuntimeException e) {
                exception = e;
                //noinspection unchecked
                exceptionMap.put((X) o, e);
            }
        }
        throw exception;
    }
}
