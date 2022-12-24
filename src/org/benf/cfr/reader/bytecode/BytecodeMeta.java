package org.benf.cfr.reader.bytecode;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

import java.util.EnumSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BytecodeMeta {
    public enum CodeInfoFlag {
        USES_MONITORS,
        USES_EXCEPTIONS,
        USES_INVOKEDYNAMIC,
        LIVENESS_CLASH,
        ITERATED_TYPE_HINTS,
        SWITCHES,
        // Kotlin uses string switches, even though it marks class files as java6.
        STRING_SWITCHES,
        INSTANCE_OF_MATCHES,
        MALFORMED_SWITCH
    }

    private final EnumSet<CodeInfoFlag> flags = EnumSet.noneOf(CodeInfoFlag.class);

    private final Set<Integer> livenessClashes = new ObjectOpenHashSet<>();
    private final Map<Integer, JavaTypeInstance> iteratedTypeHints = MapFactory.newMap();
    private final Options options;

    public BytecodeMeta(ObjectList<Op01WithProcessedDataAndByteJumps> op1s, AttributeCode code, Options options) {
        this.options = options;
        int flagCount = CodeInfoFlag.values().length;
        if (!code.getExceptionTableEntries().isEmpty()) flags.add(CodeInfoFlag.USES_EXCEPTIONS);
        for (Op01WithProcessedDataAndByteJumps op : op1s) {
            switch (op.getJVMInstr()) {
                case MONITOREXIT, MONITORENTER -> flags.add(CodeInfoFlag.USES_MONITORS);
                case INVOKEDYNAMIC -> flags.add(CodeInfoFlag.USES_INVOKEDYNAMIC);
                case TABLESWITCH, LOOKUPSWITCH -> flags.add(CodeInfoFlag.SWITCHES);
            }
            // Don't bother processing any longer if we've found all the flags!
            if (flags.size() == flagCount) return;
        }
    }

    public boolean has(CodeInfoFlag flag) {
        return flags.contains(flag);
    }

    public void set(CodeInfoFlag flag) {flags.add(flag);}

    public void informLivenessClashes(Set<Integer> slots) {
        flags.add(CodeInfoFlag.LIVENESS_CLASH);
        livenessClashes.addAll(slots);
    }

    public void takeIteratedTypeHint(InferredJavaType inferredJavaType, JavaTypeInstance itertype) {
        int bytecodeIdx = inferredJavaType.getTaggedBytecodeLocation();
        if (bytecodeIdx < 0) return;
        Integer key = bytecodeIdx;
        if (iteratedTypeHints.containsKey(key)) {
            JavaTypeInstance already = iteratedTypeHints.get(key);
            if (already == null) return;
            if (!itertype.equals(already)) {
                iteratedTypeHints.put(key, null);
            }
        } else {
            flags.add(CodeInfoFlag.ITERATED_TYPE_HINTS);
            iteratedTypeHints.put(key, itertype);
        }
    }

    public Map<Integer, JavaTypeInstance> getIteratedTypeHints() {
        return iteratedTypeHints;
    }

    public Set<Integer> getLivenessClashes() {
        return livenessClashes;
    }

    public static Function<BytecodeMeta, Boolean> hasAnyFlag(CodeInfoFlag... flags) {
        return (arg) -> {
            for (CodeInfoFlag flag : flags) {
                if (arg.has(flag)) return true;
            }
            return false;
        };
    }

    public static Function<BytecodeMeta, Boolean> checkParam(final PermittedOptionProvider.Argument<Boolean> param) {
        return arg -> arg.options.getOption(param);
    }
}
