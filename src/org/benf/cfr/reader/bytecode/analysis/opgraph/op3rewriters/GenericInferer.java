package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.*;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class GenericInferer {

    private static class GenericInferData {
        final GenericTypeBinder binder;
        final ObjectSet<JavaGenericPlaceholderTypeInstance> nullPlaceholders;

        private GenericInferData(GenericTypeBinder binder, ObjectSet<JavaGenericPlaceholderTypeInstance> nullPlaceholders) {
            this.binder = binder;
            this.nullPlaceholders = nullPlaceholders;
        }

        private GenericInferData(GenericTypeBinder binder) {
            this.binder = binder;
            this.nullPlaceholders = null;
        }

        public boolean isValid() {
            return binder != null;
        }

        GenericInferData mergeWith(GenericInferData other) {
            if (!isValid()) return this;
            if (!other.isValid()) return other;

            GenericTypeBinder newBinder = binder.mergeWith(other.binder, true);
            if (newBinder == null) return new GenericInferData(null);

            ObjectSet<JavaGenericPlaceholderTypeInstance> newNullPlaceHolders = SetUtil.originalIntersectionOrNull(nullPlaceholders, other.nullPlaceholders);
            return new GenericInferData(newBinder, newNullPlaceHolders);
        }

        /*
         * Ordinarily just return the binder.  however if there are any arguments that have ONLY EVER been 'null',
         * we can make use of that.
         */
        GenericTypeBinder getTypeBinder() {
            if (nullPlaceholders != null && !nullPlaceholders.isEmpty()) {
                for (JavaGenericPlaceholderTypeInstance onlyNull : nullPlaceholders) {
                    binder.suggestOnlyNullBinding(onlyNull);
                }
            }
            return binder;
        }
    }

    private static GenericInferData getGtbNullFiltered(MemberFunctionInvokation m) {
        ObjectList<Expression> args = m.getArgs();
        GenericTypeBinder res =  m.getMethodPrototype().getTypeBinderFor(args);
        ObjectList<Boolean> nulls = m.getNulls();
        if (args.size() != nulls.size()) return new GenericInferData(res);
        boolean found = false;
        for (Boolean b : nulls) {
            if (b) { found = true; break; }
        }
        if (!found) return new GenericInferData(res);
        /*
         * Possibly unwind some of the bindings, if they're identity bindings caused by null arguments
         * this would be better done inside the generic type binder, but
         * I'd like to keep it here, for now...
         */
        ObjectSet<JavaGenericPlaceholderTypeInstance> nullBindings = null;
        for (int x=0,len=args.size();x<len;++x) {
            if (nulls.get(x)) {
                JavaTypeInstance t = args.get(x).getInferredJavaType().getJavaTypeInstance();
                if (t instanceof JavaGenericPlaceholderTypeInstance placeholder) {
                    JavaTypeInstance t2 = res.getBindingFor(placeholder);
                    if (!t2.equals(placeholder)) continue;
                    if (nullBindings == null) nullBindings = new ObjectOpenHashSet<>();
                    res.removeBinding(placeholder);
                    nullBindings.add(placeholder);
                }
            }
        }

        return new GenericInferData(res, nullBindings);
    }

    public static void inferGenericObjectInfoFromCalls(ObjectList<Op03SimpleStatement> statements) {
        // memberFunctionInvokations will either be wrapped in ExpressionStatement or SimpleAssignment.
        ObjectList<MemberFunctionInvokation> memberFunctionInvokations = new ObjectArrayList<>();
        for (Op03SimpleStatement statement : statements) {
            Statement contained = statement.getStatement();
            if (contained instanceof ExpressionStatement) {
                Expression e = ((ExpressionStatement) contained).getExpression();
                if (e instanceof MemberFunctionInvokation) {
                    memberFunctionInvokations.add((MemberFunctionInvokation) e);
                }
            } else if (contained instanceof AssignmentSimple) {
                Expression e = contained.getRValue();
                if (e instanceof MemberFunctionInvokation) {
                    memberFunctionInvokations.add((MemberFunctionInvokation) e);
                }
            }
        }
        if (memberFunctionInvokations.isEmpty()) {
            return;
        }

        Map<Integer, ObjectList<MemberFunctionInvokation>> byTypKey = MapFactory.newTreeMap();
        Functional.groupToMapBy(memberFunctionInvokations, byTypKey,
            arg -> arg.getObject().getInferredJavaType().getLocalId()
        );

        invokationGroup:
        for (Map.Entry<Integer, ObjectList<MemberFunctionInvokation>> entry : byTypKey.entrySet()) {
            ObjectList<MemberFunctionInvokation> invokations = entry.getValue();
            if (invokations.isEmpty()) continue;

            Expression obj0 = invokations.get(0).getObject();
            JavaTypeInstance objectType = obj0.getInferredJavaType().getJavaTypeInstance();
            if (!(objectType instanceof JavaGenericBaseInstance genericType)) continue;
            if (!genericType.hasUnbound()) continue;

            GenericInferData inferData = getGtbNullFiltered(invokations.get(0));
            if (!inferData.isValid()) continue;
            for (int x = 1, len = invokations.size(); x < len; ++x) {
                GenericInferData inferData1 = getGtbNullFiltered(invokations.get(x));
                inferData = inferData.mergeWith(inferData1);
                if (!inferData.isValid()) {
                    continue invokationGroup;
                }
            }

            InferredJavaType inferredJavaType = obj0.getInferredJavaType();
            GenericTypeBinder typeBinder = inferData.getTypeBinder();
            inferredJavaType.deGenerify(typeBinder.getBindingFor(objectType));
        }
    }
}
