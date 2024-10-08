package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;

public enum ArrayType {
    T_BOOLEAN(4, "boolean", RawJavaType.BOOLEAN),
    T_CHAR(5, "char", RawJavaType.CHAR),
    T_FLOAT(6, "float", RawJavaType.FLOAT),
    T_DOUBLE(7, "double", RawJavaType.DOUBLE),
    T_BYTE(8, "byte", RawJavaType.BYTE),
    T_SHORT(9, "short", RawJavaType.SHORT),
    T_INT(10, "int", RawJavaType.INT),
    T_LONG(11, "long", RawJavaType.LONG);

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final int spec;
    private final String name;
    private final JavaTypeInstance javaTypeInstance;

    ArrayType(int spec, String name, JavaTypeInstance javaTypeInstance) {
        this.spec = spec;
        this.name = name;
        this.javaTypeInstance = javaTypeInstance;
    }

    public static ArrayType getArrayType(int id) {
        return switch (id) {
            case 4 -> T_BOOLEAN;
            case 5 -> T_CHAR;
            case 6 -> T_FLOAT;
            case 7 -> T_DOUBLE;
            case 8 -> T_BYTE;
            case 9 -> T_SHORT;
            case 10 -> T_INT;
            case 11 -> T_LONG;
            default -> throw new ConfusedCFRException("No such primitive array type " + id);
        };
    }

    @Override
    public String toString() {
        return name;
    }

    public JavaTypeInstance getJavaTypeInstance() {
        return javaTypeInstance;
    }
}
