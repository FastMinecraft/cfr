package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.VariableNameTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassCache;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * We may have deep inner classes, with references to each other.
 * <p/>
 * So
 * <p/>
 * this.Inner2.this.Inner1.this
 * <p/>
 * But this is illegal.  So remove the outer one, leaving
 * <p/>
 * this.Inner1.this (the LHS this is still illegal, but will be removed later).
 */
public class ScopeHidingVariableRewriter implements Op04Rewriter {

    private final Method method;
    private final ClassCache classCache;

    private final ObjectSet<String> outerNames = new ObjectOpenHashSet<>();
    private final ObjectSet<String> usedNames = new ObjectOpenHashSet<>();
    /*
     * Collect collisions in a first pass, so that we can avoid uneccesarily
     */
    private final ObjectList<LocalVariable> collisions = new ObjectArrayList<>();

    public ScopeHidingVariableRewriter(ObjectList<ClassFileField> fieldVariables, Method method, ClassCache classCache) {
        this.method = method;
        this.classCache = classCache;
        MethodPrototype prototype = method.getMethodPrototype();
        for (ClassFileField field : fieldVariables) {
            String fieldName = field.getFieldName();
            outerNames.add(fieldName);
            usedNames.add(fieldName);
        }
        if (prototype.parametersComputed()) {
            for (LocalVariable localVariable : prototype.getComputedParameters()) {
                checkCollision(localVariable);
            }
        }
    }

    private void checkCollision(LocalVariable localVariable) {
        String name = localVariable.getName().getStringName();
        if (outerNames.contains(name)) collisions.add(localVariable);
        usedNames.add(name);
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        ObjectList<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        for (StructuredStatement definition : structuredStatements) {
            ObjectList<LValue> createdHere = definition.findCreatedHere();
            if (createdHere == null) continue;

            for (LValue lValue : createdHere) {
                if (lValue instanceof LocalVariable) {
                    checkCollision((LocalVariable) lValue);
                }
            }
        }

        if (collisions.isEmpty()) return;

        VariableNameTidier variableNameTidier = new VariableNameTidier(method, classCache);
        variableNameTidier.renameToAvoidHiding(usedNames, collisions);
    }


}
