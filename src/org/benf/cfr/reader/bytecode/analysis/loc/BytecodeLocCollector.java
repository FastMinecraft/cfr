package org.benf.cfr.reader.bytecode.analysis.loc;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

import java.util.Map;
import java.util.Set;

/*
 * This implementation is not intended to be performant - revisit when functionally complete.
 */
public class BytecodeLocCollector {

    private final Map<Method, Set<Integer>> data = MapFactory.newIdentityMap();

    private Set<Integer> getForMethod(Method method) {
        Set<Integer> locs = data.get(method);
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

    public void add(Method method, Set<Integer> offsets) {
        getForMethod(method).addAll(offsets);
    }

    public BytecodeLoc getLoc() {
        if (data.isEmpty()) return BytecodeLoc.NONE;
        if (data.values().size() == 1) {
            Set<Integer> s = CollectionUtils.getSingle(data.values());
            if (s.size() == 1) {
                return new BytecodeLocSimple(
                        SetUtil.getSingle(s),
                        SetUtil.getSingle(data.keySet()));
            }
        }
        return new BytecodeLocSet(data);
    }
}
