/* $Id: ClassFileShrinker.java,v 1.11 2003/02/09 15:22:29 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.shrink;

import proguard.classfile.*;
import proguard.classfile.instruction.*;
import proguard.classfile.visitor.*;


/**
 * This ClassFileVisitor removes constant pool entries and class members
 * that are not marked as being used.
 *
 * @see UsageMarker
 *
 * @author Eric Lafortune
 */
public class ClassFileShrinker
  implements ClassFileVisitor,
             CpInfoVisitor,
             MemberInfoVisitor,
             AttrInfoVisitor,
             InstructionVisitor,
             InnerClassesInfoVisitor,
             ExceptionInfoVisitor,
             LocalVariableInfoVisitor
{
    private int[] cpIndexMap;


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // First shrink the arrays for constant pool, interfaces, fields,
        // methods, and class attributes.

        programClassFile.u2interfacesCount =
            shrinkCpIndexArray(programClassFile.constantPool,
                               programClassFile.u2interfaces,
                               programClassFile.u2interfacesCount);

        // Shrinking the constant pool also sets up an index map.
        programClassFile.u2constantPoolCount =
            shrinkConstantPool(programClassFile.constantPool,
                               programClassFile.u2constantPoolCount);

        programClassFile.u2fieldsCount =
            shrinkArray(programClassFile.fields,
                        programClassFile.u2fieldsCount);

        programClassFile.u2methodsCount =
            shrinkArray(programClassFile.methods,
                        programClassFile.u2methodsCount);

        programClassFile.u2attributesCount =
            shrinkArray(programClassFile.attributes,
                        programClassFile.u2attributesCount);

        // Remap the local constant pool references.
        programClassFile.u2thisClass  = remapCpIndex(programClassFile.u2thisClass);
        programClassFile.u2superClass = remapCpIndex(programClassFile.u2superClass);

        remapCpIndexArray(programClassFile.u2interfaces,
                          programClassFile.u2interfacesCount);

        // Remap the references of the remaining contant pool entries.
        programClassFile.constantPoolEntriesAccept(this);

        // Compact the remaining fields, methods, and attributes,
        // and remap their references to the constant pool.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);
        programClassFile.attributesAccept(this);

        // Compact the extra field pointing to the subclasses of this class.
        programClassFile.subClasses =
            shrinkToNewArray(programClassFile.subClasses);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Library class files are left unchanged.

        // Compact the extra field pointing to the subclasses of this class.
        libraryClassFile.subClasses =
            shrinkToNewArray(libraryClassFile.subClasses);
    }


    // Implementations for CpInfoVisitor

    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        classCpInfo.u2nameIndex =
            remapCpIndex(classCpInfo.u2nameIndex);
    }


    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo)
    {
        // Nothing to do.
    }


    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo)
    {
        fieldrefCpInfo.u2classIndex =
            remapCpIndex(fieldrefCpInfo.u2classIndex);
        fieldrefCpInfo.u2nameAndTypeIndex =
            remapCpIndex(fieldrefCpInfo.u2nameAndTypeIndex);
    }


    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo)
    {
        // Nothing to do.
    }


    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo)
    {
        // Nothing to do.
    }


    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo)
    {
        interfaceMethodrefCpInfo.u2classIndex =
            remapCpIndex(interfaceMethodrefCpInfo.u2classIndex);
        interfaceMethodrefCpInfo.u2nameAndTypeIndex =
            remapCpIndex(interfaceMethodrefCpInfo.u2nameAndTypeIndex);
    }


    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo)
    {
        // Nothing to do.
    }


    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        methodrefCpInfo.u2classIndex =
            remapCpIndex(methodrefCpInfo.u2classIndex);
        methodrefCpInfo.u2nameAndTypeIndex =
            remapCpIndex(methodrefCpInfo.u2nameAndTypeIndex);
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        nameAndTypeCpInfo.u2nameIndex =
            remapCpIndex(nameAndTypeCpInfo.u2nameIndex);
        nameAndTypeCpInfo.u2descriptorIndex =
            remapCpIndex(nameAndTypeCpInfo.u2descriptorIndex);
    }


    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
    {
        stringCpInfo.u2stringIndex =
            remapCpIndex(stringCpInfo.u2stringIndex);
    }


    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo)
    {
        // Nothing to do.
    }


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
    {
        visitMemberInfo(programClassFile, programFieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMemberInfo(programClassFile, programMethodInfo);
    }


    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        // Remap the local constant pool references.
        programMemberInfo.u2nameIndex =
            remapCpIndex(programMemberInfo.u2nameIndex);
        programMemberInfo.u2descriptorIndex =
            remapCpIndex(programMemberInfo.u2descriptorIndex);

        // Compact the attributes array.
        programMemberInfo.u2attributesCount =
            shrinkArray(programMemberInfo.attributes,
                        programMemberInfo.u2attributesCount);

        // Compact any attributes and remap the constant pool references of
        // the remaining attributes.
        programMemberInfo.attributesAccept(programClassFile, this);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        // Library class files are left unchanged.
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        // Library class files are left unchanged.
    }


    // Implementations for AttrInfoVisitor

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo)
    {
        unknownAttrInfo.u2attrNameIndex =
            remapCpIndex(unknownAttrInfo.u2attrNameIndex);

        // There's not much else we can do with unknown attributes.
    }


    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo)
    {
        innerClassesAttrInfo.u2attrNameIndex =
            remapCpIndex(innerClassesAttrInfo.u2attrNameIndex);

        // Compact the array of InnerClassesInfo objects.
        innerClassesAttrInfo.u2numberOfClasses =
            shrinkArray(innerClassesAttrInfo.classes,
                        innerClassesAttrInfo.u2numberOfClasses);

        // Remap the constant pool references of inner classes.
        innerClassesAttrInfo.innerClassEntriesAccept(classFile, this);
    }


    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo)
    {
        constantValueAttrInfo.u2attrNameIndex =
            remapCpIndex(constantValueAttrInfo.u2attrNameIndex);
        constantValueAttrInfo.u2constantValueIndex =
            remapCpIndex(constantValueAttrInfo.u2constantValueIndex);
    }


    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo)
    {
        exceptionsAttrInfo.u2attrNameIndex =
            remapCpIndex(exceptionsAttrInfo.u2attrNameIndex);

        // Remap the exception constant pool references.
        remapCpIndexArray(exceptionsAttrInfo.u2exceptionIndexTable,
                          exceptionsAttrInfo.u2numberOfExceptions);
    }


    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        codeAttrInfo.u2attrNameIndex =
            remapCpIndex(codeAttrInfo.u2attrNameIndex);

        // Compact the attributes array.
        codeAttrInfo.u2attributesCount =
            shrinkArray(codeAttrInfo.attributes,
                        codeAttrInfo.u2attributesCount);

        // Remap the constant pool references of the instructions, exceptions,
        // and attributes.
        codeAttrInfo.instructionsAccept(classFile, this);
        codeAttrInfo.exceptionsAccept(classFile, this);
        codeAttrInfo.attributesAccept(classFile, this);
    }


    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo)
    {
        lineNumberTableAttrInfo.u2attrNameIndex =
            remapCpIndex(lineNumberTableAttrInfo.u2attrNameIndex);
    }


    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo)
    {
        localVariableTableAttrInfo.u2attrNameIndex =
            remapCpIndex(localVariableTableAttrInfo.u2attrNameIndex);

        // Remap the constant pool references of local variables.
        localVariableTableAttrInfo.localVariablesAccept(classFile, this);
    }


    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo)
    {
        sourceFileAttrInfo.u2attrNameIndex =
            remapCpIndex(sourceFileAttrInfo.u2attrNameIndex);
        sourceFileAttrInfo.u2sourceFileIndex =
            remapCpIndex(sourceFileAttrInfo.u2sourceFileIndex);
    }


    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo)
    {
        deprecatedAttrInfo.u2attrNameIndex =
            remapCpIndex(deprecatedAttrInfo.u2attrNameIndex);
    }


    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo)
    {
        syntheticAttrInfo.u2attrNameIndex =
            remapCpIndex(syntheticAttrInfo.u2attrNameIndex);
    }


    // Implementations for InstructionVisitor

    public void visitInstruction(ClassFile classFile, Instruction instruction)
    {
        // Nothing to do.
    }


    public void visitCpInstruction(ClassFile classFile, CpInstruction cpInstruction)
    {
        cpInstruction.setCpIndex(remapCpIndex(cpInstruction.getCpIndex()));
    }


    // Implementations for InnerClassesInfoVisitor

    public void visitInnerClassesInfo(ClassFile classFile, InnerClassesInfo innerClassesInfo)
    {
        if (innerClassesInfo.u2innerClassInfoIndex != 0)
        {
            innerClassesInfo.u2innerClassInfoIndex =
                remapCpIndex(innerClassesInfo.u2innerClassInfoIndex);
        }

        if (innerClassesInfo.u2outerClassInfoIndex != 0)
        {
            innerClassesInfo.u2outerClassInfoIndex =
                remapCpIndex(innerClassesInfo.u2outerClassInfoIndex);
        }

        if (innerClassesInfo.u2innerNameIndex != 0)
        {
            innerClassesInfo.u2innerNameIndex =
                remapCpIndex(innerClassesInfo.u2innerNameIndex);
        }
    }


    // Implementations for ExceptionInfoVisitor

    public void visitExceptionInfo(ClassFile classFile, ExceptionInfo exceptionInfo)
    {
        if (exceptionInfo.u2catchType != 0)
        {
            exceptionInfo.u2catchType =
                remapCpIndex(exceptionInfo.u2catchType);
        }
    }


    // Implementations for LocalVariableInfoVisitor

    public void visitLocalVariableInfo(ClassFile classFile, LocalVariableInfo localVariableInfo)
    {
        localVariableInfo.u2nameIndex =
            remapCpIndex(localVariableInfo.u2nameIndex);
        localVariableInfo.u2descriptorIndex =
            remapCpIndex(localVariableInfo.u2descriptorIndex);
    }


    // Small utility methods.

    /**
     * Removes all entries that are not marked as being used from the given
     * constant pool.
     * @return the new number of entries.
     */
    private int shrinkConstantPool(CpInfo[] contantPool, int length)
    {
        if (cpIndexMap == null ||
            cpIndexMap.length < length)
        {
            cpIndexMap = new int[length];
        }

        int     counter        = 0;
        boolean isPreviousUsed = true;

        // Shift the used constant pool entries together.
        for (int index = 0; index < length; index++)
        {
            cpIndexMap[index] = counter;

            CpInfo cpInfo = contantPool[index];

            // Check whether it is the second part of a used entry,
            // or a used entry on its own.
            if (cpInfo == null ? isPreviousUsed : UsageMarker.isUsed(cpInfo))
            {
                contantPool[counter++] = cpInfo;
                isPreviousUsed = true;
            }
            else
            {
                isPreviousUsed = false;
            }
        }

        // Clear the remaining constant pool elements.
        for (int index = counter; index < length; index++)
        {
            contantPool[index] = null;
        }

        return counter;
    }


    /**
     * Returns the new constant pool index of the entry at the
     * given index.
     */
    private int remapCpIndex(int cpIndex)
    {
        return cpIndexMap[cpIndex];
    }


    /**
     * Remaps all constant pool indices in the given array.
     */
    private void remapCpIndexArray(int[] array, int length)
    {
        for (int i = 0; i < length; i++)
        {
            array[i] = remapCpIndex(array[i]);
        }
    }


    /**
     * Removes all indices that point to unused constant pool entries
     * from the given array.
     * @return the new number of indices.
     */
    private static int shrinkCpIndexArray(CpInfo[] contantPool, int[] array, int length)
    {
        int counter = 0;

        // Shift the used objects together.
        for (int index = 0; index < length; index++)
        {
            if (UsageMarker.isUsed(contantPool[array[index]]))
            {
                array[counter++] = array[index];
            }
        }

        // Clear the remaining array elements.
        for (int index = counter; index < length; index++)
        {
            array[index] = 0;
        }

        return counter;
    }


    /**
     * Removes all ClassFile objects that are not marked as being used
     * from the given array and returns the remaining objects in a an array
     * of the right size.
     * @return the new array.
     */
    private static ClassFile[] shrinkToNewArray(ClassFile[] array)
    {
        if (array == null)
        {
            return null;
        }

        // Shrink the given array in-place.
        int length = shrinkArray(array, array.length);
        if (length == 0)
        {
            return null;
        }

        // Return immediately if the array is of right size already.
        if (length == array.length)
        {
            return array;
        }

        // Copy the remaining elements into a new array of the right size.
        ClassFile[] newArray = new ClassFile[length];
        System.arraycopy(array, 0, newArray, 0, length);
        return newArray;
    }


    /**
     * Removes all VisitorAccepter objects that are not marked as being used
     * from the given array.
     * @return the new number of VisitorAccepter objects.
     */
    private static int shrinkArray(VisitorAccepter[] array, int length)
    {
        int counter = 0;

        // Shift the used objects together.
        for (int index = 0; index < length; index++)
        {
            if (UsageMarker.isUsed(array[index]))
            {
                array[counter++] = array[index];
            }
        }

        // Clear the remaining array elements.
        for (int index = counter; index < length; index++)
        {
            array[index] = null;
        }

        return counter;
    }
}
