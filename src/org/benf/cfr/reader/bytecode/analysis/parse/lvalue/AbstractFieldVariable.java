package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFieldRef;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ConfusedCFRException;

import java.util.Objects;

public abstract class AbstractFieldVariable extends AbstractLValue {

    private final ClassFileField classFileField;
    private final String failureName; // if we can't get the classfileField.
    private final JavaTypeInstance owningClass;
    private final String descriptor;

    AbstractFieldVariable(ConstantPoolEntry field) {
        super(getFieldType((ConstantPoolEntryFieldRef) field));
        ConstantPoolEntryFieldRef fieldRef = (ConstantPoolEntryFieldRef) field;
        this.classFileField = getField(fieldRef);
        this.failureName = fieldRef.getLocalName();
        this.owningClass = fieldRef.getClassEntry().getTypeInstance();
        this.descriptor = fieldRef.getNameAndTypeEntry().getDescriptor().getValue();
    }

    AbstractFieldVariable(ClassFileField field, JavaTypeInstance owningClass) {
        super(new InferredJavaType(field.getField().getJavaTypeInstance(), InferredJavaType.Source.UNKNOWN));
        this.classFileField = field;
        this.failureName = field.getFieldName();
        this.owningClass = owningClass;
        this.descriptor = field.getField().getDescriptor();
    }

    AbstractFieldVariable(AbstractFieldVariable other) {
        super(other.getInferredJavaType());
        this.classFileField = other.classFileField;
        this.failureName = other.failureName;
        this.owningClass = other.owningClass;
        this.descriptor = other.descriptor;
    }

    AbstractFieldVariable(InferredJavaType type, JavaTypeInstance clazz, String varName) {
        super(type);
        this.classFileField = null;
        this.owningClass = clazz;
        this.failureName = varName;
        this.descriptor = type.getJavaTypeInstance().getRawName(); // matching only
    }

    AbstractFieldVariable(InferredJavaType type, JavaTypeInstance clazz, ClassFileField classFileField) {
        super(type);
        this.classFileField = classFileField;
        this.owningClass = clazz;
        this.failureName = null;
        this.descriptor = classFileField.getField().getDescriptor();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        super.collectTypeUsages(collector);
        if (classFileField != null) collector.collect(classFileField.getField().getJavaTypeInstance());
        collector.collect(owningClass);
    }

    @Override
    public void markFinal() {

    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isFakeIgnored() {
        return false;
    }

    @Override
    public void markVar() {

    }

    @Override
    public boolean isVar() {
        return false;
    }

    @Override
    public int getNumberOfCreators() {
        throw new ConfusedCFRException("NYI");
    }

    public JavaTypeInstance getOwningClassType() {
        return owningClass;
    }

    public String getFieldName() {
        if (classFileField == null) {
            return failureName;
        }
        return classFileField.getFieldName();
    }

    protected boolean isHiddenDeclaration() {
        if (classFileField == null) {
            return false;
        }
        return classFileField.shouldNotDisplay();
    }

    public String getRawFieldName() {
        if (classFileField == null) {
            return failureName;
        }
        return classFileField.getRawFieldName();
    }

    public ClassFileField getClassFileField() {
        return classFileField;
    }

    public Field getField() {
        return classFileField == null ? null : classFileField.getField();
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        //noinspection unchecked
        return new SSAIdentifiers<>(this, ssaIdentifierFactory);
    }

    @Override
    public void collectLValueAssignments(Expression assignedTo, StatementContainer statementContainer, LValueAssignmentCollector lValueAssigmentCollector) {
    }


    public static ClassFileField getField(ConstantPoolEntryFieldRef fieldRef) {
        String name = fieldRef.getLocalName();
        JavaRefTypeInstance ref = (JavaRefTypeInstance) fieldRef.getClassEntry().getTypeInstance();
        try {
            ClassFile classFile = ref.getClassFile();
            if (classFile == null) return null;

            return classFile.getFieldByName(name, fieldRef.getJavaTypeInstance());
        } catch (NoSuchFieldException | CannotLoadClassException ignore) {
        }
        return null;
    }


    private static InferredJavaType getFieldType(ConstantPoolEntryFieldRef fieldRef) {
        String name = fieldRef.getLocalName();
        JavaRefTypeInstance ref = (JavaRefTypeInstance) fieldRef.getClassEntry().getTypeInstance();
        try {
            ClassFile classFile = ref.getClassFile();
            if (classFile != null) {
                // this now seems rather pointless, as it's passing the type to GET the type!
                Field field = classFile.getFieldByName(name, fieldRef.getJavaTypeInstance()).getField();
                return new InferredJavaType(field.getJavaTypeInstance(), InferredJavaType.Source.FIELD, true);
            }
        } catch (CannotLoadClassException | NoSuchFieldException ignore) {
        }
        return new InferredJavaType(fieldRef.getJavaTypeInstance(), InferredJavaType.Source.FIELD, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractFieldVariable that)) return false;

        if (!getFieldName().equals(that.getFieldName())) return false;
        return Objects.equals(owningClass, that.owningClass);
    }

    @Override
    public int hashCode() {
        int result = getFieldName().hashCode();
        result = 31 * result + (owningClass != null ? owningClass.hashCode() : 0);
        return result;
    }
}
