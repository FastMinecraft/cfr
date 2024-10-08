package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.*;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperEnum;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.*;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

import java.util.Collection;

import java.util.Map;

public class EnumClassRewriter {

    public static void rewriteEnumClass(ClassFile classFile, DCCommonState state) {
        Options options = state.getOptions();
        ClassFileVersion classFileVersion = classFile.getClassFileVersion();

        if (!options.getOption(OptionsImpl.ENUM_SUGAR, classFileVersion)) return;

        JavaTypeInstance classType = classFile.getClassType();
        if (!classFile.getBindingSupers().containsBase(TypeConstants.ENUM)) {
            return;
        }

        EnumClassRewriter c = new EnumClassRewriter(classFile, classType, state);
        if (!c.rewrite()) {
            c.removeAllRemainingSupers();
        }
    }

    private final ClassFile classFile;
    private final JavaTypeInstance classType;
    private final DCCommonState state;
    private final InferredJavaType clazzIJT;
    private final Options options;


    private EnumClassRewriter(ClassFile classFile, JavaTypeInstance classType, DCCommonState state) {
        this.classFile = classFile;
        this.classType = classType;
        this.state = state;
        this.options = state.getOptions();
        this.clazzIJT = new InferredJavaType(classType, InferredJavaType.Source.UNKNOWN, true);
    }

    /*
     * Derived classes of enums (anonymous classes) still need to be processed.
     */
    private void removeAllRemainingSupers() {
        ObjectList<Method> constructors = classFile.getConstructors();
        EnumAllSuperRewriter enumSuperRewriter = new EnumAllSuperRewriter();
        for (Method constructor : constructors) {
            enumSuperRewriter.rewrite(constructor.getAnalysis());
        }
    }

    private boolean rewrite() {
        // Ok, it's an enum....
        // Verify the static initialiser - for each instance of enum created in there, make sure
        // it's a public static enum member.
        // Verify that the values field is also valid.
        // Nop out this code from the static initialiser.  There MAY be code left, if the enum
        // actually HAD a static initialiser!

        Method staticInit;
        try {
            staticInit = classFile.getMethodByName(MiscConstants.STATIC_INIT_METHOD).get(0);
        } catch (NoSuchMethodException e) {
            // Should have a static constructor.
            return false;
        }

        Op04StructuredStatement staticInitCode = staticInit.getAnalysis();
        if (!staticInitCode.isFullyStructured()) return false;
        EnumInitMatchCollector initMatchCollector = analyseStaticMethod(staticInitCode);
        if (initMatchCollector == null) return false;

        /*
         * Need to hide all the fields, the 'static values' and 'static valueOf' method.
         */
        Method valueOf;
        Method values;
        try {
            valueOf = classFile.getMethodByName("valueOf").get(0);
            values = classFile.getMethodByName("values").get(0);
        } catch (NoSuchMethodException e) {
            return false;
        }
        valueOf.hideSynthetic();
        values.hideSynthetic();
        for (ClassFileField field : initMatchCollector.getMatchedHideTheseFields()) {
            field.markHidden();
        }

        Map<StaticVariable, MatchedEnumEntry> entryMap = initMatchCollector.getEntryMap();
        MatchedArrayData matchedArray = initMatchCollector.getMatchedArray();

        // Note that nopping out this code means that we're no longer referencing the code other than
        // in the dumper! :(
        for (MatchedEnumEntry entry : entryMap.values()) {
            entry.container().nopOut();
        }
        matchedArray.container().nopOut();
        Method matchedArrayMethod = matchedArray.methodContainer();
        if (matchedArrayMethod != null) {
            matchedArrayMethod.hideSynthetic();
        }

        /*
         * And remove all the super calls from the constructors.... AND remove the first 2 arguments from each.
         */
        ObjectList<Method> constructors = classFile.getConstructors();
        EnumSuperRewriter enumSuperRewriter = new EnumSuperRewriter();
        for (Method constructor : constructors) {
            enumSuperRewriter.rewrite(constructor.getAnalysis());
        }

        ObjectList<Pair<StaticVariable, AbstractConstructorInvokation>> entries = new ObjectArrayList<>();
        for (Map.Entry<StaticVariable, MatchedEnumEntry> entry : entryMap.entrySet()) {
            entries.add(Pair.make(entry.getKey(), entry.getValue().data()));
        }
        classFile.setDumpHelper(new ClassFileDumperEnum(state, entries));

        /*
         * Check for illegal identifiers - if there are any, either flag it, or rename.
         * NB: We need a second pass if we're renaming.
         */
        boolean rename_members = options.getOption(OptionsImpl.RENAME_ENUM_MEMBERS);

        if (rename_members) {

            Collection<String> content = Functional.map(classFile.getFields(),
                ClassFileField::getFieldName
            );
            ObjectSet<String> fieldNames = new ObjectOpenHashSet<>(content);

            ObjectList<Pair<StaticVariable, String>> renames = new ObjectArrayList<>();

            for (Pair<StaticVariable, AbstractConstructorInvokation> entry : entries) {
                StaticVariable sv = entry.getFirst();
                AbstractConstructorInvokation aci = entry.getSecond();
                String name = sv.getFieldName();

                /*
                 * Two things are going on here - is it illegal?  Should it be renamed?
                 */
                Expression expectedNameExp = aci.getArgs().get(0);
                String expectedName = name;
                if (expectedNameExp.getInferredJavaType().getJavaTypeInstance() == TypeConstants.STRING) {
                    if (expectedNameExp instanceof Literal) {
                        Object expectedValue = ((Literal) expectedNameExp).getValue().getValue();
                        if (expectedValue instanceof String) {
                            expectedName = QuotingUtils.unquoteString((String) expectedValue);
                        }
                    }
                }

                if (!name.equals(expectedName)) {
                    if (!IllegalIdentifierReplacement.isIllegal(expectedName)) {
                        renames.add(Pair.make(sv, expectedName));
                    }
                }
            }

            for (Pair<StaticVariable, String> rename : renames) {
                String newName = rename.getSecond();
                StaticVariable sv = rename.getFirst();
                if (fieldNames.contains(newName)) {
                    classFile.addComment("Tried to rename field '" + sv.getFieldName() + "' to '" + newName + "' but it's alread used.");
                    continue;
                }
                fieldNames.remove(sv.getFieldName());
                fieldNames.add(newName);
                sv.getClassFileField().overrideName(newName);
            }
        }

        return true;
    }

    /*
     * Expect the static initialiser to be in a fairly common format - i.e. we will
     * NOT attempt to deal with obfuscation.
     */
    private EnumInitMatchCollector analyseStaticMethod(Op04StructuredStatement statement) {
        ObjectList<StructuredStatement> statements = new ObjectArrayList<>();
        statement.linearizeStatementsInto(statements);

        // Filter out the comments.
        statements = Functional.filter(statements, in -> !(in instanceof StructuredComment));

        WildcardMatch wcm = new WildcardMatch();

        InferredJavaType clazzIJT = new InferredJavaType(classType, InferredJavaType.Source.UNKNOWN, true);
        JavaTypeInstance arrayType = new JavaArrayTypeInstance(1, classType);
        InferredJavaType clazzAIJT = new InferredJavaType(arrayType, InferredJavaType.Source.UNKNOWN, true);
        Matcher<StructuredStatement> matcher = new MatchSequence(
                new BeginBlock(null),
                new KleeneStar(
                        new MatchOneOf(
                                new ResetAfterTest(wcm, new CollectMatch("entry", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("e", classType, clazzIJT), wcm.getConstructorSimpleWildcard("c", classType)))),
                                new ResetAfterTest(wcm, new CollectMatch("entryderived", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("e2", classType, clazzIJT, false), wcm.getConstructorAnonymousWildcard("c2", null))))
                        )
                ),
                new MatchOneOf(
                    new ResetAfterTest(wcm, new CollectMatch("values", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", classType, clazzAIJT), wcm.getNewArrayWildCard("v", 0, 1)))),
                    new ResetAfterTest(wcm, new CollectMatch("values15", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", classType, clazzAIJT), wcm.getStaticFunction("v", this.classType, new JavaArrayTypeInstance(1, this.classType), null)))),
                    new ResetAfterTest(wcm, new CollectMatch("noValues", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", classType, clazzAIJT), new NewObjectArray(BytecodeLoc.NONE, ObjectLists.singleton(Literal.INT_ZERO), arrayType))))
                )
        );

        MatchIterator<StructuredStatement> mi = new MatchIterator<>(statements);
        EnumInitMatchCollector matchCollector = new EnumInitMatchCollector(wcm);

        mi.advance();
        if (!matcher.match(mi, matchCollector)) {
            return null;
        }

        if (!matchCollector.isValid()) return null;
        return matchCollector;
    }

    private record MatchedEnumEntry(Op04StructuredStatement container, AbstractConstructorInvokation data) {
    }

    private record MatchedArrayData(Op04StructuredStatement container, Method methodContainer,
                                    ObjectList<Expression> elementExpressions) {
    }

    private class EnumInitMatchCollector extends AbstractMatchResultIterator {

        private final WildcardMatch wcm;
        private final Map<StaticVariable, MatchedEnumEntry> entryMap = new Object2ObjectLinkedOpenHashMap<>();
        private MatchedArrayData matchedArray;
        private final ObjectList<ClassFileField> matchedHideTheseFields = new ObjectArrayList<>();

        private EnumInitMatchCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            if (name.equals("entry")) {
                StaticVariable staticVariable = wcm.getStaticVariable("e").getMatch();
                ConstructorInvokationSimple constructorInvokation = wcm.getConstructorSimpleWildcard("c").getMatch();
                entryMap.put(staticVariable, new MatchedEnumEntry(statement.getContainer(), constructorInvokation));
                return;
            }

            if (name.equals("entryderived")) {
                StaticVariable staticVariable = wcm.getStaticVariable("e2").getMatch();
                ConstructorInvokationAnonymousInner constructorInvokation = wcm.getConstructorAnonymousWildcard("c2").getMatch();
                entryMap.put(staticVariable, new MatchedEnumEntry(statement.getContainer(), constructorInvokation));
                return;
            }

            switch (name) {
                case "values" -> {
                    AbstractNewArray abstractNewArray = wcm.getNewArrayWildCard("v").getMatch();
                    // We need this to have been successfully desugared...
                    if (abstractNewArray instanceof NewAnonymousArray) {
                        matchedArray = new MatchedArrayData(
                            statement.getContainer(),
                            null,
                            ((NewAnonymousArray) abstractNewArray).getValues()
                        );
                    }
                }
                case "noValues" -> matchedArray = new MatchedArrayData(statement.getContainer(),
                    null,
                    ObjectLists.emptyList()
                );
                case "values15" -> {
                    StaticFunctionInvokation sf = wcm.getStaticFunction("v").getMatch();
                    matchedArray = getJava15Values(statement.getContainer(), sf.getMethodPrototype());
                }
            }

        }

        boolean isValid() {
            /*
             * Validate that the entries of the "values" array are those of the enum entries.
             *
             * Examine all static members, make sure they're in this set.
             */
            ObjectList<ClassFileField> fields = classFile.getFields();
            for (ClassFileField classFileField : fields) {
                Field field = classFileField.getField();
                JavaTypeInstance fieldType = field.getJavaTypeInstance();
                boolean isStatic = field.testAccessFlag(AccessFlag.ACC_STATIC);
                boolean isEnum = field.testAccessFlag(AccessFlag.ACC_ENUM);
                boolean expected = (isStatic && isEnum && fieldType.equals(classType));
                StaticVariable tmp = new StaticVariable(clazzIJT, classType, field.getFieldName());
                if (expected != entryMap.containsKey(tmp)) {
                    return false;
                }
                if (expected) {
                    matchedHideTheseFields.add(classFileField);
                }
            }
            if (matchedArray == null) return false;
            ObjectList<Expression> values = matchedArray.elementExpressions();
            if (values.size() != entryMap.size()) {
                return false;
            }
            for (Expression value : values) {
                if (!(value instanceof LValueExpression lValueExpression)) {
                    return false;
                }
                LValue lvalue = lValueExpression.getLValue();
                if (!(lvalue instanceof StaticVariable staticVariable)) {
                    return false;
                }
                if (!entryMap.containsKey(staticVariable)) {
                    return false;
                }
            }
            LValue valuesArray = ((StructuredAssignment) (matchedArray.container().getStatement())).getLvalue();
            if (!(valuesArray instanceof StaticVariable valuesArrayStatic)) {
                return false;
            }
            try {
                ClassFileField valuesField = classFile.getFieldByName(valuesArrayStatic.getFieldName(), valuesArrayStatic.getInferredJavaType().getJavaTypeInstance());
                if (!valuesField.getField().testAccessFlag(AccessFlag.ACC_STATIC)) {
                    return false;
                }
                matchedHideTheseFields.add(valuesField);
            } catch (NoSuchFieldException e) {
                return false;
            }
            return true;
        }

        private ObjectList<ClassFileField> getMatchedHideTheseFields() {
            return matchedHideTheseFields;
        }

        private Map<StaticVariable, MatchedEnumEntry> getEntryMap() {
            return entryMap;
        }

        private MatchedArrayData getMatchedArray() {
            return matchedArray;
        }
    }

    /*
     * This could be a more sophisticated matcher with a wildcardmatch, but  we currently only have very
     * simple single match to find.
     */
    private MatchedArrayData getJava15Values(Op04StructuredStatement container, MethodPrototype methodPrototype) {
        Method method = null;
        try {
            method = classFile.getMethodByPrototype(methodPrototype);
        } catch (NoSuchMethodException e) {
            return null;
        }
        Op04StructuredStatement stm = method.getAnalysis();
        // We require this to be a single return of an anonymous array.
        if (!(stm.getStatement() instanceof Block blk)) return null;
        Optional<Op04StructuredStatement> inner = blk.getMaybeJustOneStatement();
        if (!inner.isSet()) return null;
        StructuredStatement testArr = inner.getValue().getStatement();
        // Return of new anonymous array.
        if (!(testArr instanceof StructuredReturn)) return null;
        Expression rv = ((StructuredReturn)testArr).getValue();

        if (rv instanceof NewAnonymousArray) {
            return new MatchedArrayData(container, method, ((NewAnonymousArray)rv).getValues());
        } else if (rv instanceof NewObjectArray newArrayExpr) {
            // Expecting `new EnumType[0]`
            if (newArrayExpr.getNumDims() != 1) return null;
            if (!newArrayExpr.getDimSize(0).equals(Literal.INT_ZERO)) return null;
            return new MatchedArrayData(container, method, ObjectLists.emptyList());
        } else {
            return null;
        }
    }
}
