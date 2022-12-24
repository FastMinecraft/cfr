package org.benf.cfr.reader.entities.exceptions;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface ExceptionCheck {
    boolean checkAgainst(ObjectSet<? extends JavaTypeInstance> thrown);

    // Might this throw in a way which means it can't be moved into the exception block?
    boolean checkAgainst(AbstractMemberFunctionInvokation functionInvokation);

    boolean checkAgainstException(Expression expression);

    boolean mightCatchUnchecked();
}
