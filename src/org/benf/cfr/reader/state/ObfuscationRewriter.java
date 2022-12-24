package org.benf.cfr.reader.state;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public interface ObfuscationRewriter {
    Dumper wrap(Dumper d);

    JavaTypeInstance get(JavaTypeInstance t);

    ObjectList<JavaTypeInstance> get(ObjectList<JavaTypeInstance> types);
}
