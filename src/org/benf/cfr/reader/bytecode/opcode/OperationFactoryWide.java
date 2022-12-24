package org.benf.cfr.reader.bytecode.opcode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.ByteData;

public class OperationFactoryWide extends OperationFactoryDefault {

    private static JVMInstr getWideInstrVersion(JVMInstr instr) {
        return switch (instr) {
            case IINC -> JVMInstr.IINC_WIDE;
            case ILOAD -> JVMInstr.ILOAD_WIDE;
            case FLOAD -> JVMInstr.FLOAD_WIDE;
            case ALOAD -> JVMInstr.ALOAD_WIDE;
            case LLOAD -> JVMInstr.LLOAD_WIDE;
            case DLOAD -> JVMInstr.DLOAD_WIDE;
            case ISTORE -> JVMInstr.ISTORE_WIDE;
            case FSTORE -> JVMInstr.FSTORE_WIDE;
            case ASTORE -> JVMInstr.ASTORE_WIDE;
            case LSTORE -> JVMInstr.LSTORE_WIDE;
            case DSTORE -> JVMInstr.DSTORE_WIDE;
            case RET -> JVMInstr.RET_WIDE;
            default -> throw new ConfusedCFRException("Wide is not defined for instr " + instr);
        };
    }

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        JVMInstr widenedInstr = getWideInstrVersion(JVMInstr.find(bd.getS1At(1)));
        return widenedInstr.createOperation(bd, cp, offset);
    }
}
