package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.util.collections.Functional;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class UselessNops {
    public static ObjectList<Op03SimpleStatement> removeUselessNops(ObjectList<Op03SimpleStatement> in) {
        return Functional.filter(in, in1 -> !(in1.getSources().isEmpty() && in1.getTargets().isEmpty()));
    }
}
