package org.benf.cfr.reader.entities.classfilehelpers;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfoUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.FakeMethod;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleAnnotations;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;

import java.util.*;

abstract class AbstractClassFileDumper implements ClassFileDumper {

    static String getAccessFlagsString(Set<AccessFlag> accessFlags, AccessFlag[] dumpableAccessFlags) {
        StringBuilder sb = new StringBuilder();

        for (AccessFlag accessFlag : dumpableAccessFlags) {
            if (accessFlags.contains(accessFlag)) sb.append(accessFlag).append(' ');
        }
        return sb.toString();
    }

    private final DCCommonState dcCommonState;

    AbstractClassFileDumper(DCCommonState dcCommonState) {
        this.dcCommonState = dcCommonState;

    }


    void dumpTopHeader(ClassFile classFile, Dumper d, boolean showPackage) {
        if (dcCommonState == null) return;
        Options options = dcCommonState.getOptions();
        String header = MiscConstants.CFR_HEADER_BRA;
        if (options.getOption(OptionsImpl.SHOW_CFR_VERSION)) {
            header += " " + CfrVersionInfo.VERSION_INFO;
        }
        header += '.';

        d.beginBlockComment(false);
        d.print(header).newln();
        if (options.getOption(OptionsImpl.DECOMPILER_COMMENTS)) {
            TypeUsageInformation typeUsageInformation = d.getTypeUsageInformation();
            ObjectList<JavaTypeInstance> couldNotLoad = new ObjectArrayList<>();
            for (JavaTypeInstance type : typeUsageInformation.getUsedClassTypes()) {
                if (type instanceof JavaRefTypeInstance) {
                    ClassFile loadedClass = null;
                    try {
                        loadedClass = dcCommonState.getClassFile(type);
                    } catch (CannotLoadClassException ignore) {
                    }
                    if (loadedClass == null) {
                        couldNotLoad.add(type);
                    }
                }
            }
            if (!couldNotLoad.isEmpty()) {
                d.newln();
                d.print("Could not load the following classes:").newln();
                for (JavaTypeInstance type : couldNotLoad) {
                    d.print(" ").print(type.getRawName()).newln();
                }
            }
        }
        d.endBlockComment();
        // package name may be empty, in which case it's ignored by dumper.
        if (showPackage) {
            d.packageName(classFile.getRefClassType());
        }
    }

    protected void dumpImplements(Dumper d, ClassSignature signature) {
        ObjectList<JavaTypeInstance> interfaces = signature.interfaces();
        if (!interfaces.isEmpty()) {
            d.keyword("implements ");
            int size = interfaces.size();
            for (int x = 0; x < size; ++x) {
                JavaTypeInstance iface = interfaces.get(x);
                d.dump(iface).separator((x < (size - 1) ? "," : "")).newln();
            }
        }
    }

    protected void dumpPermitted(ClassFile c, Dumper d) {
        c.dumpPermitted(d);
    }

    void dumpImports(Dumper d, ClassFile classFile) {
        /*
         * It's a bit irritating that we have to check obfuscations here, but we are stripping unused types,
         * and don't want to strip obfuscated names.
         */
        ObjectList<JavaTypeInstance> classTypes = d.getObfuscationMapping().get(classFile.getAllClassTypes());
        Set<JavaRefTypeInstance> types = d.getTypeUsageInformation().getShortenedClassTypes();
        //noinspection SuspiciousMethodCalls
        classTypes.forEach(types::remove);
        /*
         * Now - for all inner class types, remove them, but make sure the base class of the inner class is imported.
         */
        ObjectList<JavaRefTypeInstance> inners = Functional.filter(types, in -> in.getInnerClassHereInfo().isInnerClass());
        inners.forEach(types::remove);
        for (JavaRefTypeInstance inner : inners) {
            types.add(InnerClassInfoUtils.getTransitiveOuterClass(inner));
        }
        /*
         * Additional pass to find types we don't want to import for other reasons (default package, etc).
         *
         * (as with others, this could be done with an iterator pass to avoid having scan-then-remove,
         * but this feels cleaner for very little cost).
         */
        types.removeIf(in -> in.getPackageName().isEmpty());

        Options options = dcCommonState.getOptions();
        final IllegalIdentifierDump iid = IllegalIdentifierDump.Factory.getOrNull(options);

        Collection<JavaRefTypeInstance> importTypes = types;
        if (options.getOption(OptionsImpl.HIDE_LANG_IMPORTS)) {
            importTypes = Functional.filter(importTypes, in -> !"java.lang".equals(in.getPackageName()));
        }

        ObjectList<String> names = Functional.map(importTypes, arg -> {
            if (arg.getInnerClassHereInfo().isInnerClass()) {
                String name = arg.getRawName(iid);
                return name.replace(MiscConstants.INNER_CLASS_SEP_CHAR, '.');
            }
            return arg.getRawName(iid);
        });

        boolean action = false;
        if (!names.isEmpty()) {
            Collections.sort(names);
            for (String name : names) {
                d.keyword("import ").print(name).endCodeln();
            }
            action = true;
        }

        Set<DetectedStaticImport> staticImports = d.getTypeUsageInformation().getDetectedStaticImports();
        if (!staticImports.isEmpty()) {
            ObjectList<String> sis = Functional.map(staticImports, arg -> {
                String name = arg.getClazz().getRawName(iid);
                return name.replace(MiscConstants.INNER_CLASS_SEP_CHAR, '.') + '.' + arg.getName();
            });
            Collections.sort(sis);
            for (String si : sis) {
                d.keyword("import").print(' ').keyword("static").print(" " + si).endCodeln();
            }
            action = true;
        }

        if (action) {
            d.newln();
        }
    }

    void dumpMethods(ClassFile classFile, Dumper d, boolean first, boolean asClass) {
        ObjectList<Method> methods = classFile.getMethods();
        if (!methods.isEmpty()) {
            for (Method method : methods) {
                if (method.hiddenState() != Method.Visibility.Visible) {
                    continue;
                }
                if (!first) {
                    d.newln();
                }
                first = false;
                method.dump(d, asClass);
            }
        }
        /*
         * Any 'additional' methods we've had to create to work around unsynthesisable code.
         */
        ObjectList<FakeMethod> fakes = classFile.getMethodFakes();
        if (fakes != null && !fakes.isEmpty()) {
            for (FakeMethod method : fakes) {
                if (!first) {
                    d.newln();
                }
                first = false;
                method.dump(d);
            }
        }
    }

    void dumpComments(ClassFile classFile, Dumper d) {
        d.dumpClassDoc(classFile.getClassType());
        DecompilerComments comments = classFile.getNullableDecompilerComments();
        if (comments == null) return;
        comments.dump(d);
    }

    void dumpAnnotations(ClassFile classFile, Dumper d) {
        AttributeMap classFileAttributes = classFile.getAttributes();
        AttributeRuntimeVisibleAnnotations runtimeVisibleAnnotations = classFileAttributes.getByName(AttributeRuntimeVisibleAnnotations.ATTRIBUTE_NAME) ;
        AttributeRuntimeInvisibleAnnotations runtimeInvisibleAnnotations = classFileAttributes.getByName(AttributeRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);
        if (runtimeVisibleAnnotations != null) runtimeVisibleAnnotations.dump(d);
        if (runtimeInvisibleAnnotations != null) runtimeInvisibleAnnotations.dump(d);
    }

}
