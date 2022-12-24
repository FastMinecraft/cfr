package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collection;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public class SetUtil {
    public static <X> boolean equals (ObjectSet<? extends X> b, Collection<? extends X> a) {
        if (a.size() != b.size()) return false;
        for (X x : a) {
            if (!b.contains(x)) return false;
        }
        return true;
    }

    public static <X> boolean hasIntersection(Set<? extends X> b, Collection<? extends X> a) {
        if (a.isEmpty() || b.isEmpty()) return false;
        for (X x : a) {
            if (b.contains(x)) return true;
        }
        return false;
    }

    // Note - this could return the original set, so don't use it if you want to mutate the set!
    public static <X> ObjectSet<X> originalIntersectionOrNull(ObjectSet<X> a, ObjectSet<? extends X> b) {
        if (a==null||b==null) return null;
        if (a.equals(b)) return a;
        return intersectionOrNull(a,b);
    }

    public static <X> ObjectSet<X> intersectionOrNull(ObjectSet<? extends X> a, ObjectSet<? extends X> b) {
        if (a==null||b==null) return null;
        if (b.size() < a.size()) {
            ObjectSet<? extends X> tmp = a;
            a = b;
            b = tmp;
        }
        ObjectSet<X> res = null;
        for (X x : a) {
            if (b.contains(x)) {
                if (res == null) res = new ObjectOpenHashSet<>();
                res.add(x);
            }
        }
        return res;
    }

    public static <X> ObjectSet<X> difference(Set<? extends X> a, Set<? extends X> b) {
        ObjectSet<X> res = new ObjectOpenHashSet<>();
        for (X a1 : a) {
            if (!b.contains(a1)) res.add(a1);
        }
        for (X b1 : b) {
            if (!a.contains(b1)) res.add(b1);
        }
        return res;
    }

    public static <X> ObjectList<X> differenceAtakeBtoList(Set<? extends X> a, Set<? extends X> b) {
        ObjectList<X> res = new ObjectArrayList<>();
        for (X a1 : a) {
            if (!b.contains(a1)) res.add(a1);
        }
        return res;
    }

    public static <X> X getSingle(Set<? extends X> a) {
        return a.iterator().next();
    }
}
