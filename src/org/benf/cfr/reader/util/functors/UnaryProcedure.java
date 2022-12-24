package org.benf.cfr.reader.util.functors;

@FunctionalInterface
public interface UnaryProcedure<T> {
    void call(T arg);
}
