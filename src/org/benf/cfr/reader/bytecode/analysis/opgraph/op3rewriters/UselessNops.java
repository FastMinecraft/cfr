package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.util.collections.Functional;
import java.util.function.Predicate;

import java.util.List;

public class UselessNops {
    public static List<Op03SimpleStatement> removeUselessNops(List<Op03SimpleStatement> in) {
        return Functional.filter(in, in1 -> !(in1.getSources().isEmpty() && in1.getTargets().isEmpty()));
    }
}
