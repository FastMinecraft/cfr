package org.benf.cfr.reader.util.functors;

@FunctionalInterface
public interface UnaryFunction<X,Y> {
    Y invoke(X arg);
}
