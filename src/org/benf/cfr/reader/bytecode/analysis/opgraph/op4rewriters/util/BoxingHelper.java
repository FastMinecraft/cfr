package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class BoxingHelper {
    @SuppressWarnings("unchecked")
    private static final ObjectSet<Pair<String, String>> unboxing = new ObjectOpenHashSet<>((Pair<String, String>[]) new Pair[]{ Pair.make(
        TypeConstants.boxingNameInt,
        "intValue"
    ), Pair.make(TypeConstants.boxingNameLong, "longValue"), Pair.make(
        TypeConstants.boxingNameDouble,
        "doubleValue"
    ), Pair.make(TypeConstants.boxingNameShort, "shortValue"), Pair.make(
        TypeConstants.boxingNameByte,
        "byteValue"
    ), Pair.make(TypeConstants.boxingNameBoolean, "booleanValue") });

    private static final Map<String, String> unboxingByRawName;

    @SuppressWarnings("unchecked")
    private static final ObjectSet<Pair<String, String>> boxing;

    static {
        boxing = new ObjectOpenHashSet<>((Pair<String, String>[]) new Pair[]{ Pair.make(
            TypeConstants.boxingNameInt,
            "valueOf"
        ), Pair.make(TypeConstants.boxingNameLong, "valueOf"), Pair.make(
            TypeConstants.boxingNameDouble,
            "valueOf"
        ), Pair.make(TypeConstants.boxingNameShort, "valueOf"), Pair.make(
            TypeConstants.boxingNameByte,
            "valueOf"
        ), Pair.make(TypeConstants.boxingNameBoolean, "valueOf") });
        unboxingByRawName = MapFactory.newMap();
        for (Pair<String, String> pair : unboxing) {
            unboxingByRawName.put(pair.getFirst(), pair.getSecond());
        }
    }

    public static Expression sugarUnboxing(MemberFunctionInvokation memberFunctionInvokation) {
        String name = memberFunctionInvokation.getName();
        JavaTypeInstance type = memberFunctionInvokation.getObject().getInferredJavaType().getJavaTypeInstance();
        String rawTypeName = type.getRawName();
        Pair<String, String> testPair = Pair.make(rawTypeName, name);
        if (unboxing.contains(testPair)) {
            return memberFunctionInvokation.getObject();
        }
        return memberFunctionInvokation;
    }

    public static String getUnboxingMethodName(JavaTypeInstance type) {
        return unboxingByRawName.get(type.getRawName());
    }

    public static Expression sugarBoxing(StaticFunctionInvokation staticFunctionInvokation) {
        String name = staticFunctionInvokation.getName();
        JavaTypeInstance type = staticFunctionInvokation.getClazz();
        if (staticFunctionInvokation.getArgs().size() != 1) return staticFunctionInvokation;
        Expression arg1 = staticFunctionInvokation.getArgs().get(0);
        String rawTypeName = type.getRawName();
        Pair<String, String> testPair = Pair.make(rawTypeName, name);
        if (boxing.contains(testPair)) {
            JavaTypeInstance argType = arg1.getInferredJavaType().getJavaTypeInstance();
            if (argType.implicitlyCastsTo(type, null)) {
                return staticFunctionInvokation.getArgs().get(0);
            }
        }
        return staticFunctionInvokation;
    }

    public static boolean isBoxedTypeInclNumber(JavaTypeInstance type) {
        if (RawJavaType.getUnboxedTypeFor(type) != null) return true;
        return type.getRawName().equals(TypeConstants.boxingNameNumber);
    }

    public static boolean isBoxedType(JavaTypeInstance type) {
        return (RawJavaType.getUnboxedTypeFor(type) != null);
    }


}
