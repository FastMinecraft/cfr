package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.getopt.MutableOptions;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.function.Function;

public abstract class RecoveryOption<T> {

    final Function<BytecodeMeta, Boolean> canhelp;
    protected final PermittedOptionProvider.Argument<T> arg;
    protected final T value;
    private final DecompilerComment decompilerComment;

    RecoveryOption(PermittedOptionProvider.Argument<T> arg, T value, Function<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
        this.arg = arg;
        this.value = value;
        this.decompilerComment = comment;
        this.canhelp = canHelp;
    }

    boolean applyComment(boolean applied, ObjectList<DecompilerComment> commentList) {
        if (!applied) return false;
        if (decompilerComment == null) return true;
        commentList.add(decompilerComment);
        return true;
    }

    public abstract boolean apply(MutableOptions mutableOptions, ObjectList<DecompilerComment> commentList, BytecodeMeta bytecodeMeta);

    public static class TrooleanRO extends RecoveryOption<Troolean> {
        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value) {
            super(arg, value, null, null);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, Function<BytecodeMeta, Boolean> canHelp) {
            super(arg, value, canHelp, null);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, Function<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, ObjectList<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (canhelp != null && !canhelp.apply(bytecodeMeta)) return false;
            return applyComment(mutableOptions.override(arg, value), commentList);
        }
    }

    public static class BooleanRO extends RecoveryOption<Boolean> {
        BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value) {
            super(arg, value, null, null);
        }

        public BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value, Function<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, ObjectList<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (canhelp != null && !canhelp.apply(bytecodeMeta)) return false;
            return applyComment(mutableOptions.override(arg, value), commentList);
        }
    }

    public static class IntRO extends RecoveryOption<Integer> {
        IntRO(PermittedOptionProvider.Argument<Integer> arg, int value) {
            super(arg, value, null, null);
        }

        public IntRO(PermittedOptionProvider.Argument<Integer> arg, int value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        IntRO(PermittedOptionProvider.Argument<Integer> arg, int value, Function<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, ObjectList<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (canhelp != null && !canhelp.apply(bytecodeMeta)) return false;
            if (mutableOptions.optionIsSet(arg)) return false;
            return applyComment(mutableOptions.override(arg, value), commentList);
        }
    }

    public static class ConditionalRO<X, T> extends RecoveryOption<T> {
        private final RecoveryOption<T> delegate;
        private final PermittedOptionProvider.ArgumentParam<X, ?> test;
        private final X required;

        public ConditionalRO(PermittedOptionProvider.ArgumentParam<X, ?> test, X required, RecoveryOption<T> delegate) {
            super(null, null, null, null);
            this.delegate = delegate;
            this.required = required;
            this.test = test;
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, ObjectList<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (mutableOptions.getOption(test, null).equals(required)) {
                return delegate.apply(mutableOptions, commentList, bytecodeMeta);
            }
            return false;
        }
    }
}
