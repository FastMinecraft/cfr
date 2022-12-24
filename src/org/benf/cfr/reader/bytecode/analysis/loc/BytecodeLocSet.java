package org.benf.cfr.reader.bytecode.analysis.loc;

import org.benf.cfr.reader.entities.Method;

import java.util.Collection;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class BytecodeLocSet extends BytecodeLoc {
    private final Map<Method, ObjectSet<Integer>> locs;

    BytecodeLocSet(Map<Method, ObjectSet<Integer>> locs) {
        this.locs = locs;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
        for (Map.Entry<Method, ObjectSet<Integer>> entry : locs.entrySet()) {
            collector.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Method, ObjectSet<Integer>> entry : locs.entrySet()) {
            sb.append(entry.getKey().getName()).append("[");
            for (Integer i : entry.getValue()) {
                sb.append(i).append(",");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public Collection<Method> getMethods() {
        return locs.keySet();
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return locs.get(method);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
