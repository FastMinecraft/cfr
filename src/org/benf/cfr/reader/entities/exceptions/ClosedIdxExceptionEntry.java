package org.benf.cfr.reader.entities.exceptions;

import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;

import java.util.Map;

/**
 * Sanitised version of Exception table entry, where we use instruction idx, rather than opcode,
 * and the exceptions are CLOSED, rather than half open.
 * <p/>
 * We preprocess exceptions in terms of this where possible, as it's simpler.
 *
 * @param start     first instruction idx covered
 * @param end       last instruction idx covered
 * @param catchType have to preserve, to convert back.
 * @param priority  "
 */
public record ClosedIdxExceptionEntry(int start, int end, int handler, short catchType, int priority,
                                      JavaRefTypeInstance catchRefType) {

    public ClosedIdxExceptionEntry withRange(int newStart, int newEnd) {
        if (start == newStart && end == newEnd) return this;
        return new ClosedIdxExceptionEntry(
            newStart,
            newEnd,
            handler,
            catchType,
            priority,
            catchRefType
        );
    }

    public ExceptionTableEntry convertToRaw(Map<Integer, Integer> offsetByIdx) {
        return new ExceptionTableEntry(
            (short) (int) offsetByIdx.get(start),
            (short) (int) offsetByIdx.get(end + 1),
            (short) (int) offsetByIdx.get(handler),
            catchType,
            priority
        );

    }
}
