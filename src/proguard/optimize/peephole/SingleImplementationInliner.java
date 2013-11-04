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
package proguard.optimize.peephole;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.ClassReferenceFixer;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.*;
import proguard.optimize.info.SingleImplementationMarker;

/**
 * This ClassVisitor replaces all references to interfaces that have single
 * implementations by references to those implementations. The names will then
 * have to be fixed, based on the new references.
 *
 * @see SingleImplementationMarker
 * @see SingleImplementationFixer
 * @see ClassReferenceFixer
 * @author Eric Lafortune
 */
public class SingleImplementationInliner
extends      SimplifiedVisitor
implements   ClassVisitor,
             ConstantVisitor,
             MemberVisitor,
             AttributeVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Update the constant pool.
        programClass.constantPoolEntriesAccept(this);

        // Update the class members.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Update the attributes.
        programClass.attributesAccept(this);
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        if (stringConstant.referencedMember == null)
        {
            // Update the referenced class if it is an interface with a single
            // implementation.
            Clazz singleImplementationClass =
                SingleImplementationMarker.singleImplementation(stringConstant.referencedClass);

            if (singleImplementationClass != null)
            {
                stringConstant.referencedClass = singleImplementationClass;
            }
        }
    }


    public void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        // Update the referenced interface if it has a single implementation.
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(interfaceMethodrefConstant.referencedClass);

        if (singleImplementationClass != null)
        {
            // We know the single implementation contains the method.
            String name = interfaceMethodrefConstant.getName(clazz);
            String type = interfaceMethodrefConstant.getType(clazz);

            interfaceMethodrefConstant.referencedClass  = singleImplementationClass;
            interfaceMethodrefConstant.referencedMember = singleImplementationClass.findMethod(name, type);
        }
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Update the referenced class if it is an interface with a single
        // implementation.
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(classConstant.referencedClass);

        if (singleImplementationClass != null)
        {
            classConstant.referencedClass = singleImplementationClass;
        }
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        // Update the referenced class if the type is an interface with a
        // single implementation.
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(programField.referencedClass);

        if (singleImplementationClass != null)
        {
            programField.referencedClass = singleImplementationClass;
        }

        // Update the attributes.
        programField.attributesAccept(programClass, this);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        // Update the referenced classes if the descriptor contains
        // interfaces with single implementations.
        updateReferencedClasses(programMethod.referencedClasses);

        // Update the attributes.
        programMethod.attributesAccept(programClass, this);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Update the referenced classes of the local variables.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Update the referenced classes of the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Update the referenced classes of the local variable types.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        // Update the referenced classes.
        updateReferencedClasses(signatureAttribute.referencedClasses);
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Update the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Update the annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Update the annotation.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        // Update the referenced class if it is an interface with a single
        // implementation.
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(localVariableInfo.referencedClass);

        if (singleImplementationClass != null)
        {
            localVariableInfo.referencedClass = singleImplementationClass;
        }
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        // Update the referenced classes.
        updateReferencedClasses(localVariableTypeInfo.referencedClasses);
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        // Update the referenced classes.
        updateReferencedClasses(annotation.referencedClasses);

        // Update the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        // Update the referenced classes.
        updateReferencedClasses(enumConstantElementValue.referencedClasses);
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        // Update the referenced classes.
        updateReferencedClasses(classElementValue.referencedClasses);
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        // Update the annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        // Update the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }


    // Small utility methods.

    /**
     * Updates the given array of referenced classes, replacing references
     * to a interfaces with single implementations by these implementations.
     */
    private void updateReferencedClasses(Clazz[] referencedClasses)
    {
        // Update all referenced classes.
        if (referencedClasses != null)
        {
            for (int index = 0; index < referencedClasses.length; index++)
            {
                // See if we have is an interface with a single implementation.
                Clazz singleImplementationClass =
                    SingleImplementationMarker.singleImplementation(referencedClasses[index]);

                // Update or copy the referenced class.
                if (singleImplementationClass != null)
                {
                    referencedClasses[index] = singleImplementationClass;
                }
            }
        }
    }
}
