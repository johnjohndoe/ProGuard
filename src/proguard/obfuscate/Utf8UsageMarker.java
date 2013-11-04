/* $Id: Utf8UsageMarker.java,v 1.11 2002/11/03 13:30:14 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.visitor.*;


/**
 * This ClassFileVisitor marks all UTF-8 constant pool entries that are
 * being used in the classes it visits.
 *
 * @see Utf8Shrinker
 *
 * @author Eric Lafortune
 */
public class Utf8UsageMarker
  implements ClassFileVisitor,
             MemberInfoVisitor,
             CpInfoVisitor,
             AttrInfoVisitor,
             InnerClassesInfoVisitor,
             LocalVariableInfoVisitor
{
    // A visitor info flag to indicate the UTF-8 constant pool entry is being used.
    private static final Object USED = new Object();


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Mark the UTF-8 entries referenced by all other constant pool entries.
        programClassFile.constantPoolEntriesAccept(this);

        // Mark the UTF-8 entries referenced by all fields and methods.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);

        // Mark the UTF-8 entries referenced by all attributes.
        programClassFile.attributesAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
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
        // Mark the name and descriptor UTF-8 entries.
        markCpUtf8Entry(programClassFile, programMemberInfo.u2nameIndex);
        markCpUtf8Entry(programClassFile, programMemberInfo.u2descriptorIndex);

        // Mark the UTF-8 entries referenced by all attributes.
        programMemberInfo.attributesAccept(programClassFile, this);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Implementations for CpInfoVisitor

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo) {}


    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
    {
        markCpUtf8Entry(classFile, stringCpInfo.u2stringIndex);
    }


    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        markCpUtf8Entry(classFile, classCpInfo.u2nameIndex);
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        markCpUtf8Entry(classFile, nameAndTypeCpInfo.u2nameIndex);
        markCpUtf8Entry(classFile, nameAndTypeCpInfo.u2descriptorIndex);
    }


    // Implementations for AttrInfoVisitor

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo)
    {
        // This is the best we can do for unknown attributes.
        markCpUtf8Entry(classFile, unknownAttrInfo.u2attrNameIndex);
    }


    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo)
    {
        markCpUtf8Entry(classFile, innerClassesAttrInfo.u2attrNameIndex);

        innerClassesAttrInfo.innerClassEntriesAccept(classFile, this);
    }


    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo)
    {
        markCpUtf8Entry(classFile, constantValueAttrInfo.u2attrNameIndex);
    }


    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo)
    {
        markCpUtf8Entry(classFile, exceptionsAttrInfo.u2attrNameIndex);
    }


    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        markCpUtf8Entry(classFile, codeAttrInfo.u2attrNameIndex);

        codeAttrInfo.attributesAccept(classFile, this);
    }


    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo)
    {
        markCpUtf8Entry(classFile, lineNumberTableAttrInfo.u2attrNameIndex);
    }


    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo)
    {
        markCpUtf8Entry(classFile, localVariableTableAttrInfo.u2attrNameIndex);

        localVariableTableAttrInfo.localVariablesAccept(classFile, this);
    }


    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo)
    {
        markCpUtf8Entry(classFile, sourceFileAttrInfo.u2attrNameIndex);

        markCpUtf8Entry(classFile, sourceFileAttrInfo.u2sourceFileIndex);
    }


    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo)
    {
        markCpUtf8Entry(classFile, deprecatedAttrInfo.u2attrNameIndex);
    }


    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo)
    {
        markCpUtf8Entry(classFile, syntheticAttrInfo.u2attrNameIndex);
    }


    // Implementations for InnerClassesInfoVisitor

    public void visitInnerClassesInfo(ClassFile classFile, InnerClassesInfo innerClassesInfo)
    {
        if (innerClassesInfo.u2innerNameIndex != 0)
        {
            markCpUtf8Entry(classFile, innerClassesInfo.u2innerNameIndex);
        }
    }


    // Implementations for LocalVariableInfoVisitor

    public void visitLocalVariableInfo(ClassFile classFile, LocalVariableInfo localVariableInfo)
    {
        markCpUtf8Entry(classFile, localVariableInfo.u2nameIndex);
        markCpUtf8Entry(classFile, localVariableInfo.u2descriptorIndex);
    }


    // Small utility methods.

    /**
     * Marks the given UTF-8 constant pool entry of the given class.
     */
    private void markCpUtf8Entry(ClassFile classFile, int index)
    {
         markAsUsed((Utf8CpInfo)((ProgramClassFile)classFile).getCpEntry(index));
    }


    /**
     * Marks the given VisitorAccepter as being used.
     * In this context, the VisitorAccepter will be a Utf8CpInfo object.
     */
    private static void markAsUsed(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(USED);
    }


    /**
     * Returns whether the given VisitorAccepter has been marked as being used.
     * In this context, the VisitorAccepter will be a Utf8CpInfo object.
     */
    static boolean isUsed(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == USED;
    }
}
