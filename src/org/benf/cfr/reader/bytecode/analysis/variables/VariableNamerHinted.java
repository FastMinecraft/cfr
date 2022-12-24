package org.benf.cfr.reader.bytecode.analysis.variables;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.attributes.LocalVariableEntry;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableNamerHinted implements VariableNamer {

    private final VariableNamer missingNamer = new VariableNamerDefault();

    private final OrderLocalVariables orderLocalVariable = new OrderLocalVariables();
    private final Map<Integer, TreeSet<LocalVariableEntry>> localVariableEntryTreeSet =
            MapFactory.newLazyMap(arg -> new TreeSet<>(orderLocalVariable));
    //    private final Map<Pair<LocalVariableEntry, Ident>, NamedVariable> cache = MapFactory.newMap();
    private final Map<LocalVariableEntry, NamedVariable> cache = MapFactory.newMap();

    private final ConstantPool cp;

    VariableNamerHinted(ObjectList<LocalVariableEntry> entryList, ConstantPool cp) {
        for (LocalVariableEntry e : entryList) {
            localVariableEntryTreeSet.get(e.index()).add(e);
        }
        this.cp = cp;
    }

    @Override
    public NamedVariable getName(int originalRawOffset, Ident ident, long stackPosition, boolean clashed) {
        // Slightly crappy heuristic for dealing with slight fibbing in offsets by compilers.
        // clamp 0 to 0 to handle empty functions, as that is not incorrectly reported.
        originalRawOffset = originalRawOffset > 0 ? originalRawOffset + 2 : 0;
        int sstackPos = (int) stackPosition;
        if (clashed || !localVariableEntryTreeSet.containsKey(sstackPos)) {
            return missingNamer.getName(originalRawOffset, ident, sstackPos, clashed);
        }
        LocalVariableEntry tmp = new LocalVariableEntry(originalRawOffset, (short) 1, (short) -1, (short) -1, (short) stackPosition);
        TreeSet<LocalVariableEntry> lveSet = localVariableEntryTreeSet.get(sstackPos);
        LocalVariableEntry lve = lveSet.floor(tmp);

        // We'd expect that we could just do a range test, not check start and falling off end.
        // See ScopeTest18 for counterexample.
        if (lve == null || originalRawOffset > lve.getEndPc() && null == lveSet.ceiling(tmp)) {
            return missingNamer.getName(originalRawOffset, ident, sstackPos, clashed);
        }

        NamedVariable namedVariable = cache.get(lve);
        if (namedVariable == null) {
            String name = cp.getUTF8Entry(lve.nameIndex()).getValue();
            if (IllegalIdentifierReplacement.isIllegal(name)) {
                namedVariable = new NamedVariableDefault(name);
                // This is a bit of a hack - we bless the 'this' constant
                // if used in a legit location, however we should also track if this
                // is an instance method.
                if (name.equals(MiscConstants.THIS) && ident.getIdx() == 0) {
                    namedVariable.forceName(MiscConstants.THIS);
                }
            } else {
                int genIdx = 0;
                namedVariable = new NamedVariableFromHint(name, lve.index(), genIdx);
            }
            cache.put(lve, namedVariable);
        }
        return namedVariable;
    }

    private static class OrderLocalVariables implements Comparator<LocalVariableEntry> {
        @Override
        public int compare(LocalVariableEntry a, LocalVariableEntry b) {
            int x = a.index() - b.index();
            if (x != 0) return x;
            return a.startPc() - b.startPc();
        }
    }


    @Override
    public ObjectList<NamedVariable> getNamedVariables() {
        return new ObjectArrayList<>(cache.values());
    }

    @Override
    public void forceName(Ident ident, long stackPosition, String name) {
        missingNamer.forceName(ident, stackPosition, name);
    }

    @Override
    public void mutatingRenameUnClash(NamedVariable toRename) {
        Map<String, NamedVariable> namedVariableMap = MapFactory.newMap();
        for (NamedVariable var : cache.values()) {
            namedVariableMap.put(var.getStringName(), var);
        }
        for (NamedVariable var : missingNamer.getNamedVariables()) {
            namedVariableMap.put(var.getStringName(), var);
        }

        String name = toRename.getStringName();
        Pattern p = Pattern.compile("^(.*[^\\d]+)([\\d]+)$");
        Matcher m = p.matcher(name);
        int start = 2;
        String prefix = name;
        if (m.matches()) {
            prefix = m.group(1);
            String numPart = m.group(2);
            start = Integer.parseInt(numPart);
            start++;
        }
        do {
            String name2 = prefix + start;
            if (!namedVariableMap.containsKey(name2)) {
                toRename.forceName(name2);
                return;
            }
            start++;
        } while (true);
    }

}
