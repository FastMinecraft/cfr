package org.benf.cfr.reader.entities;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.MapFactory;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.function.Function;

public class FakeMethods implements TypeUsageCollectable {
    private final Map<Object, FakeMethod> fakes = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<String, Integer> nameCounts = MapFactory.newLazyMap(arg -> 0);

    public FakeMethod add(Object key, String nameHint, Function<String, FakeMethod> methodFactory) {
        FakeMethod method = fakes.get(key);
        if (method == null) {
            Integer idx = nameCounts.get(nameHint);
            nameCounts.put(nameHint, idx+1);
            nameHint = "cfr_" + nameHint + "_" + idx;
            method = methodFactory.apply(nameHint);
            fakes.put(key, method);
        }
        return method;
    }

    public ObjectList<FakeMethod> getMethods() {
        return new ObjectArrayList<>(fakes.values());
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (FakeMethod method : fakes.values()) {
            collector.collectFrom(method);
        }
    }
}
