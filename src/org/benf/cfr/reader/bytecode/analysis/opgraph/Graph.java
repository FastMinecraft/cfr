package org.benf.cfr.reader.bytecode.analysis.opgraph;

import it.unimi.dsi.fastutil.objects.ObjectList;

public interface Graph<T> {
    ObjectList<T> getSources();
    ObjectList<T> getTargets();
}
