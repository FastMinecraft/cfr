package org.benf.cfr.reader.bytecode.analysis.opgraph.op02obf;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.exceptions.ExceptionAggregator;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.SortedMap;

public class Op02Obf {
    public static void removeControlFlowExceptions(Method method, ExceptionAggregator exceptions, ObjectList<Op02WithProcessedDataAndRefs> op2list, SortedMap<Integer, Integer> lutByOffset) {
        ControlFlowIntDiv0Exception.Instance.process(method, exceptions, op2list, lutByOffset);
        ControlFlowNullException.Instance.process(method, exceptions, op2list, lutByOffset);
    }

    public static void removeNumericObf(Method method, ObjectList<Op02WithProcessedDataAndRefs> op2list) {
        ControlFlowNumericObf.Instance.process(method, op2list);
    }

    public static boolean detectObfuscations(Method method, ExceptionAggregator exceptions, ObjectList<Op02WithProcessedDataAndRefs> op2list, SortedMap<Integer, Integer> lutByOffset) {
        if (ControlFlowIntDiv0Exception.Instance.check(exceptions, op2list, lutByOffset)) return true;
        return ControlFlowNullException.Instance.check(exceptions, op2list, lutByOffset);
    }
}
