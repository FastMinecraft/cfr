package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ListFactory {
    public static <X> List<X> uniqueList(Collection<X> list) {
        return new ObjectArrayList<>(SetFactory.newOrderedSet(list));
    }

    /** Note that you can't expect to mutate the result. */
    public static <X> List<X> combinedOptimistic(List<X> a, List<X> b) {
        if (a == null || a.isEmpty()) return b;
        if (b == null || b.isEmpty()) return a;
        List<X> res = new ObjectArrayList<>();
        res.addAll(a);
        res.addAll(b);
        return res;
    }

    public static <X> List<X> orEmptyList(List<X> nullableList) {
        return nullableList == null ? Collections.emptyList() : nullableList;
    }
}
