package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

import java.util.*;
import java.util.function.Predicate;

public class TypeUsageInformationImpl implements TypeUsageInformation {
    private final IllegalIdentifierDump iid;
    private final ObjectSet<DetectedStaticImport> staticImports;
    private final JavaRefTypeInstance analysisType;
    private final ObjectSet<JavaRefTypeInstance> usedRefTypes = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<JavaRefTypeInstance> shortenedRefTypes = new ObjectLinkedOpenHashSet<>();
    private final ObjectSet<JavaRefTypeInstance> usedLocalInnerTypes = new ObjectLinkedOpenHashSet<>();
    private final Map<JavaRefTypeInstance, String> displayName = MapFactory.newMap();
    private final Map<String, LinkedList<JavaRefTypeInstance>> shortNames = MapFactory.newLazyMap(arg -> new LinkedList<>());
    private final Predicate<String> allowShorten;
    private final Map<String, Boolean> clashNames = MapFactory.newLazyMap(this::fieldClash);

    public TypeUsageInformationImpl(Options options, JavaRefTypeInstance analysisType, ObjectSet<JavaRefTypeInstance> usedRefTypes, ObjectSet<DetectedStaticImport> staticImports) {
        this.allowShorten = MiscUtils.mkRegexFilter(options.getOption(OptionsImpl.IMPORT_FILTER), true);
        this.analysisType = analysisType;
        this.iid = IllegalIdentifierDump.Factory.getOrNull(options);
        this.staticImports = staticImports;
        initialiseFrom(usedRefTypes);
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return iid;
    }

    @Override
    public JavaRefTypeInstance getAnalysisType() {
        return analysisType;
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return TypeUsageUtils.generateInnerClassShortName(iid, clazz, analysisType, false);
    }

    @Override
    public String generateOverriddenName(JavaRefTypeInstance clazz) {
        if (clazz.getInnerClassHereInfo().isInnerClass()) {
            return TypeUsageUtils.generateInnerClassShortName(iid, clazz, analysisType, true);
        }
        return clazz.getRawName(iid);
    }

    private void initialiseFrom(ObjectSet<JavaRefTypeInstance> usedRefTypes) {
        ObjectList<JavaRefTypeInstance> usedRefs = new ObjectArrayList<>(usedRefTypes);
        usedRefs.sort((a, b) -> a.getRawName(iid).compareTo(b.getRawName(iid)));
        this.usedRefTypes.addAll(usedRefs);

        Pair<ObjectList<JavaRefTypeInstance>, ObjectList<JavaRefTypeInstance>> types = Functional.partition(usedRefs,
            in -> in.getInnerClassHereInfo().isTransitiveInnerClassOf(analysisType)
        );
        this.usedLocalInnerTypes.addAll(types.getFirst());
        addDisplayNames(usedRefTypes);
    }

    private void addDisplayNames(Collection<JavaRefTypeInstance> types) {
        if (!shortNames.isEmpty()) throw new IllegalStateException();
        for (JavaRefTypeInstance type : types) {
            InnerClassInfo innerClassInfo = type.getInnerClassHereInfo();
            if (innerClassInfo.isInnerClass()) {
                String name = generateInnerClassShortName(type);
                shortNames.get(name).addFirst(type);
            } else {
                if (!allowShorten.test(type.getRawName(iid))) {
                    continue;
                }
                String name = type.getRawShortName(iid);
                shortNames.get(name).addLast(type);
            }
        }
        /*
         * Now, decide which is the best - if multiple 'win', then we can't use any.
         */
        for (Map.Entry<String, LinkedList<JavaRefTypeInstance>> nameList : shortNames.entrySet()) {
            LinkedList<JavaRefTypeInstance> typeList = nameList.getValue();
            String name = nameList.getKey();
            if (typeList.size() == 1) {
                displayName.put(typeList.get(0), name);
                shortenedRefTypes.add(typeList.get(0));
                continue;
            }
            /*
             * There's been a collision in shortname.
             *
             * Resolve :
             *
             * 1) Inner class
             * 2) Package
             * Anything
             *
             * This is ... slightly wrong.  If the list is prefixed by any inner classes, they win.
             * otherwise, if there is a SINGLE same package (same level), it wins.
             * Everything else gets the long name.
             */
            class PriClass implements Comparable<PriClass> {
                private final int priType;
                private boolean innerClass = false;
                private final JavaRefTypeInstance type;

                PriClass(JavaRefTypeInstance type) {
                    if (type.equals(analysisType)) {
                        priType = 0;
                    } else {
                        InnerClassInfo innerClassInfo = type.getInnerClassHereInfo();
                        if (innerClassInfo.isInnerClass()) {
                            innerClass = true;
                            if (innerClassInfo.isTransitiveInnerClassOf(analysisType)) {
                                priType = 1;
                            } else {
                                priType = 3;
                            }
                        } else {
                            String p1 = type.getPackageName();
                            String p2 = analysisType.getPackageName();
                            if (p1.startsWith(p2) || p2.startsWith(p1)) {
                                priType = 2;
                            } else {
                                priType = 3;
                            }
                        }
                    }
                    this.type = type;
                }

                @Override
                public int compareTo(PriClass priClass) {
                    return priType - priClass.priType;
                }
            }

            ObjectList<PriClass> priClasses = Functional.map(typeList, PriClass::new);
            Collections.sort(priClasses);

            displayName.put(priClasses.get(0).type, name);
            shortenedRefTypes.add(priClasses.get(0).type);
            priClasses.set(0, null);
            for (int x=0;x<priClasses.size();++x) {
                PriClass priClass = priClasses.get(x);
                if (priClass != null && priClass.priType == 1) {
                    displayName.put(priClass.type, name);
                    shortenedRefTypes.add(priClass.type);
                    priClasses.set(x, null);
                }
            }
            for (PriClass priClass : priClasses) {
                if (priClass == null) continue;
                if (priClass.innerClass) {
                    String useName = generateInnerClassShortName(priClass.type);
                    shortenedRefTypes.add(priClass.type);
                    displayName.put(priClass.type, useName);
                } else {
                    String useName = priClass.type.getRawName(iid);
                    displayName.put(priClass.type, useName);
                }
            }
        }
    }

    private boolean fieldClash(String name) {
        // Does analysisType (or any of its outer classes) have a field of this name?
        ClassFile classFile = analysisType.getClassFile();
        if (classFile == null) return false;
        return classFile.hasAccessibleField(name, analysisType);
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getUsedClassTypes() {
        return usedRefTypes;
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getShortenedClassTypes() {
        return shortenedRefTypes;
    }

    @Override
    public ObjectSet<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return usedLocalInnerTypes;
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return false;
    }

    @Override
    public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
        return staticImports.contains(new DetectedStaticImport(clazz, fixedName));
    }

    @Override
    public ObjectSet<DetectedStaticImport> getDetectedStaticImports() {
        return staticImports;
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        //noinspection SuspiciousMethodCalls
        String res = displayName.get(type);
        if (res == null) {
            // This should not happen, unless we're forcing
            // import filter on a name.
            return type.getRawName(iid);
        }
        if (isNameClash(type, res, typeContext)) {
            return type.getRawName(iid);
        }
        return res;
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return typeContext == TypeContext.Static && clashNames.get(name);
    }
}
