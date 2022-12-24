package org.benf.cfr.reader.bytecode;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.getopt.MutableOptions;
import org.benf.cfr.reader.util.getopt.Options;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class RecoveryOptions {
    private final ObjectList<RecoveryOption<?>> recoveryOptions;

    public RecoveryOptions(RecoveryOption<?>... recoveryOptions) {
        this.recoveryOptions = ObjectList.of(recoveryOptions);
    }

    public RecoveryOptions(RecoveryOptions prev, RecoveryOption<?>... recoveryOptions) {
        ObjectList<RecoveryOption<?>> recoveryOptionList = ObjectList.of(recoveryOptions);
        this.recoveryOptions = new ObjectArrayList<>();
        this.recoveryOptions.addAll(prev.recoveryOptions);
        this.recoveryOptions.addAll(recoveryOptionList);
    }

    public record Applied(Options options, ObjectList<DecompilerComment> comments, boolean valid) {
    }

    public Applied apply(DCCommonState commonState, Options originalOptions, BytecodeMeta bytecodeMeta) {
        MutableOptions mutableOptions = new MutableOptions(originalOptions);
        ObjectList<DecompilerComment> appliedComments = new ObjectArrayList<>();
        boolean hadEffect = false;
        for (RecoveryOption<?> option : recoveryOptions) {
            if (option.apply(mutableOptions, appliedComments, bytecodeMeta)) hadEffect = true;
        }
        return new Applied(mutableOptions, appliedComments, hadEffect);
    }
}
