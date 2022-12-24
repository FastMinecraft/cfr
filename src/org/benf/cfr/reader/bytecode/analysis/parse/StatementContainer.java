package org.benf.cfr.reader.bytecode.analysis.parse;

import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface StatementContainer<T> {
    T getStatement();

    T getTargetStatement(int idx);

    String getLabel();

    InstrIndex getIndex();

    void nopOut();

    void replaceStatement(T newTarget);

    void nopOutConditional();

    SSAIdentifiers<LValue> getSSAIdentifiers();

    ObjectSet<BlockIdentifier> getBlockIdentifiers();

    BlockIdentifier getBlockStarted();

    ObjectSet<BlockIdentifier> getBlocksEnded();

    void copyBlockInformationFrom(StatementContainer<T> other);

    void copyBytecodeInformationFrom(StatementContainer<T> other);
}
