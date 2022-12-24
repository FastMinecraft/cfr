package org.benf.cfr.reader.bytecode.analysis.variables;

import it.unimi.dsi.fastutil.objects.ObjectList;

public interface VariableNamer {
    NamedVariable getName(int originalRawOffset, Ident ident, long stackPosition, boolean clashed);

    ObjectList<NamedVariable> getNamedVariables();

    void mutatingRenameUnClash(NamedVariable toRename);

    void forceName(Ident ident, long stackPosition, String name);
}
