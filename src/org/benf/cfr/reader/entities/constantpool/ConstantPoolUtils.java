package org.benf.cfr.reader.entities.constantpool;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDeltaImpl;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MalformedPrototypeException;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;

public class ConstantPoolUtils {

    private static JavaTypeInstance parseRefType(String tok, ConstantPool cp, boolean isTemplate) {
        int idxGen = tok.indexOf('<');
        int idxStart = 0;

        if (idxGen != -1) {
            tok = tok.replace(">.", ">$");
            ObjectList<JavaTypeInstance> genericTypes;
            StringBuilder already = new StringBuilder();
            while (true) {
                String pre = tok.substring(idxStart, idxGen);
                already.append(pre);
                String gen = tok.substring(idxGen + 1, tok.length() - 1);
                Pair<ObjectList<JavaTypeInstance>, Integer> genericTypePair = parseTypeList(gen, cp);
                genericTypes = genericTypePair.getFirst();
                idxStart = idxGen + genericTypePair.getSecond() + 1;
                if (idxStart < idxGen + gen.length()) {
                    if (tok.charAt(idxStart) != '>') {
                        throw new IllegalStateException();
                    }
                    idxStart++;
                    idxGen = tok.indexOf('<', idxStart);
                    if (idxGen == -1) {
                        // At this point we're parsing an inner class.
                        // Append rest, treat as if no generics.
                        already.append(tok.substring(idxStart));
                        return cp.getClassCache().getRefClassFor(already.toString());
                    }
                    /*
                     * At this point we're discarding the outer generics info - that's not good....
                     */
                } else {
                    break;
                }
            }
            JavaRefTypeInstance clazzType = cp.getClassCache().getRefClassFor(already.toString());
            return new JavaGenericRefTypeInstance(clazzType, genericTypes);
        } else if (isTemplate) {
            return new JavaGenericPlaceholderTypeInstance(tok, cp);
        } else {
            return cp.getClassCache().getRefClassFor(tok);
        }
    }

    public static JavaTypeInstance decodeTypeTok(String tok, ConstantPool cp) {
        int idx = 0;
        int numArrayDims = 0;
        char c = tok.charAt(idx);
        WildcardType wildcardType = WildcardType.NONE;
        if (c == '-' || c == '+') {
            wildcardType = c == '+' ? WildcardType.EXTENDS : WildcardType.SUPER;
            c = tok.charAt(++idx);
        }
        while (c == '[') {
            numArrayDims++;
            c = tok.charAt(++idx);
        }
        JavaTypeInstance javaTypeInstance = switch (c) {
            case '*' -> // wildcard
                new JavaGenericPlaceholderTypeInstance(MiscConstants.UNBOUND_GENERIC, cp);
            case 'L' ->   // object
                parseRefType(tok.substring(idx + 1, tok.length() - 1), cp, false);
            case 'T' ->   // Template
                parseRefType(tok.substring(idx + 1, tok.length() - 1), cp, true);   // byte
            // char
            // integer
            // short
            // boolean
            // float
            // double
            case 'B', 'C', 'I', 'S', 'Z', 'F', 'D', 'J' ->   // long
                decodeRawJavaType(c);
            default -> throw new ConfusedCFRException("Invalid type string " + tok);
        };
        if (numArrayDims > 0) javaTypeInstance = new JavaArrayTypeInstance(numArrayDims, javaTypeInstance);
        if (wildcardType != WildcardType.NONE) {
            javaTypeInstance = new JavaWildcardTypeInstance(wildcardType, javaTypeInstance);
        }
        return javaTypeInstance;
    }

    public static RawJavaType decodeRawJavaType(char c) {
        return switch (c) {
            case 'B' ->   // byte
                RawJavaType.BYTE;
            case 'C' ->   // char
                RawJavaType.CHAR;
            case 'I' ->   // integer
                RawJavaType.INT;
            case 'S' ->   // short
                RawJavaType.SHORT;
            case 'Z' ->   // boolean
                RawJavaType.BOOLEAN;
            case 'F' ->   // float
                RawJavaType.FLOAT;
            case 'D' ->   // double
                RawJavaType.DOUBLE;
            case 'J' ->   // long
                RawJavaType.LONG;
            default -> throw new ConfusedCFRException("Illegal raw java type");
        };
    }

    private static String getNextTypeTok(String proto, int curridx) {
        final int startidx = curridx;
        char c = proto.charAt(curridx);

        if (c == '-' || c == '+') {
            c = proto.charAt(++curridx);
        }

        while (c == '[') {
            c = proto.charAt(++curridx);
        }

        switch (c) {
            case '*' ->   // wildcard
                curridx++;
            case 'L', 'T' -> {
                int openBra = 0;
                do {
                    c = proto.charAt(++curridx);
                    switch (c) {
                        case '<' -> openBra++;
                        case '>' -> openBra--;
                    }
                } while (openBra > 0 || c != ';');
                curridx++;
            }
            // byte
            // char
            // integer
            // short
            // boolean
            // float
            // double
            case 'B', 'C', 'I', 'S', 'Z', 'F', 'D', 'J' ->   // long
                curridx++;
            default ->
                throw new ConfusedCFRException("Can't parse proto : " + proto + " starting " + proto.substring(startidx));
        }
        return proto.substring(startidx, curridx);
    }

    private static String getNextFormalTypeTok(String proto, int curridx) {
        final int startidx = curridx;

        while (proto.charAt(curridx) != ':') {
            curridx++;
        }
        curridx++;
        if (proto.charAt(curridx) != ':') {
            // Class bound.
            String classBound = getNextTypeTok(proto, curridx);
            curridx += classBound.length();
        }
        if (proto.charAt(curridx) == ':') {
            // interface bound
            curridx++;
            String interfaceBound = getNextTypeTok(proto, curridx);
            curridx += interfaceBound.length();
        }
        return proto.substring(startidx, curridx);
    }

    private static FormalTypeParameter decodeFormalTypeTok(String tok, ConstantPool cp) {
        int idx = 0;
        while (tok.charAt(idx) != ':') {
            idx++;
        }
        String name = tok.substring(0, idx);
        idx++;
        JavaTypeInstance classBound = null;
        if (tok.charAt(idx) != ':') {
            // Class bound.
            String classBoundTok = getNextTypeTok(tok, idx);
            classBound = decodeTypeTok(classBoundTok, cp);
            idx += classBoundTok.length();
        }
        JavaTypeInstance interfaceBound = null;
        if (idx < tok.length()) {
            if (tok.charAt(idx) == ':') {
                // interface bound
                idx++;
                String interfaceBoundTok = getNextTypeTok(tok, idx);
                interfaceBound = decodeTypeTok(interfaceBoundTok, cp);
                // should we ever need it.
                //idx += interfaceBoundTok.length();
            }
        }
        return new FormalTypeParameter(name, classBound, interfaceBound);
    }

    public static ClassSignature parseClassSignature(ConstantPoolEntryUTF8 signature, ConstantPool cp) {
        final String sig = signature.getValue();
        int curridx = 0;

        /*
         * Optional formal type parameters
         */
        Pair<Integer, ObjectList<FormalTypeParameter>> formalTypeParametersRes = parseFormalTypeParameters(sig, cp, curridx);
        curridx = formalTypeParametersRes.getFirst();
        ObjectList<FormalTypeParameter> formalTypeParameters = formalTypeParametersRes.getSecond();

        /*
         * Superclass signature.
         */
        String superClassSignatureTok = getNextTypeTok(sig, curridx);
        curridx += superClassSignatureTok.length();
        JavaTypeInstance superClassSignature = decodeTypeTok(superClassSignatureTok, cp);

        ObjectList<JavaTypeInstance> interfaceClassSignatures = new ObjectArrayList<>();
        while (curridx < sig.length()) {
            String interfaceSignatureTok = getNextTypeTok(sig, curridx);
            curridx += interfaceSignatureTok.length();
            interfaceClassSignatures.add(decodeTypeTok(interfaceSignatureTok, cp));
        }

        return new ClassSignature(formalTypeParameters, superClassSignature, interfaceClassSignatures);
    }

    private static Pair<Integer, ObjectList<FormalTypeParameter>> parseFormalTypeParameters(String proto, ConstantPool cp, int curridx) {
        ObjectList<FormalTypeParameter> formalTypeParameters = null;
        FormalTypeParameter last = null;
        if (proto.charAt(curridx) == '<') {
            formalTypeParameters = new ObjectArrayList<>();
            curridx++;
            while (proto.charAt(curridx) != '>') {
                String formalTypeTok = getNextFormalTypeTok(proto, curridx);
                FormalTypeParameter typeTok = decodeFormalTypeTok(formalTypeTok, cp);
                if (typeTok.getName().equals("")) {
                    // previous type was an intersection type!
                    if (last != null) {
                        last.add(typeTok);
                    } // else no idea - have to skip.
                } else {
                    formalTypeParameters.add(typeTok);
                    last = typeTok;
                }
                curridx += formalTypeTok.length();
            }
            curridx++;
        }
        return Pair.make(curridx, formalTypeParameters);
    }

    public static MethodPrototype parseJavaMethodPrototype(DCCommonState state, ClassFile classFile, JavaTypeInstance classType, String name, boolean instanceMethod, Method.MethodConstructor constructorFlag, ConstantPoolEntryUTF8 prototype, ConstantPool cp, boolean varargs, boolean synthetic, VariableNamer variableNamer, String originalDescriptor) {
        String proto = prototype.getValue();
        try {
            int curridx = 0;
            /*
             * Method is itself generic...
             */
            Pair<Integer, ObjectList<FormalTypeParameter>> formalTypeParametersRes = parseFormalTypeParameters(proto, cp, curridx);
            curridx = formalTypeParametersRes.getFirst();
            ObjectList<FormalTypeParameter> formalTypeParameters = formalTypeParametersRes.getSecond();
            Map<String, JavaTypeInstance> ftpMap;
            if (formalTypeParameters == null) {
                ftpMap = Collections.emptyMap();
            } else {
                ftpMap = MapFactory.newMap();
                for (FormalTypeParameter ftp : formalTypeParameters) {
                    ftpMap.put(ftp.getName(), ftp.getBound());
                }
            }

            if (proto.charAt(curridx) != '(') throw new ConfusedCFRException("Prototype " + proto + " is invalid");
            curridx++;
            ObjectList<JavaTypeInstance> args = new ObjectArrayList<>();
            // could use parseTypeList below.
            while (proto.charAt(curridx) != ')') {
                curridx = processTypeEntry(cp, proto, curridx, ftpMap, args);
            }
            curridx++;
            JavaTypeInstance resultType = RawJavaType.VOID;
            if (proto.charAt(curridx) == 'V') {
                curridx++;
            } else {
                String resTypeTok = getNextTypeTok(proto, curridx);
                curridx += resTypeTok.length();
                resultType = decodeTypeTok(resTypeTok, cp);
            }
            // And process any exceptions....
            ObjectList<JavaTypeInstance> exceptions = ObjectLists.emptyList();
            if (curridx < proto.length()) {
                exceptions = new ObjectArrayList<>();
                while (curridx < proto.length() && proto.charAt(curridx) == '^') {
                    curridx++;
                    curridx = processTypeEntry(cp, proto, curridx, ftpMap, exceptions);
                }
            }
            return new MethodPrototype(state, classFile, classType, name, instanceMethod, constructorFlag, formalTypeParameters, args, resultType, exceptions, varargs, variableNamer, synthetic, originalDescriptor);
        } catch (StringIndexOutOfBoundsException e) {
            throw new MalformedPrototypeException(proto, e);
        }
    }

    private static int processTypeEntry(ConstantPool cp, String proto, int curridx, Map<String, JavaTypeInstance> ftpMap, ObjectList<JavaTypeInstance> args) {
        String typeTok = getNextTypeTok(proto, curridx);
        JavaTypeInstance type = decodeTypeTok(typeTok, cp);
        if (type instanceof JavaGenericPlaceholderTypeInstance) {
            type = ((JavaGenericPlaceholderTypeInstance) type).withBound(ftpMap.get(type.getRawName()));
        }
        args.add(type);
        curridx += typeTok.length();
        return curridx;
    }

    private static Pair<ObjectList<JavaTypeInstance>, Integer> parseTypeList(String proto, ConstantPool cp) {
        int curridx = 0;
        int len = proto.length();
        ObjectList<JavaTypeInstance> res = new ObjectArrayList<>();
        while (curridx < len && proto.charAt(curridx) != '>') {
            String typeTok = getNextTypeTok(proto, curridx);
            res.add(decodeTypeTok(typeTok, cp));
            curridx += typeTok.length();
        }
        return Pair.make(res, curridx);
    }

    /*
     * could be rephrased in terms of MethodPrototype.
     */
    static StackDelta parseMethodPrototype(boolean member, ConstantPoolEntryUTF8 prototype, ConstantPool cp) {
        String proto = prototype.getValue();
        int curridx = 1;
        if (!proto.startsWith("(")) throw new ConfusedCFRException("Prototype " + proto + " is invalid");
        StackTypes argumentTypes = new StackTypes();
        if (member) {
            argumentTypes.add(StackType.REF); // thisPtr
        }
        while (proto.charAt(curridx) != ')') {
            String typeTok = getNextTypeTok(proto, curridx);
            argumentTypes.add(decodeTypeTok(typeTok, cp).getStackType());
            curridx += typeTok.length();
        }
        curridx++;
        StackTypes resultType = StackTypes.EMPTY; // void.
        if (proto.charAt(curridx) != 'V') {
            resultType = decodeTypeTok(getNextTypeTok(proto, curridx), cp).getStackType().asList();
        }
        //        logger.info("Parsed prototype " + proto + " as " + res);
        return new StackDeltaImpl(argumentTypes, resultType);
    }
}
