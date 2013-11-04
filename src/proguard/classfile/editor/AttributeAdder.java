/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2008 Eric Lafortune (eric@graphics.cornell.edu)
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
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This AttributeVisitor adds all attributes that it visits to the given
 * target class, class member, or attribute.
 *
 * @author Eric Lafortune
 */
public class AttributeAdder
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    private static final byte[]          EMPTY_BYTES       = new byte[0];
    private static final int[]           EMPTY_INTS        = new int[0];
    private static final Attribute[]     EMPTY_ATTRIBUTES  = new Attribute[0];
    private static final ExceptionInfo[] EMPTY_EXCEPTIONS  = new ExceptionInfo[0];


    private final ProgramClass  targetClass;
    private final ProgramMember targetMember;
    private final CodeAttribute targetCodeAttribute;
    private final boolean       replaceAttributes;

    private final ConstantAdder    constantAdder;
    private final AttributesEditor attributesEditor;


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target class.
     */
    public AttributeAdder(ProgramClass targetClass,
                          boolean      replaceAttributes)
    {
        this(targetClass, null, null, replaceAttributes);
    }


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target class member.
     */
    public AttributeAdder(ProgramClass  targetClass,
                          ProgramMember targetMember,
                          boolean       replaceAttributes)
    {
        this(targetClass, targetMember, null, replaceAttributes);
    }


    /**
     * Creates a new AttributeAdder that will copy attributes into the given
     * target attribute.
     */
    public AttributeAdder(ProgramClass  targetClass,
                          ProgramMember targetMember,
                          CodeAttribute targetCodeAttribute,
                          boolean       replaceAttributes)
    {
        this.targetClass         = targetClass;
        this.targetMember        = targetMember;
        this.targetCodeAttribute = targetCodeAttribute;
        this.replaceAttributes   = replaceAttributes;

        constantAdder    = new ConstantAdder(targetClass);
        attributesEditor = new AttributesEditor(targetClass,
                                                targetMember,
                                                targetCodeAttribute,
                                                replaceAttributes);
    }


    // Implementations for AttributeVisitor.

    public void visitUnknownAttribute(Clazz clazz, UnknownAttribute unknownAttribute)
    {
        // Create a copy of the attribute.
        UnknownAttribute newUnknownAttribute =
            new UnknownAttribute(constantAdder.addConstant(clazz, unknownAttribute.u2attributeNameIndex),
                                 unknownAttribute.u4attributeLength,
                                 unknownAttribute.info);

        // Add it to the target class.
        attributesEditor.addAttribute(newUnknownAttribute);
    }


    public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute)
    {
        // Create a copy of the attribute.
        SourceFileAttribute newSourceFileAttribute =
            new SourceFileAttribute(constantAdder.addConstant(clazz, sourceFileAttribute.u2attributeNameIndex),
                                    constantAdder.addConstant(clazz, sourceFileAttribute.u2sourceFileIndex));

        // Add it to the target class.
        attributesEditor.addAttribute(newSourceFileAttribute);
    }


    public void visitSourceDirAttribute(Clazz clazz, SourceDirAttribute sourceDirAttribute)
    {
        // Create a copy of the attribute.
        SourceDirAttribute newSourceDirAttribute =
            new SourceDirAttribute(constantAdder.addConstant(clazz, sourceDirAttribute.u2attributeNameIndex),
                                   constantAdder.addConstant(clazz, sourceDirAttribute.u2sourceDirIndex));

        // Add it to the target class.
        attributesEditor.addAttribute(newSourceDirAttribute);
    }


    public void visitInnerClassesAttribute(Clazz clazz, InnerClassesAttribute innerClassesAttribute)
    {
        // TODO: Implement method.
        // Note that the attribute may already be present.
//        // Create a copy of the attribute.
//        InnerClassesAttribute newInnerClassesAttribute =
//            new InnerClassesAttribute(constantAdder.addConstant(clazz, innerClassesAttribute.u2attributeNameIndex),
//                                      0,
//                                      null);
//
//        // Add it to the target class.
//        attributesEditor.addClassAttribute(newInnerClassesAttribute);
    }


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        // Create a copy of the attribute.
        EnclosingMethodAttribute newEnclosingMethodAttribute =
            new EnclosingMethodAttribute(constantAdder.addConstant(clazz, enclosingMethodAttribute.u2attributeNameIndex),
                                         constantAdder.addConstant(clazz, enclosingMethodAttribute.u2classIndex),
                                         enclosingMethodAttribute.u2nameAndTypeIndex == 0 ? 0 :
                                         constantAdder.addConstant(clazz, enclosingMethodAttribute.u2nameAndTypeIndex));

        newEnclosingMethodAttribute.referencedClass  = enclosingMethodAttribute.referencedClass;
        newEnclosingMethodAttribute.referencedMethod = enclosingMethodAttribute.referencedMethod;

        // Add it to the target class.
        attributesEditor.addAttribute(newEnclosingMethodAttribute);
    }


    public void visitDeprecatedAttribute(Clazz clazz, DeprecatedAttribute deprecatedAttribute)
    {
        // Create a copy of the attribute.
        DeprecatedAttribute newDeprecatedAttribute =
            new DeprecatedAttribute(constantAdder.addConstant(clazz, deprecatedAttribute.u2attributeNameIndex));

        // Add it to the target.
        attributesEditor.addAttribute(newDeprecatedAttribute);
    }


    public void visitSyntheticAttribute(Clazz clazz, SyntheticAttribute syntheticAttribute)
    {
        // Create a copy of the attribute.
        SyntheticAttribute newSyntheticAttribute =
            new SyntheticAttribute(constantAdder.addConstant(clazz, syntheticAttribute.u2attributeNameIndex));

        // Add it to the target.
        attributesEditor.addAttribute(newSyntheticAttribute);
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        // Create a copy of the attribute.
        SignatureAttribute newSignatureAttribute =
            new SignatureAttribute(constantAdder.addConstant(clazz, signatureAttribute.u2attributeNameIndex),
                                   constantAdder.addConstant(clazz, signatureAttribute.u2signatureIndex));

        newSignatureAttribute.referencedClasses = signatureAttribute.referencedClasses;

        // Add it to the target.
        attributesEditor.addAttribute(newSignatureAttribute);
    }


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        // Create a copy of the attribute.
        ConstantValueAttribute newConstantValueAttribute =
            new ConstantValueAttribute(constantAdder.addConstant(clazz, constantValueAttribute.u2attributeNameIndex),
                                       constantAdder.addConstant(clazz, constantValueAttribute.u2constantValueIndex));

        // Add it to the target field.
        attributesEditor.addAttribute(newConstantValueAttribute);
    }


    public void visitExceptionsAttribute(Clazz clazz, Method method, ExceptionsAttribute exceptionsAttribute)
    {
        // Create a new exceptions attribute.
        ExceptionsAttribute newExceptionsAttribute =
            new ExceptionsAttribute(constantAdder.addConstant(clazz, exceptionsAttribute.u2attributeNameIndex),
                                    0,
                                    exceptionsAttribute.u2exceptionIndexTableLength > 0 ?
                                        new int[exceptionsAttribute.u2exceptionIndexTableLength] :
                                        EMPTY_INTS);

        // Add the exceptions.
        exceptionsAttribute.exceptionEntriesAccept((ProgramClass)clazz,
                                                   new ExceptionAdder(targetClass,
                                                                      newExceptionsAttribute));

        // Add it to the target method.
        attributesEditor.addAttribute(newExceptionsAttribute);
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Create a new code attribute.
        CodeAttribute newCodeAttribute =
            new CodeAttribute(constantAdder.addConstant(clazz, codeAttribute.u2attributeNameIndex),
                              codeAttribute.u2maxStack,
                              codeAttribute.u2maxLocals,
                              0,
                              EMPTY_BYTES,
                              0,
                              codeAttribute.u2exceptionTableLength > 0 ?
                                  new ExceptionInfo[codeAttribute.u2exceptionTableLength] :
                                  EMPTY_EXCEPTIONS,
                              0,
                              codeAttribute.u2attributesCount > 0 ?
                                  new Attribute[codeAttribute.u2attributesCount] :
                                  EMPTY_ATTRIBUTES);

        CodeAttributeComposer codeAttributeComposer = new CodeAttributeComposer();

        codeAttributeComposer.beginCodeFragment(codeAttribute.u4codeLength + 32);

        // Add the instructions.
        codeAttribute.instructionsAccept(clazz,
                                         method,
                                         new InstructionAdder(targetClass,
                                                              codeAttributeComposer));
        // Add the exceptions.
        codeAttribute.exceptionsAccept(clazz,
                                       method,
                                       new ExceptionInfoAdder(targetClass,
                                                              codeAttributeComposer));

        codeAttributeComposer.endCodeFragment();

        // Add the attributes.
        codeAttribute.attributesAccept(clazz,
                                       method,
                                       new AttributeAdder(targetClass,
                                                          targetMember,
                                                          newCodeAttribute,
                                                          replaceAttributes));

        // Apply these changes to the new code attribute.
        codeAttributeComposer.visitCodeAttribute(targetClass,
                                                 (Method)targetMember,
                                                 newCodeAttribute);

        // Add the completed code attribute to the target method.
        attributesEditor.addAttribute(newCodeAttribute);
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
        // Create a new annotations attribute.
        RuntimeVisibleAnnotationsAttribute newAnnotationsAttribute =
            new RuntimeVisibleAnnotationsAttribute(constantAdder.addConstant(clazz, runtimeVisibleAnnotationsAttribute.u2attributeNameIndex),
                                                   0,
                                                   new Annotation[runtimeVisibleAnnotationsAttribute.u2annotationsCount]);

        // Add the annotations.
        runtimeVisibleAnnotationsAttribute.annotationsAccept(clazz,
                                                             new AnnotationAdder(targetClass,
                                                                                 newAnnotationsAttribute));

        // Add it to the target.
        attributesEditor.addAttribute(newAnnotationsAttribute);
    }


    public void visitRuntimeInvisibleAnnotationsAttribute(Clazz clazz, RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute)
    {
        // Create a new annotations attribute.
        RuntimeInvisibleAnnotationsAttribute newAnnotationsAttribute =
            new RuntimeInvisibleAnnotationsAttribute(constantAdder.addConstant(clazz, runtimeInvisibleAnnotationsAttribute.u2attributeNameIndex),
                                                     0,
                                                     new Annotation[runtimeInvisibleAnnotationsAttribute.u2annotationsCount]);

        // Add the annotations.
        runtimeInvisibleAnnotationsAttribute.annotationsAccept(clazz,
                                                               new AnnotationAdder(targetClass,
                                                                                   newAnnotationsAttribute));

        // Add it to the target.
        attributesEditor.addAttribute(newAnnotationsAttribute);
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
        // Create a new annotation default attribute.
        AnnotationDefaultAttribute newAnnotationDefaultAttribute =
            new AnnotationDefaultAttribute(constantAdder.addConstant(clazz, annotationDefaultAttribute.u2attributeNameIndex),
                                           null);

        // Add the annotations.
        annotationDefaultAttribute.defaultValueAccept(clazz,
                                                      new ElementValueAdder(targetClass,
                                                                            newAnnotationDefaultAttribute,
                                                                            false));

        // Add it to the target.
        attributesEditor.addAttribute(newAnnotationDefaultAttribute);
    }
}
