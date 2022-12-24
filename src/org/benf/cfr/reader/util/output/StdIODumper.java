package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;

import java.io.BufferedOutputStream;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class StdIODumper extends StreamDumper {
    StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(typeUsageInformation, options, illegalIdentifierDump, context);
    }

    private StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context, ObjectSet<JavaTypeInstance> emitted) {
        super(typeUsageInformation, options, illegalIdentifierDump, context, emitted);
    }

    @Override
    protected void write(String s) {
        System.out.print(s);
    }

    @Override
    public void addSummaryError(Method method, String s) {
    }

    @Override
    public void close() {
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new StdIODumper(innerclassTypeUsageInformation, options, illegalIdentifierDump, context, emitted);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        return new BufferedOutputStream(System.out);
    }
}
