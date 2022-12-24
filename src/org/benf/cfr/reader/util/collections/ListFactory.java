package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collection;
import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.Set;

public class ListFactory {
    public static <X> ObjectList<X> uniqueList(Collection<X> list) {
        return new ObjectArrayList<>(SetFactory.newOrderedSet(list));
    }

    /** Note that you can't expect to mutate the result. */
    public static <X> ObjectList<X> combinedOptimistic(ObjectList<X> a, ObjectList<X> b) {
        if (a == null || a.isEmpty()) return b;
        if (b == null || b.isEmpty()) return a;
        ObjectList<X> res = new ObjectArrayList<>();
        res.addAll(a);
        res.addAll(b);
        return res;
    }

    public static <X> ObjectList<X> orEmptyList(ObjectList<X> nullableList) {
        return nullableList == null ? ObjectLists.emptyList() : nullableList;
    }
}
