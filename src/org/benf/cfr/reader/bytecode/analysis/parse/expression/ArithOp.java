package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.ConfusedCFRException;

import it.unimi.dsi.fastutil.objects.ObjectSet;

public enum ArithOp {
    LCMP("LCMP", true, false, Precedence.WEAKEST),
    DCMPL("DCMPL", true, false, Precedence.WEAKEST),
    DCMPG("DCMPG", true, false, Precedence.WEAKEST),
    FCMPL("FCMPL", true, false, Precedence.WEAKEST),
    FCMPG("FCMPG", true, false, Precedence.WEAKEST),
    PLUS("+", false, false, Precedence.ADD_SUB),
    MINUS("-", false, false,Precedence.ADD_SUB),
    MULTIPLY("*", false, false, Precedence.MUL_DIV_MOD),
    DIVIDE("/", false, false, Precedence.MUL_DIV_MOD),
    REM("%", false,false, Precedence.MUL_DIV_MOD),
    OR("|", false, true, Precedence.BIT_OR),
    AND("&", false, true, Precedence.BIT_AND),
    SHR(">>", false, false, Precedence.BITWISE_SHIFT),
    SHL("<<", false, false, Precedence.BITWISE_SHIFT),
    SHRU(">>>", false, false, Precedence.BITWISE_SHIFT),
    XOR("^", false,true, Precedence.BIT_XOR),
    NEG("~", false, false, Precedence.UNARY_OTHER);

    private final String showAs;
    private final boolean temporary;
    private final boolean boolSafe;
    private final Precedence precedence;

    ArithOp(String showAs, boolean temporary, boolean boolSafe, Precedence precedence) {
        this.showAs = showAs;
        this.temporary = temporary;
        this.boolSafe = boolSafe;
        this.precedence = precedence;
    }

    public String getShowAs() {
        return showAs;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public Precedence getPrecedence() {
        return precedence;
    }

    public static ArithOp getOpFor(JVMInstr instr) {
        return switch (instr) {
            case LCMP -> LCMP;
            case DCMPG -> DCMPG;
            case DCMPL -> DCMPL;
            case FCMPG -> FCMPG;
            case FCMPL -> FCMPL;
            case ISUB, LSUB, FSUB, DSUB -> MINUS;
            case IMUL, LMUL, FMUL, DMUL -> MULTIPLY;
            case IADD, LADD, FADD, DADD -> PLUS;
            case LDIV, IDIV, FDIV, DDIV -> DIVIDE;
            case LOR, IOR -> OR;
            case LAND, IAND -> AND;
            case IREM, LREM, FREM, DREM -> REM;
            case ISHR, LSHR -> SHR;
            case IUSHR, LUSHR -> SHRU;
            case ISHL, LSHL -> SHL;
            case IXOR, LXOR -> XOR;
            default -> throw new ConfusedCFRException("Don't know arith op for " + instr);
        };
    }

    public boolean canThrow(InferredJavaType inferredJavaType, ExceptionCheck caught, ObjectSet<? extends JavaTypeInstance> instances) {
        StackType stackType = inferredJavaType.getRawType().getStackType();
        switch (stackType) {
            case DOUBLE, FLOAT, INT, LONG -> {
                if (this != DIVIDE) return false;
            }
        }
        return caught.checkAgainst(instances);
    }

    public boolean isBoolSafe() {
        return boolSafe;
    }
}
