/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.preverification.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This AttributeVisitor adds all attributes that it visits to the given
 * target class, class member, or attribute.
 *
 * @author Eric Lafortune
 */
public class AttributeAdder
implements   AttributeVisitor
{
    private static final int[] EMPTY_INTS = new int[0];


    private final ProgramClass  targetClass;
    private final ProgramMember targetMember;
    private final Attribute     targetAttribute;

    private final ConstantAdder    constantAdder    = new ConstantAdder();
    private final AttributesEditor attributesEditor = new AttributesEditor();


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target class.
     */
    public AttributeAdder(ProgramClass targetClass)
    {
        this(targetClass, null, null);
    }


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target class member.
     */
    public AttributeAdder(ProgramClass  targetClass,
                          ProgramMember targetMember)
    {
        this(targetClass, targetMember, null);
    }


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target attribute.
     */
    public AttributeAdder(ProgramClass  targetClass,
                          ProgramMember targetMember,
                          Attribute     targetAttribute)
    {
        this.targetClass     = targetClass;
        this.targetMember    = targetMember;
        this.targetAttribute = targetAttribute;

        constantAdder.setTargetClass(targetClass);
    }


    // Implementations for AttibuteVisitor.

    public void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        // TODO: Implement method.
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        // TODO: Implement method.
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        // TODO: Implement method.
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        // TODO: Implement method.
    }


    public void visitDeprecatedAttribute(Clazz clazz, Field field, DeprecatedAttribute deprecatedAttribute)
    {
        // TODO: Implement method.
    }


    public void visitDeprecatedAttribute(Clazz clazz, Method method, DeprecatedAttribute deprecatedAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSyntheticAttribute(Clazz clazz, Field field, SyntheticAttribute syntheticAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSyntheticAttribute(Clazz clazz, Method method, SyntheticAttribute syntheticAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSignatureAttribute(Clazz clazz, Field field, SignatureAttribute signatureAttribute)
    {
        // TODO: Implement method.
    }


    public void visitSignatureAttribute(Clazz clazz, Method method, SignatureAttribute signatureAttribute)
    {
        // TODO: Implement method.
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        // TODO: Implement method.
    }


    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        // Create a new exceptions attribute.
        ExceptionsAttribute newExceptionsAttribute = new ExceptionsAttribute();

        // Make sure the name is set in the constant pool.
        clazz.constantPoolEntryAccept(exceptionsAttribute.u2attributeNameIndex,
                                      constantAdder);

        newExceptionsAttribute.u2attributeNameIndex = constantAdder.getConstantIndex();

        // Start with an empty exception index table.
        newExceptionsAttribute.u2exceptionIndexTableLength = 0;
        newExceptionsAttribute.u2exceptionIndexTable       = EMPTY_INTS;

        // Add the exceptions.
        exceptionsAttribute.exceptionEntriesAccept((ProgramClass)clazz,
                                                   new ExceptionAdder(targetClass,
                                                                      newExceptionsAttribute));

        // Add the completed exceptions attribute.
        attributesEditor.addAttribute(targetClass,
                                      (ProgramMethod)targetMember,
                                      newExceptionsAttribute);
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // TODO: Implement method.
    }


    public void visitStackMapAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapAttribute stackMapAttribute)
    {
        // TODO: Implement method.
    }


    public void visitStackMapTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, StackMapTableAttribute stackMapTableAttribute)
    {
        // TODO: Implement method.
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        // TODO: Implement method.
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // TODO: Implement method.
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeVisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Field field, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeVisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitRuntimeInvisibleParameterAnnotationsAttribute(Clazz clazz, Method method, RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute)
    {
        // TODO: Implement method.
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // TODO: Implement method.
    }
}
