package org.benf.cfr.reader.entities.bootstrap;

public enum MethodHandleBehaviour {
    GET_FIELD,
    GET_STATIC,
    PUT_FIELD,
    PUT_STATIC,
    INVOKE_VIRTUAL,
    INVOKE_STATIC,
    INVOKE_SPECIAL,
    NEW_INVOKE_SPECIAL,
    INVOKE_INTERFACE;

    public static MethodHandleBehaviour decode(byte value) {
        return switch (value) {
            case 1 -> GET_FIELD;
            case 2 -> GET_STATIC;
            case 3 -> PUT_FIELD;
            case 4 -> PUT_STATIC;
            case 5 -> INVOKE_VIRTUAL;
            case 6 -> INVOKE_STATIC;
            case 7 -> INVOKE_SPECIAL;
            case 8 -> NEW_INVOKE_SPECIAL;
            case 9 -> INVOKE_INTERFACE;
            default -> throw new IllegalArgumentException("Unknown method handle behaviour " + value);
        };
    }
}
