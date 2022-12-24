package org.benf.cfr.reader.bytecode.analysis.loc;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/*
 * This implementation is not intended to be performant - revisit when functionally complete.
 */
public class BytecodeLocCollector {

    private final Map<Method, ObjectSet<Integer>> data = MapFactory.newIdentityMap();

    private ObjectSet<Integer> getForMethod(Method method) {
        ObjectSet<Integer> locs = data.get(method);
        //noinspection Java8MapApi
        if (locs == null) {
            locs = new ObjectOpenHashSet<>();
            data.put(method, locs);
        }
        return locs;
    }

    public void add(Method method, int offset) {
        getForMethod(method).add(offset);
    }

    public void add(Method method, ObjectSet<Integer> offsets) {
        getForMethod(method).addAll(offsets);
    }

    public BytecodeLoc getLoc() {
        if (data.isEmpty()) return BytecodeLoc.NONE;
        if (data.values().size() == 1) {
            ObjectSet<Integer> s = CollectionUtils.getSingle(data.values());
            if (s.size() == 1) {
                return new BytecodeLocSimple(
                        SetUtil.getSingle(s),
                        SetUtil.getSingle(data.keySet()));
            }
        }
        return new BytecodeLocSet(data);
    }
}
