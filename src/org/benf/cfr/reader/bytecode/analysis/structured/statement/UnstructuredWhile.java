package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.LinkedList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Vector;

public class UnstructuredWhile extends AbstractUnStructuredStatement {
    private final ConditionalExpression condition;
    private final BlockIdentifier blockIdentifier;
    private final ObjectSet<BlockIdentifier> blocksEndedAfter;

    public UnstructuredWhile(BytecodeLoc loc, ConditionalExpression condition, BlockIdentifier blockIdentifier, ObjectSet<BlockIdentifier> blocksEndedAfter) {
        super(loc);
        this.condition = condition;
        this.blockIdentifier = blockIdentifier;
        // We have to be careful here - if this while statement jumps out PAST an outer block when it
        // ends, we have to ADD a break to the correct block.
        this.blocksEndedAfter = blocksEndedAfter;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("** while (");
        if (condition == null) {
            dumper.print("true");
        } else {
            dumper.dump(condition);
        }
        return dumper.separator(")").newln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, condition);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(condition);
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        switch (blockIdentifier.getBlockType()) {
            case DOLOOP:
            case UNCONDITIONALDOLOOP:
                break;
            default:
                return null;
        }
        if (blockIdentifiers.isEmpty()) return null;
        if (blockIdentifier != blockIdentifiers.get(blockIdentifiers.size() - 1)) {
            // We think we're ending a block, but we're inside something else?  This must have been a backjump
            // on an unterminated loop.  We need to convert this into a continue statement.
            // (and the loop will get turned into a  ..... break; }  while (true) when it is finished. )
            StructuredStatement res = new UnstructuredContinue(BytecodeLoc.TODO, blockIdentifier);
            StructuredStatement resInform = res.informBlockHeirachy(blockIdentifiers);
            if (resInform != null) res = resInform;

            if (condition == null) return res;
            // Else we need to make up an if block as well!!
            return new StructuredIf(BytecodeLoc.TODO, condition, new Op04StructuredStatement(res));
        }
        return null;
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("While statement claiming wrong block");
        }
        innerBlock.removeLastContinue(blockIdentifier);
        /* If blocksEndedAfter includes blocks which are in 'blocksCurrentlyIn', then we're
         * breaking out of an outer block after this loop!
         */
        StructuredStatement whileLoop = new StructuredWhile(condition, innerBlock, blockIdentifier);

        BlockIdentifier externalBreak = BlockIdentifier.getOutermostEnding(new ObjectArrayList<>(blocksCurrentlyIn), blocksEndedAfter);
        if (externalBreak == null) {
            return whileLoop;
        }

        /* We have subsumed a break to an outer loop. :P */
        LinkedList<Op04StructuredStatement> lst = new LinkedList<>();
        lst.add(new Op04StructuredStatement(whileLoop));
        lst.add(new Op04StructuredStatement(new StructuredBreak(BytecodeLoc.TODO, externalBreak, false)));
        return new Block(
                lst, false
        );
    }

    public ConditionalExpression getCondition() {
        return condition;
    }
}
