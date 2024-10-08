package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForIterStatement;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.Functional;

public class LoopLivenessClash {

    public static boolean detect(ObjectList<Op03SimpleStatement> statements, BytecodeMeta bytecodeMeta) {
        ObjectList<Op03SimpleStatement> iters = Functional.filter(statements, new TypeFilter<>(ForIterStatement.class));
        if (iters.isEmpty()) return false;

        boolean found = false;
        for (Op03SimpleStatement iter : iters) {
            if (detect(iter, bytecodeMeta)) found = true;
        }
        return found;
    }

    private static JavaTypeInstance getIterableIterType(JavaTypeInstance type) {
        if (!(type instanceof JavaGenericRefTypeInstance generic)) return null;
        BindingSuperContainer bindingSuperContainer = type.getBindingSupers();

        JavaGenericRefTypeInstance iterType = bindingSuperContainer.getBoundSuperForBase(TypeConstants.ITERABLE);
        GenericTypeBinder typeBinder = GenericTypeBinder.extractBindings(iterType,generic);
        JavaGenericRefTypeInstance boundIterable = iterType.getBoundInstance(typeBinder);
        ObjectList<JavaTypeInstance> iterBindings = boundIterable.getGenericTypes();
        if (iterBindings.size() != 1) return null;
        return iterBindings.get(0);
    }

    private static boolean detect(Op03SimpleStatement statement, BytecodeMeta bytecodeMeta) {
        boolean res = false;
        ForIterStatement forIterStatement = (ForIterStatement)statement.getStatement();
        LValue iterator = forIterStatement.getCreatedLValue();
        // Shouldn't happen, but if it has, don't check further.
        if (!(iterator instanceof LocalVariable lvIter)) return res;

        JavaTypeInstance iterType = iterator.getInferredJavaType().getJavaTypeInstance();
        InferredJavaType inferredListType = forIterStatement.getList().getInferredJavaType();
        LValue hiddenList = forIterStatement.getHiddenList();
        if (hiddenList != null && hiddenList.getInferredJavaType().isClash()) {
            if (hiddenList instanceof LocalVariable) {
                bytecodeMeta.informLivenessClashes(IntSets.singleton(((LocalVariable) hiddenList).getIdx()));
                res = true;
            }
        }
        JavaTypeInstance listType = inferredListType.getJavaTypeInstance();
        // Figure out the iterable type - if we have an array / list.
        JavaTypeInstance listIterType;
        if (listType instanceof JavaArrayTypeInstance) {
            listIterType = listType.removeAnArrayIndirection();
        } else {
            listIterType = getIterableIterType(listType);
        }
        if (listIterType == null) return res;
        if (iterType.equals(listIterType)) return res;

        // We've probably screwed up the types by failing to get the correct list type instead.
        // If it's appropriate, collect the 'better' type.
        if (listIterType instanceof JavaGenericPlaceholderTypeInstance) {
            bytecodeMeta.takeIteratedTypeHint(inferredListType, iterType);
            return res;
        }
        // Or if we're unboxed-looping round a boxed type - this isn't a clash.
        boolean listIterTypeRaw = listIterType.isRaw();
        if (listIterTypeRaw ^ iterType.isRaw()) {
            JavaTypeInstance raw = listIterTypeRaw ? listIterType : iterType;
            JavaTypeInstance oth = listIterTypeRaw ? iterType : listIterType;
            if (raw == RawJavaType.getUnboxedTypeFor(oth)) return res;
        }

        /*
         * If we're iterating using an object, we're NOT strictly wrong, though could surely do better.
         */
//        if (iterType == TypeConstants.OBJECT) return false;
        /*
         * We're not iterating over the right thing.
         */
        IntSet clashes = new IntOpenHashSet();
        clashes.add(lvIter.getIdx());
        bytecodeMeta.informLivenessClashes(clashes);
        return true;
    }
}
