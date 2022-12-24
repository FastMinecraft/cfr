package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.util.collections.Functional;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Set;

public class BlockIdentifier implements Comparable<BlockIdentifier> {
    private final int index;
    private BlockType blockType;
    // foreign refs - for spotting non local jumps to this block.
    private int knownForeignReferences = 0;

    public BlockIdentifier(int index, BlockType blockType) {
        this.index = index;
        this.blockType = blockType;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }

    public String getName() {
        return "block" + index;
    }

    public int getIndex() {
        return index;
    }

    public void addForeignRef() {
        knownForeignReferences++;
    }

    public void releaseForeignRef() {
        knownForeignReferences--;
    }

    public boolean hasForeignReferences() {
        return knownForeignReferences > 0;
    }

    @Override
    public String toString() {
        return "" + index + "[" + blockType + "]";
    }

    public static boolean blockIsOneOf(BlockIdentifier needle, Set<BlockIdentifier> haystack) {
        return haystack.contains(needle);
    }

    public static BlockIdentifier getOutermostContainedIn(Set<BlockIdentifier> endingBlocks, final Set<BlockIdentifier> blocksInAtThisPoint) {
        ObjectList<BlockIdentifier> containedIn = Functional.filter(
            new ObjectArrayList<BlockIdentifier>(endingBlocks),
            blocksInAtThisPoint::contains
        );
        if (containedIn.isEmpty()) return null;
        Collections.sort(containedIn);
        return containedIn.get(0);
    }

    /* Given a scope heirachy, which is the innermost one which can be broken out of? */
    public static BlockIdentifier getInnermostBreakable(ObjectList<BlockIdentifier> blocks) {
        BlockIdentifier res = null;
        for (BlockIdentifier block : blocks) {
            if (block.blockType.isBreakable()) res = block;
        }
        return res;
    }

    /* Given a scope heirachy, and a list of blocks which are ending, which is the outermost block which is ending?
     * i.e. we want the earliest block in blocks which is also in blocksEnding.
     */
    public static BlockIdentifier getOutermostEnding(ObjectList<BlockIdentifier> blocks, Set<BlockIdentifier> blocksEnding) {
        for (BlockIdentifier blockIdentifier : blocks) {
            if (blocksEnding.contains(blockIdentifier)) return blockIdentifier;
        }
        return null;
    }

    @Override
    public int compareTo(BlockIdentifier blockIdentifier) {
        return index - blockIdentifier.index;
    }
}
