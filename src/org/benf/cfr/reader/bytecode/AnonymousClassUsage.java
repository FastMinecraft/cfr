package org.benf.cfr.reader.bytecode;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;

import it.unimi.dsi.fastutil.objects.ObjectList;

/*
 * Usage of anonymous classes currently requires decorating those classes once we've determined the code that's
 * using them - i.e. mutating state. We therefore have to extract this information so we don't perform it multiple
 * times (inside a recovery).
 */
public class AnonymousClassUsage {
    private final ObjectList<Pair<ClassFile, ConstructorInvokationAnonymousInner>> noted = new ObjectArrayList<>();
    private final ObjectList<Pair<ClassFile, ConstructorInvokationSimple>> localNoted = new ObjectArrayList<>();

    public void note(ClassFile classFile, ConstructorInvokationAnonymousInner constructorInvokationAnonymousInner) {
        noted.add(Pair.make(classFile, constructorInvokationAnonymousInner));
    }

    public void noteMethodClass(ClassFile classFile, ConstructorInvokationSimple constructorInvokation) {
        localNoted.add(Pair.make(classFile, constructorInvokation));
    }

    public boolean isEmpty() {
        return noted.isEmpty() && localNoted.isEmpty();
    }

    void useNotes() {
        for (Pair<ClassFile, ConstructorInvokationAnonymousInner> note : noted) {
            note.getFirst().noteAnonymousUse(note.getSecond());
        }
        for (Pair<ClassFile, ConstructorInvokationSimple> note : localNoted) {
            note.getFirst().noteMethodUse(note.getSecond());
        }
    }
}
