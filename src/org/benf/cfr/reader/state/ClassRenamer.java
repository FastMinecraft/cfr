package org.benf.cfr.reader.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class ClassRenamer {
    private final Map<String, String> classCollisionRenamerToReal = MapFactory.newMap();
    private final Map<String, String> classCollisionRenamerFromReal = MapFactory.newMap();

    private final ObjectList<ClassNameFunction> renamers;

    private ClassRenamer(ObjectList<ClassNameFunction> renamers) {
        this.renamers = renamers;
    }

    public static ClassRenamer create(Options options) {
        ObjectSet<String> invalidNames = OsInfo.OS().getIllegalNames();
        // We still fetch the insensitivity flag from options, to allow it to be forced.
        boolean renameCase = (options.getOption(OptionsImpl.CASE_INSENSITIVE_FS_RENAME));

        ObjectList<ClassNameFunction> functions = new ObjectArrayList<>();
        if (!invalidNames.isEmpty()) {
            functions.add(new ClassNameFunctionInvalid(renameCase, invalidNames));
        }
        if (renameCase) {
            functions.add(new ClassNameFunctionCase());
        }
        if (functions.isEmpty()) {
            return null;
        }
        return new ClassRenamer(functions);
    }

    String getRenamedClass(String name) {
        String res = classCollisionRenamerFromReal.get(name);
        return res == null ? name : res;
    }

    String getOriginalClass(String name) {
        String res = classCollisionRenamerToReal.get(name);
        return res == null ? name : res;
    }

    void notifyClassFiles(Collection<String> names) {
        Map<String, String> originalToXfrm = new Object2ObjectLinkedOpenHashMap<>();
        for (String name : names) {
            originalToXfrm.put(name, name);
        }
        for (ClassNameFunction renamer : renamers) {
            originalToXfrm = renamer.apply(originalToXfrm);
        }
        for (Map.Entry<String, String> entry : originalToXfrm.entrySet()) {
            String original = entry.getKey();
            String rename = entry.getValue();
            if (!original.equals(rename)) {
                classCollisionRenamerFromReal.put(original, rename);
                classCollisionRenamerToReal.put(rename, original);
            }
        }
    }
}
