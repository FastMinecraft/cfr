package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ClassNameFunctionInvalid implements ClassNameFunction {
    private final ObjectSet<String> illegalNames;

    ClassNameFunctionInvalid(boolean caseInsensitive, ObjectSet<String> illegalNames) {
        if (caseInsensitive) {
            ObjectSet<String> ciNames = new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER);
            ciNames.addAll(illegalNames);
            illegalNames = ciNames;
        }
        this.illegalNames = illegalNames;
    }

    @Override
    public Map<String, String> apply(Map<String, String> names) {
        // Any class names which would create files in the illegal set need to be renamed.
        Map<String, String> res = new Object2ObjectLinkedOpenHashMap<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String val = entry.getValue();
            if (illegalName(val)) {
                val = val.substring(0, val.length()-6) + "_.class";
            }
            res.put(entry.getKey(), val);
        }
        return res;
    }

    private boolean illegalName(String path) {
        String stripClass = path.substring(0, path.length() - 6);
        int idx = stripClass.lastIndexOf("/");
        if (idx != -1) {
            stripClass = stripClass.substring(idx+1);
        }
        return illegalNames.contains(stripClass);
    }
}
