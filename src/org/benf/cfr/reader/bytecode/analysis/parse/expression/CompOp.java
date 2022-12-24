package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.util.ConfusedCFRException;

public enum CompOp {
    LT("<", Precedence.REL_CMP_INSTANCEOF),
    GT(">", Precedence.REL_CMP_INSTANCEOF),
    LTE("<=", Precedence.REL_CMP_INSTANCEOF),
    GTE(">=", Precedence.REL_CMP_INSTANCEOF),
    EQ("==", Precedence.REL_EQ),
    NE("!=", Precedence.REL_EQ);


    private final String showAs;
    private final Precedence precedence;

    CompOp(String showAs, Precedence precedence) {
        this.showAs = showAs;
        this.precedence = precedence;
    }

    public String getShowAs() {
        return showAs;
    }

    public Precedence getPrecedence() {
        return precedence;
    }

    public CompOp getInverted() {
        return switch (this) {
            case LT -> GTE;
            case GT -> LTE;
            case GTE -> LT;
            case LTE -> GT;
            case EQ -> NE;
            case NE -> EQ;
        };
    }


    public static CompOp getOpFor(JVMInstr instr) {
        return switch (instr) {
            case IF_ICMPEQ, IF_ACMPEQ, IFEQ -> EQ;
            case IF_ICMPLT, IFLT -> LT;
            case IF_ICMPGE, IFGE -> GTE;
            case IF_ICMPGT, IFGT -> GT;
            case IF_ICMPNE, IF_ACMPNE, IFNE -> NE;
            case IF_ICMPLE, IFLE -> LTE;
            default -> throw new ConfusedCFRException("Don't know comparison op for " + instr);
        };
    }
}
