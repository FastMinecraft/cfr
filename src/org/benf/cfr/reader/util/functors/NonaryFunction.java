package org.benf.cfr.reader.util.functors;

@FunctionalInterface
public interface NonaryFunction<T> {
    T invoke();
}
