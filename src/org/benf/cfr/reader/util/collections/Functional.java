package org.benf.cfr.reader.util.collections;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Functional {

    public static class NotNull<X> implements Predicate<X> {
        @Override
        public boolean test(X in) {
            return in != null;
        }
    }

    public static <X> List<X> filterOptimistic(List<X> input, Predicate<X> predicate) {
        List<X> res = null;
        for (int x=0;x<input.size();++x) {
            X item = input.get(x);
            if (!predicate.test(item)) {
                if (res == null) {
                    res = new ArrayList<>();
                    for (int y=0;y<x;++y) {
                        res.add(input.get(y));
                    }
                }
            } else {
                if (res != null) {
                    res.add(item);
                }
            }
        }
        return res == null ? input : res;
    }

    public static <X> List<X> filter(Collection<X> input, Predicate<X> predicate) {
        List<X> result = new ObjectArrayList<>();
        for (X item : input) {
            if (predicate.test(item)) result.add(item);
        }
        return result;
    }

    public static <X> X findOrNull(Collection<X> input, Predicate<X> predicate) {
        List<X> result = new ObjectArrayList<>();
        for (X item : input) {
            if (predicate.test(item)) return item;
        }
        return null;
    }

    public static <X> Set<X> filterSet(Collection<X> input, Predicate<X> predicate) {
        Set<X> result = SetFactory.newSet();
        for (X item : input) {
            if (predicate.test(item)) result.add(item);
        }
        return result;
    }

    public static <X> boolean any(Collection<X> input, Predicate<X> predicate) {
        List<X> result = new ObjectArrayList<>();
        for (X item : input) {
            if (predicate.test(item)) return true;
        }
        return false;
    }

    public static <X> boolean all(Collection<X> input, Predicate<X> predicate) {
        List<X> result = new ObjectArrayList<>();
        for (X item : input) {
            if (!predicate.test(item)) return false;
        }
        return true;
    }

    public static <X> Pair<List<X>, List<X>> partition(Collection<X> input, Predicate<X> predicate) {
        List<X> lTrue = new ObjectArrayList<>();
        List<X> lFalse = new ObjectArrayList<>();
        for (X item : input) {
            if (predicate.test(item)) {
                lTrue.add(item);
            } else {
                lFalse.add(item);
            }
        }
        return new Pair<>(lTrue, lFalse);
    }


    public static <X, Y> List<Y> map(Collection<X> input, Function<X, Y> function) {
        List<Y> result = new ObjectArrayList<>();
        for (X item : input) {
            result.add(function.apply(item));
        }
        return result;
    }

    public static <X, Y> Set<Y> mapToSet(Collection<X> input, Function<X, Y> function) {
        Set<Y> result = SetFactory.newSet();
        for (X item : input) {
            result.add(function.apply(item));
        }
        return result;
    }

    public static <X> List<X> uniqAll(List<X> input) {
        Set<X> found = SetFactory.newSet();
        List<X> result = new ObjectArrayList<>();
        for (X in : input) {
            if (found.add(in)) result.add(in);
        }
        return result;
    }

    public static <X> Map<X, Integer> indexedIdentityMapOf(Collection<X> input) {
        Map<X, Integer> temp = MapFactory.newIdentityMap();
        int idx = 0;
        for (X x : input) {
            temp.put(x, idx++);
        }
        return temp;
    }

    public static <Y, X> Map<Y, List<X>> groupToMapBy(Collection<X> input, Function<X, Y> mapF) {
        Map<Y, List<X>> temp = MapFactory.newMap();
        return groupToMapBy(input, temp, mapF);
    }

    public static <Y, X> Map<Y, List<X>> groupToMapBy(Collection<X> input, Map<Y, List<X>> tgt, Function<X, Y> mapF) {
        for (X x : input) {
            Y key = mapF.apply(x);
            List<X> lx = tgt.get(key);
            //noinspection Java8MapApi
            if (lx == null) {
                lx = new ObjectArrayList<>();
                tgt.put(key, lx);
            }
            lx.add(x);
        }
        return tgt;
    }

    public static <Y, X> List<Y> groupBy(List<X> input, Comparator<? super X> comparator, Function<List<X>, Y> gf) {
        TreeMap<X, List<X>> temp = new TreeMap<>(comparator);
        for (X x : input) {
            List<X> lx = temp.get(x);
            //noinspection Java8MapApi
            if (lx == null) {
                lx = new ObjectArrayList<>();
                temp.put(x, lx);
            }
            lx.add(x);
        }
        List<Y> res = new ObjectArrayList<>();
        for (List<X> lx : temp.values()) {
            res.add(gf.apply(lx));
        }
        return res;
    }
}
