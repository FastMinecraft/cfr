package org.benf.cfr.reader.util;

import java.util.function.Consumer;

public class Optional<T> {
    private final T value;
    private final boolean set;
    private static final Optional Empty = new Optional();

    private Optional(T val) {
        value = val;
        set = true;
    }

    private Optional() {
        set = false;
        value = null;
    }

    public boolean isSet() {
        return set;
    }

    public T getValue() {
        return value;
    }

    public void then(Consumer<T> func) {
        func.accept(value);
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> empty() {
        //noinspection unchecked
        return (Optional<T>)Optional.Empty;
    }
}
