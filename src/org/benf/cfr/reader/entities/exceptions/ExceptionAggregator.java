package org.benf.cfr.reader.entities.exceptions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.*;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import java.util.function.Predicate;

import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.*;

public class ExceptionAggregator {

    private final ObjectList<ExceptionGroup> exceptionsByRange = new ObjectArrayList<>();
    private final Map<Integer, Integer> lutByOffset;
    private final ObjectList<Op01WithProcessedDataAndByteJumps> instrs;
    private final boolean aggressiveAggregate;
    private final boolean aggressiveAggregate2;
    private final boolean removedLoopingExceptions;


    private static class CompareExceptionTablesByRange implements Comparator<ExceptionTableEntry> {
        @Override
        public int compare(ExceptionTableEntry exceptionTableEntry, ExceptionTableEntry exceptionTableEntry1) {
            int res = exceptionTableEntry.getBytecodeIndexFrom() - exceptionTableEntry1.getBytecodeIndexFrom();
            if (res != 0) return res;
            return exceptionTableEntry.getBytecodeIndexTo() - exceptionTableEntry1.getBytecodeIndexTo();
        }
    }

    private class ByTarget {
        private final ObjectList<ExceptionTableEntry> entries;

        ByTarget(ObjectList<ExceptionTableEntry> entries) {
            this.entries = entries;
        }

        Collection<ExceptionTableEntry> getAggregated(DecompilerComments comments) {
            this.entries.sort(new CompareExceptionTablesByRange());
            /* If two entries are contiguous, they can be merged 
             * If they're 'almost' contiguous, but point to the same range? ........ don't know.
             */
            ObjectList<ExceptionTableEntry> res = new ObjectArrayList<>();
            ExceptionTableEntry held = null;
            for (ExceptionTableEntry entry : this.entries) {
                if (held == null) {
                    held = entry;
                } else {
                    // TODO - shouldn't be using bytecode indices unless we can account for instruction length?
                    // TODO - depends if the end is the start of the last opcode, or the end.
                    if (held.getBytecodeIndexTo() == entry.getBytecodeIndexFrom()) {
                        held = held.aggregateWith(entry);
                    } else if (held.getBytecodeIndexFrom() == entry.getBytecodeIndexFrom() &&
                            held.getBytecodeIndexTo() <= entry.getBytecodeIndexTo()) {
                        held = entry;
                    } else if (held.getBytecodeIndexFrom() < entry.getBytecodeIndexFrom() &&
                            entry.getBytecodeIndexFrom() < held.getBytecodeIndexTo() &&
                            entry.getBytecodeIndexTo() > held.getBytecodeIndexTo()) {
                        held = held.aggregateWithLenient(entry);
                    } else if (aggressiveAggregate && canExtendTo(held, entry, comments)) {
                        held = held.aggregateWithLenient(entry);
                    } else {
                        res.add(held);
                        held = entry;
                    }
                }
            }
            if (held != null) res.add(held);
            return res;
        }

    }

    private static class ValidException implements Predicate<ExceptionTableEntry> {
        @Override
        public boolean test(ExceptionTableEntry in) {
            return (in.getBytecodeIndexFrom() != in.getBytecodeIndexHandler());
        }
    }

    /*
     * We have a range a[1,4] b[6, 7].
     *
     * If a can be extended to cover 1-6, then a & b can be combined.
     * (remember, exception ranges are half open, 1-4 covers 1 UNTIL 4, not incl).
     *
     * Some instructions can be guaranteed not to throw, they can be extended over.
     */
    private boolean canExtendTo(ExceptionTableEntry a, ExceptionTableEntry b, DecompilerComments comments) {
        final int startNext = b.getBytecodeIndexFrom();
        int current = a.getBytecodeIndexTo();
        if (current > startNext) return false;
        boolean veryAggressive = false;

        while (current < startNext) {
            Integer idx = lutByOffset.get(current);
            if (idx == null) return false;
            Op01WithProcessedDataAndByteJumps op = instrs.get(idx);
            JVMInstr instr = op.getJVMInstr();
            if (instr.isNoThrow()) {
                current += op.getInstructionLength();
            } else if (aggressiveAggregate) {
                switch (instr) {
                    case IASTORE:
                    case IALOAD:
                    case DASTORE:
                    case DALOAD:
                    case FASTORE:
                    case FALOAD:
                    case AASTORE:
                    case AALOAD:
                        /*
                         * These are doubly interesting - they could NPE, or array index out of bounds.
                         * However, I've seen multiple examples where exceptions are split with this inbetween.
                         * There's no reasonably way that this code would have been written like that, so it implies
                         * that either an obfuscator has been at it, or a compiler has proved they won't fail, and
                         * split the exception up.  (WHY, though?)
                         */
                        if (!this.aggressiveAggregate2) {
                            return false;
                        }
                        veryAggressive = true;
                    case GETSTATIC:
                        /* Getstatic CAN throw, but some code will separate exception blocks around it, just to be
                         * awkward.  This is likely enough (and reasonably innocent) that we don't consider it to be a
                         * "very" dangerous aggregation.
                         */
                        current += op.getInstructionLength();
                        break;
                    default:
                        return false;
                }
            } else {
                return false;
            }
        }
        if (veryAggressive) {
            comments.addComment(DecompilerComment.AGGRESSIVE_EXCEPTION_VERY_AGG);
        }
        return true;
    }


    // Note - we deliberately don't use instr.isNoThrow here, that leads to over eager expansion into exception handlers!
    private static int canExpandTryBy(int idx, ObjectList<Op01WithProcessedDataAndByteJumps> statements) {
        Op01WithProcessedDataAndByteJumps op = statements.get(idx);
        JVMInstr instr = op.getJVMInstr();
        switch (instr) {
            case GOTO, GOTO_W, RETURN, ARETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                return op.getInstructionLength();
            }
            case ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3 -> {
                Op01WithProcessedDataAndByteJumps op2 = statements.get(idx + 1);
                if (op2.getJVMInstr() == JVMInstr.MONITOREXIT)
                    return op.getInstructionLength() + op2.getInstructionLength();
            }
        }
        return 0;
    }

    /* Raw exceptions are just start -> last+1 lists.  There's no (REF?) requirement that they be non overlapping, so
    * I guess a compiler could have a,b a2, b2 where a < a2, b > a2 < b2... (eww).
    * In that case, we should split the exception regime into non-overlapping sections.
    */
    public ExceptionAggregator(ObjectList<ExceptionTableEntry> rawExceptions, BlockIdentifierFactory blockIdentifierFactory,
                               final Map<Integer, Integer> lutByOffset,
                               ObjectList<Op01WithProcessedDataAndByteJumps> instrs,
                               final Options options,
                               final ConstantPool cp,
                               DecompilerComments comments) {

        this.lutByOffset = lutByOffset;
        this.instrs = instrs;
        this.aggressiveAggregate = options.getOption(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG) == Troolean.TRUE;
        this.aggressiveAggregate2 = options.getOption(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2) == Troolean.TRUE;

        ObjectList<ExceptionTableEntry> tmpExceptions = Functional.filter(rawExceptions, new ValidException());
        boolean removedLoopingExceptions = false;
        if (tmpExceptions.size() != rawExceptions.size()) {
            rawExceptions = tmpExceptions;
            removedLoopingExceptions = true;
        }
        this.removedLoopingExceptions = removedLoopingExceptions;
        if (rawExceptions.isEmpty()) return;

        // Todo : Use ConvExceptions (based on op index), not RawExceptions (based on bytecode index).
        /*
         * Extend an exception which terminates at a return.
         * Remember exception tables are half closed [0,1) == just covers 0.
         */
        ObjectList<ExceptionTableEntry> extended = new ObjectArrayList<>();
        for (ExceptionTableEntry exceptionTableEntry : rawExceptions) {

            ExceptionTableEntry exceptionTableEntryOrig;

            int indexTo = exceptionTableEntry.getBytecodeIndexTo();

            do {
                exceptionTableEntryOrig = exceptionTableEntry;
                Integer tgtIdx = lutByOffset.get(indexTo);
                if (tgtIdx != null) {

                    // See if the last statement is a direct return, which could be pushed in.  If so, expand try block.
                    int offset = canExpandTryBy(tgtIdx, instrs);
                    if (offset != 0) {
                        int bytecodeIndexFrom = exceptionTableEntry.getBytecodeIndexFrom();
                        int bytecodeIndexTo = (exceptionTableEntry.getBytecodeIndexTo() + offset);
                        exceptionTableEntry = exceptionTableEntry.copyWithRange(bytecodeIndexFrom,
                                bytecodeIndexTo);
                    }
                    indexTo += offset;
                }

            } while (exceptionTableEntry != exceptionTableEntryOrig);

            /*
             * But now, we shrink it to make sure that it doesn't overlap the catch block.
             * This will break some of the nastier exception obfuscations I can think of :(
             */
            int handlerIndex = exceptionTableEntry.getBytecodeIndexHandler();
            indexTo = exceptionTableEntry.getBytecodeIndexTo();
            int indexFrom = exceptionTableEntry.getBytecodeIndexFrom();
            if (indexFrom < handlerIndex &&
                    indexTo >= handlerIndex) {
                exceptionTableEntry = exceptionTableEntry.copyWithRange(indexFrom, handlerIndex);
            }
            extended.add(exceptionTableEntry);
        }
        rawExceptions = extended;

        /*
         * If an exception table entry for a type X OVERLAPS an entry for a type X, but has a lower priority, then
         * it is truncated.  This probably indicates obfuscation.
         */

        // Need to build up an interval tree for EACH exception handler type
        Map<Integer, ObjectList<ExceptionTableEntry>> grouped = Functional.groupToMapBy(rawExceptions,
            ExceptionTableEntry::getCatchType
        );

        ObjectList<ExceptionTableEntry> processedExceptions = new ObjectArrayList<>(rawExceptions.size());
        for (ObjectList<ExceptionTableEntry> list : grouped.values()) {
            IntervalCount intervalCount = new IntervalCount();
            for (ExceptionTableEntry e : list) {
                int from = e.getBytecodeIndexFrom();
                int to = e.getBytecodeIndexTo();
                Pair<Integer, Integer> res = intervalCount.generateNonIntersection(from, to);
                if (res == null) continue;
                processedExceptions.add(new ExceptionTableEntry(res.getFirst(), res.getSecond(), e.getBytecodeIndexHandler(), e.getCatchType(), e.getPriority()));
            }
        }

        /* 
         * Try and aggregate exceptions for the same object which jump to the same target.
         */
        Collection<ByTarget> byTargetList = Functional.groupBy(processedExceptions,
            Comparator.comparingInt(ExceptionTableEntry::getBytecodeIndexHandler).thenComparingInt(ExceptionTableEntry::getCatchType),
            ByTarget::new
        );

        rawExceptions = new ObjectArrayList<>();

        /*
         * If there are two exceptions which both vector to the same target,
         *
         * A,B  [e1] ->   X
         * G,H  [e1] ->   X
         *
         * but there is another exception in the range of the EARLIER one which
         * vectors to the later one
         *
         * A,B  [e2] ->   G
         *
         * Then we make the (probably dodgy) assumption that the first exceptions
         * actually are one range.
         *
         */
        Map<Integer, ByTarget> byTargetMap = MapFactory.newMap();
        for (ByTarget t : byTargetList) {
            byTargetMap.put(t.entries.get(0).getBytecodeIndexHandler(), t);
        }

        /*
         * Each of these is now lists which point to the same handler+type.
         */
        for (ByTarget byTarget : byTargetList) {
            rawExceptions.addAll(byTarget.getAggregated(comments));
        }

        /*
         * But if two different exceptions actually overlap, then we've either got obfuscation or hand coded?
         * (or some interesting transformation).
         */
        IntervalOverlapper intervalOverlapper = new IntervalOverlapper(rawExceptions);
        rawExceptions = intervalOverlapper.getExceptions();

        Collections.sort(rawExceptions);

        CompareExceptionTablesByRange compareExceptionTablesByStart = new CompareExceptionTablesByRange();
        ExceptionTableEntry prev = null;
        ExceptionGroup currentGroup = null;
        ObjectList<ExceptionGroup> rawExceptionsByRange = new ObjectArrayList<>();
        for (ExceptionTableEntry e : rawExceptions) {
            if (prev == null || compareExceptionTablesByStart.compare(e, prev) != 0) {
                currentGroup = new ExceptionGroup(e.getBytecodeIndexFrom(), blockIdentifierFactory.getNextBlockIdentifier(BlockType.TRYBLOCK), cp);
                rawExceptionsByRange.add(currentGroup);
                prev = e;
            }
            currentGroup.add(e);
        }
        exceptionsByRange.addAll(rawExceptionsByRange);
    }

    public ObjectList<ExceptionGroup> getExceptionsGroups() {
        return exceptionsByRange;
    }

    /*
     * Remove try statements which simply jump to monitorexit+ , throw statements.
     */
    public void removeSynchronisedHandlers(final Map<Integer, Integer> lutByIdx) {
        Iterator<ExceptionGroup> groupIterator = exceptionsByRange.iterator();
        while (groupIterator.hasNext()) {
            ExceptionGroup group = groupIterator.next();
            group.removeSynchronisedHandlers(lutByOffset, lutByIdx, instrs);
            if (group.getEntries().isEmpty()) {
                groupIterator.remove();
            }
        }
    }

    /*
     * Remove any exception handlers which can't possibly do anything useful.
     *
     * i.e.
     *
     * try {
     *   x
     * } catch (e) {
     *   throw e;
     * }
     *
     * We have to be very careful here, as it's not valid to do this if the exception handler
     * is one of multiple exception handlers for the same block - i.e.
     *
     * try {
     *  x
     * } catch (e) {
     *  throw e
     * } catch (f) {
     *  // do stuff
     * }
     *
     * for any given catch-rethrow block, we can remove it IF the range covered by its try handler is not covered
     * by any other try handler.
     *
     * We should then re-cover the try block with the coverage which is applied to the exception handler (if any).
     */
    public void aggressiveRethrowPruning() {
        Iterator<ExceptionGroup> groupIterator = exceptionsByRange.iterator();
        while (groupIterator.hasNext()) {
            ExceptionGroup group = groupIterator.next();
            ObjectList<ExceptionGroup.Entry> entries = group.getEntries();
            if (entries.size() != 1) continue;
            ExceptionGroup.Entry entry = entries.get(0);
            int handler = entry.getBytecodeIndexHandler();
            Integer index = lutByOffset.get(handler);
            if (index == null) continue;
            Op01WithProcessedDataAndByteJumps handlerStartInstr = instrs.get(index);
            if (handlerStartInstr.getJVMInstr() == JVMInstr.ATHROW) {
                groupIterator.remove();
            }
        }
    }

    /*
     * We can also remove exception handlers that can't possibly be called.
     * Need to be extremely conservative about this.
     *
     * We do this very early (at Op01 stage) as doing it later would require undoing work,
     * and allows us to leave exception handlers unlinked.
     */
    public void aggressiveImpossiblePruning() {
        TreeMap<Integer, Op01WithProcessedDataAndByteJumps> sortedNodes = MapFactory.newTreeMap();
        for (Op01WithProcessedDataAndByteJumps instr : instrs) {
            sortedNodes.put(instr.getOriginalRawOffset(), instr);
        }
        Iterator<ExceptionGroup> groupIterator = exceptionsByRange.iterator();
        nextGroup : while (groupIterator.hasNext()) {
            ExceptionGroup group = groupIterator.next();
            int from = group.getBytecodeIndexFrom();
            int to = group.getBytecodeIndexTo();
            Integer fromKey = sortedNodes.floorKey(from);
            Collection<Op01WithProcessedDataAndByteJumps> content = sortedNodes.tailMap(fromKey, true).values();
            for (Op01WithProcessedDataAndByteJumps item : content) {
                if (item.getOriginalRawOffset() >= to) {
                    break;
                }
                if (!item.getJVMInstr().isNoThrow()) {
                    continue nextGroup;
                }
            }
            groupIterator.remove();
        }
    }

    public boolean RemovedLoopingExceptions() {
        return removedLoopingExceptions;
    }
}
