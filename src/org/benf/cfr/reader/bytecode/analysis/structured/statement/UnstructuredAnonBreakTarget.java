package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class UnstructuredAnonBreakTarget extends AbstractUnStructuredStatement {
    private final BlockIdentifier blockIdentifier;

    public UnstructuredAnonBreakTarget(BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper;
    }

    // Lose the comments.
    @Override
    public void linearizeInto(ObjectList<StructuredStatement> out) {
    }

    public BlockIdentifier getBlockIdentifier() {
        return blockIdentifier;
    }

    @Override
    public boolean isEffectivelyNOP() {
        return true;
    }
}
