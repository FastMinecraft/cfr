package org.benf.cfr.reader.entities.annotations;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class ElementValueArray implements ElementValue {
    private final ObjectList<ElementValue> content;

    public ElementValueArray(ObjectList<ElementValue> content) {
        this.content = content;
    }

    @Override
    public Dumper dump(Dumper d) {
        d.print('{');
        boolean first = true;
        for (ElementValue value : content) {
            first = StringUtils.comma(first, d);
            value.dump(d);
        }
        d.print('}');
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ElementValue e : content) {
            e.collectTypeUsages(collector);
        }
    }

    @Override
    public ElementValue withTypeHint(JavaTypeInstance hint) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof ElementValueArray other) {
            return content.equals(other.content);
        }
        else {
            return false;
        }
    }
}
