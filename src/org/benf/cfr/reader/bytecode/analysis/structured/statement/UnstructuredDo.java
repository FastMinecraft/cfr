package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.LinkedList;
import java.util.Vector;

public class UnstructuredDo extends AbstractUnStructuredStatement {
    private final BlockIdentifier blockIdentifier;

    public UnstructuredDo(BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** do ").newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("Do statement claiming wrong block");
        }
        UnstructuredWhile lastEndWhile = innerBlock.removeLastEndWhile();
        if (lastEndWhile != null) {
            ConditionalExpression condition = lastEndWhile.getCondition();
            return StructuredDo.create(condition, innerBlock, blockIdentifier);
        }

        /*
         * If there were any ways of legitimately hitting the exit, we need a break.  If not, we don't.
         * do always points to while so it's not orphaned, so we're checking for > 1 parent.
         *
         * need to transform
         * do {
         * } ???
         *    ->
         * do {
         *  ...
         *  break;
         * } while (true);
         */
        /*
         * But - if the inner statement is simply a single statement, and not a break FROM this block,
         * (or a continue of it), we can just drop the loop completely.
         */

        StructuredStatement inner = innerBlock.getStatement();
        if (!(inner instanceof Block)) {
            LinkedList<Op04StructuredStatement> blockContent = new LinkedList<>();
            blockContent.add(new Op04StructuredStatement(inner));
            inner = new Block(blockContent, true);
            innerBlock.replaceStatement(inner);
        }
        Block block = (Block) inner;
        Optional<Op04StructuredStatement> maybeStatement = block.getMaybeJustOneStatement();
        if (maybeStatement.isSet()) {
            Op04StructuredStatement singleStatement = maybeStatement.getValue();
            StructuredStatement stm = singleStatement.getStatement();
            boolean canRemove = true;
            if (stm instanceof StructuredBreak brk) {
                if (brk.getBreakBlock().equals(blockIdentifier)) canRemove = false;
            } else if (stm instanceof StructuredContinue cnt) {
                if (cnt.getContinueTgt().equals(blockIdentifier)) canRemove = false;
            } else if (stm.canFall()) {
                canRemove = false;
            }
            if (canRemove) {
                return stm;
            }
        }
        Op04StructuredStatement last = block.getLast();
        if (last != null) {
            if (last.getStatement().canFall()) {
                block.addStatement(new Op04StructuredStatement(new StructuredBreak(getLoc(), blockIdentifier, true)));
            }
        }
        return StructuredDo.create(null, innerBlock, blockIdentifier);
    }
}
