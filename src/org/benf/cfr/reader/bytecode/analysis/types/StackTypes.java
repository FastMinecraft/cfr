package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.ArrayList;
import java.util.Arrays;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Really ObjectList<StackType> but for legibility, shortened.
 */
public class StackTypes extends ArrayList<StackType> {
    public static final StackTypes EMPTY = new StackTypes();

    public StackTypes(StackType... stackTypes) {
        super(Arrays.asList(stackTypes));
    }

    public StackTypes(ObjectList<StackType> stackTypes) {
        super(stackTypes);
    }
}
