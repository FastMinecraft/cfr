package org.benf.cfr.reader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.state.ClassFileSourceChained;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.ClassFileSourceWrapper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.InternalDumperFactoryImpl;
import org.benf.cfr.reader.util.output.SinkDumperFactory;

import java.util.*;
import java.util.concurrent.*;

public class CfrDriverImpl implements CfrDriver {
    private final Options options;
    private final ClassFileSource2 classFileSource;
    private final OutputSinkFactory outputSinkFactory;

    public CfrDriverImpl(ClassFileSource source, OutputSinkFactory outputSinkFactory, Options options, boolean fallbackToDefaultSource) {
        if (options == null) {
            options = new OptionsImpl(new HashMap<>());
        }
        ClassFileSource2 tmpSource;
        if (source == null) {
            tmpSource = new ClassFileSourceImpl(options);
        } else {
            tmpSource = source instanceof ClassFileSource2 ? (ClassFileSource2)source : new ClassFileSourceWrapper(source);
            if (fallbackToDefaultSource) {
                tmpSource = new ClassFileSourceChained(Arrays.asList(tmpSource, new ClassFileSourceImpl(options)));
            }
        }
        this.outputSinkFactory = outputSinkFactory;
        this.options = options;
        this.classFileSource = tmpSource;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void analyse(ObjectList<String> toAnalyse) {
        /*
         * There's an interesting question here - do we want to skip inner classes, if we've been given a wildcard?
         * (or a wildcard expanded by the operating system).
         *
         * Assume yes.
         */
        boolean skipInnerClass = toAnalyse.size() > 1 && options.getOption(OptionsImpl.SKIP_BATCH_INNER_CLASSES);

        // Can't sort a 1.6 singleton list.
        toAnalyse = new ObjectArrayList<>(toAnalyse);
        Collections.sort(toAnalyse);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (String path : toAnalyse) {
            // TODO : We shouldn't have to discard state here.  But we do, because
            // it causes test fails.  (used class name table retains useful symbols).
            classFileSource.informAnalysisRelativePathDetail(null, null);
            // Note - both of these need to be reset, as they have caches.
            DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
            DumperFactory dumperFactory = outputSinkFactory != null ?
                    new SinkDumperFactory(outputSinkFactory, options) :
                    new InternalDumperFactoryImpl(options);

            AnalysisType type = options.getOption(OptionsImpl.ANALYSE_AS);
            if (type == null || type == AnalysisType.DETECT) {
                type = dcCommonState.detectClsJar(path);
            }

            if (type == AnalysisType.JAR || type == AnalysisType.WAR) {
                Driver.doJar(executor, dcCommonState, path, type, dumperFactory);
            } else if (type == AnalysisType.CLASS) {
                executor.execute(() -> Driver.doClass(dcCommonState, path, skipInnerClass, dumperFactory));
            }
        }

        try {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
