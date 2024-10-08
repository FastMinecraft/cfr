package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public interface ConstantPoolEntry {

    long getRawByteLength();

    void dump(Dumper d);

    enum Type {
        CPT_UTF8,
        CPT_Integer,
        CPT_Float,
        CPT_Long,
        CPT_Double,
        CPT_Class,
        CPT_String,
        CPT_FieldRef,
        CPT_MethodRef,
        CPT_InterfaceMethodRef,
        CPT_NameAndType,
        CPT_MethodHandle,
        CPT_MethodType,
        CPT_DynamicInfo,
        CPT_InvokeDynamic,
        CPT_ModuleInfo,
        CPT_PackageInfo;

        private static final byte VAL_UTF8 = 1;
        private static final byte VAL_Integer = 3;
        private static final byte VAL_Float = 4;
        private static final byte VAL_Long = 5;
        private static final byte VAL_Double = 6;
        private static final byte VAL_Class = 7;
        private static final byte VAL_String = 8;
        private static final byte VAL_FieldRef = 9;
        private static final byte VAL_MethodRef = 10;
        private static final byte VAL_InterfaceMethodRef = 11;
        private static final byte VAL_NameAndType = 12;
        private static final byte VAL_MethodHandle = 15;
        private static final byte VAL_MethodType = 16;
        private static final byte VAL_DynamicInfo = 17;
        private static final byte VAL_InvokeDynamic = 18;
        private static final byte VAL_ModuleInfo = 19;
        private static final byte VAL_PackageInfo = 20;

        public static Type get(byte val) {
            return switch (val) {
                case VAL_UTF8 -> CPT_UTF8;
                case VAL_Integer -> CPT_Integer;
                case VAL_Float -> CPT_Float;
                case VAL_Long -> CPT_Long;
                case VAL_Double -> CPT_Double;
                case VAL_Class -> CPT_Class;
                case VAL_String -> CPT_String;
                case VAL_FieldRef -> CPT_FieldRef;
                case VAL_MethodRef -> CPT_MethodRef;
                case VAL_InterfaceMethodRef -> CPT_InterfaceMethodRef;
                case VAL_NameAndType -> CPT_NameAndType;
                case VAL_MethodHandle -> CPT_MethodHandle;
                case VAL_MethodType -> CPT_MethodType;
                case VAL_DynamicInfo -> CPT_DynamicInfo;
                case VAL_InvokeDynamic -> CPT_InvokeDynamic;
                case VAL_ModuleInfo -> CPT_ModuleInfo;
                case VAL_PackageInfo -> CPT_PackageInfo;
                default -> throw new ConfusedCFRException("Invalid constant pool entry type : " + val);
            };
        }
    }
}
