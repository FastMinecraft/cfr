package org.benf.cfr.reader.mapping;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ClassMapping {
    private final JavaRefTypeInstance realClass;
    private final JavaRefTypeInstance obClass;
    private final Map<String, Map<MethodData, String>> methodMappings = MapFactory.newMap();
    private final Map<String, FieldMapping> fieldMappings = MapFactory.newMap();

    private static class MethodData {
        final ObjectList<JavaTypeInstance> args;

        MethodData(ObjectList<JavaTypeInstance> argTypes) {
            if (!argTypes.isEmpty()) {
                argTypes = Functional.map(argTypes, JavaTypeInstance::getDeGenerifiedType);
            }
            this.args = argTypes;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (JavaTypeInstance a : args) {
                hash = 31 * hash + a.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != MethodData.class) return false;
            MethodData other = (MethodData)obj;
            if (other.args.size() != args.size()) return false;
            return args.equals(other.args);
        }

        @Override
        public String toString() {
            return "" + args.size() + " args : " + args;
        }
    }

    ClassMapping(JavaRefTypeInstance realClass, JavaRefTypeInstance obClass) {
        this.realClass = realClass;
        this.obClass = obClass;
    }

    void addMethodMapping(MethodMapping m) {
        Map<MethodData, String> byName = methodMappings.get(m.getName());
        //noinspection Java8MapApi
        if (byName == null) {
            byName = new Object2ObjectLinkedOpenHashMap<>();
            methodMappings.put(m.getName(), byName);
        }
        // Don't need to check return type, two methods can't differ just by return type.
        MethodData data = new MethodData(m.getArgTypes());
        byName.put(data, m.getRename());
    }

    void addFieldMapping(FieldMapping f) {
        fieldMappings.put(f.getName(), f);
    }

    JavaRefTypeInstance getRealClass() {
        return realClass;
    }

    JavaRefTypeInstance getObClass() {
        return obClass;
    }

    String getMethodName(String displayName, ObjectList<JavaTypeInstance> args, final Mapping mapping, Dumper d) {
        Map<MethodData, String> poss = methodMappings.get(displayName);
        if (poss == null) {
            return displayName;
        }
        MethodData md = new MethodData(Functional.map(args, arg -> {
            JavaTypeInstance deGenerifiedType = arg.getDeGenerifiedType();
            return mapping.get(deGenerifiedType);
        }));
        String name = poss.get(md);
        if (name != null) {
            return name;
        }
        /*
         * Ouch.  Can't figure this out.  This is possibly because of a nasty intersection type in the reified code.
         * However, if it can only be one name, we're ok!
         * (Don't do anything as sophisticated as checking type hierarchies etc.)
         */
        for (Map.Entry<MethodData, String> entry : poss.entrySet()) {
            if (entry.getKey().args.size() == md.args.size()) {
                if (name != null) {
                    if (name.equals(entry.getValue())) {
                        continue;
                    }
                    name = null;
                    break;
                }
                name = entry.getValue();
            }
        }
        if (name != null) {
            return name;
        }

        d.addSummaryError(null,"Could not resolve method '" + this.getRealClass().getRawName() + " " + displayName + "'");
        return displayName;
    }

    String getFieldName(String name, JavaTypeInstance type, Dumper d, Mapping mapping, boolean isStatic) {
        String res = getFieldNameOrNull(name, type, d, mapping);
        if (res == null && isStatic) {
            res = getInterfaceFieldNameOrNull(name, type, d, mapping);
        }
        if (res == null) {
            d.addSummaryError(null,"Could not resolve field '" + name + "'");
            return name;
        }
        return res;
    }

    private String getInterfaceFieldNameOrNull(String name, JavaTypeInstance type, Dumper d, Mapping mapping) {
        if (!(type instanceof JavaRefTypeInstance)) return null;
        BindingSuperContainer bindingSupers = type.getBindingSupers();
        for (Map.Entry<JavaRefTypeInstance, BindingSuperContainer.Route> entry :  bindingSupers.getBoundSuperRoute().entrySet()) {
            if (entry.getValue() != BindingSuperContainer.Route.INTERFACE) continue;
            ClassMapping cm = mapping.getClassMapping(entry.getKey().getDeGenerifiedType());
            if (cm == null) continue;
            FieldMapping rename = cm.fieldMappings.get(name);
            if (rename == null) continue;
            return rename.getRename();
        }
        return null;
    }

    private String getFieldNameOrNull(String name, JavaTypeInstance type, Dumper d, Mapping mapping) {
        if (name.endsWith(MiscConstants.DOT_THIS)) {
            String preName = name.substring(0, name.length() - MiscConstants.DOT_THIS.length());
            ObjectSet<JavaTypeInstance> parents = new ObjectLinkedOpenHashSet<>();
            type.getInnerClassHereInfo().collectTransitiveDegenericParents(parents);
            for (JavaTypeInstance parent : parents) {
                if (((JavaRefTypeInstance)parent).getRawShortName().equals(preName)) {
                    JavaRefTypeInstance mappedParent = (JavaRefTypeInstance)mapping.get(parent);
                    if (mappedParent != null) {
                        return mappedParent.getRawShortName() + MiscConstants.DOT_THIS;
                    }
                }
            }
        }
        FieldMapping f = fieldMappings.get(name);
        if (f == null) {

            if (type instanceof JavaRefTypeInstance) {
                /*
                 * Try in bases
                 */
                ClassFile classFile = ((JavaRefTypeInstance) type).getClassFile();
                if (classFile != null) {
                    JavaTypeInstance baseType = classFile.getBaseClassType().getDeGenerifiedType();
                    String res = getClassFieldNameOrNull(name, d, mapping, baseType);
                    return res;
                }
            }
            return null;
        }
        return f.getRename();
    }

    private String getClassFieldNameOrNull(String name, Dumper d, Mapping mapping, JavaTypeInstance baseType) {
        ClassMapping parentCM = mapping.getClassMapping(baseType);
        if (parentCM != null) {
            String res = parentCM.getFieldNameOrNull(name, baseType, d, mapping);
            return res;
        }
        return null;
    }

}
