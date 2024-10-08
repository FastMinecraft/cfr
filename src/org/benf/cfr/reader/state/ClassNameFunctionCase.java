package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ClassNameFunctionCase implements ClassNameFunction {
    @Override
    public Map<String, String> apply(Map<String, String> names) {
        ObjectSet<String> caseInTest = new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> applied = new Object2ObjectLinkedOpenHashMap<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String original = entry.getKey();
            String used = entry.getValue();
            if (!caseInTest.add(used)) {
                used = deDup(used, caseInTest);
            }
            applied.put(original, used);
        }
        return applied;
    }

    private static String deDup(String potDup, ObjectSet<String> caseInTest) {
        String n = potDup.toLowerCase();
        String name = n.substring(0, n.length()-6);
        int next = 0;
        if (!caseInTest.contains(n)) return potDup;
        String testName = name + "_" + next + ".class";
        while (caseInTest.contains(testName)) {
            testName = name + "_" + ++next + ".class";
        }
        caseInTest.add(testName);
        return testName;
    }
}
