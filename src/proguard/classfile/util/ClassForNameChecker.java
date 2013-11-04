/* $Id: ClassForNameChecker.java,v 1.4 2003/12/06 22:15:38 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.instruction.*;
import proguard.classfile.visitor.*;


/**
 * This class can check whether method references point to <code>Class.forName</code>
 * or <code>.class</code>.
 *
 * @author Eric Lafortune
 */
class      ClassForNameChecker
implements CpInfoVisitor,
           MemberInfoVisitor,
           AttrInfoVisitor,
           InstructionVisitor
{
    // These fields acts as a return variables and status variables for the visitors.
    private boolean isClassForNameInvocation;
    private boolean insideDotClassMethod;
    private boolean firstInstructionOk;


    /**
     * Creates a new ClassForNameChecker.
     */
    public ClassForNameChecker()
    {
    }


    /**
     * Checks whether the specified method reference points to
     * <code>Class.forName</code> or <code>.class</code>.
     * @param classFile            the class file in which the method reference
     *                             is located.
     * @param methodrefCpInfoIndex the index of the method reference in the
     *                             constant pool.
     * @return a boolean that indicates whether the reference points to either
     *         method.
     */
    public boolean check(ClassFile classFile, int methodrefCpInfoIndex)
    {
        isClassForNameInvocation = false;

        classFile.constantPoolEntryAccept(this, methodrefCpInfoIndex);

        return isClassForNameInvocation;
    }


    // Implementations for CpInfoVisitor.

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo) {}
    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}

    /**
     * Checks whether the given method reference points to
     * <code>Class.forName</code> or <code>.class</code>.
     */
    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        String className  = methodrefCpInfo.getClassName(classFile);
        String methodName = methodrefCpInfo.getName(classFile);
        String methodType = methodrefCpInfo.getType(classFile);

        // Is it a reference to "Class Class.forName(String)"?
        isClassForNameInvocation =
            className .equals(ClassConstants.INTERNAL_CLASS_NAME_JAVA_LANG_CLASS) &&
            methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_CLASS_FOR_NAME) &&
            methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_CLASS_FOR_NAME);

        if (isClassForNameInvocation || insideDotClassMethod)
        {
            return;
        }

        // Is it a reference to .class? This construct is typically implemented
        // as (static) "Class class$(String)" or "Class class$(String, boolean)".
        // First check the type, which has to be right to start with.
        isClassForNameInvocation =
            methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_DOT_CLASS_JAVAC) ||
            methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_DOT_CLASS_JIKES);

        if (!isClassForNameInvocation)
        {
            return;
        }

        // Then check if the method perhaps still has its original "class$" name.
        isClassForNameInvocation =
            methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_DOT_CLASS);

        if (isClassForNameInvocation)
        {
            return;
        }

        // We're still not sure it's not a reference to .class, since the
        // method name may have been changed or obfuscated.
        // Perform a more detailed analysis by looking at the referenced method
        // itself. Make sure we don't do this recursively.
        insideDotClassMethod = true;
        methodrefCpInfo.referencedMemberInfoAccept(this);
        insideDotClassMethod = false;
    }


    // Implementations for MemberInfoVisitor.

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo) {}
    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}

    /**
     * Checks whether the given method is a .class implementation.
     */
    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        programMethodInfo.attributesAccept(programClassFile, this);
    }


    // Implementations for AttrInfoVisitor.

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo) {}
    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo) {}
    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo) {}
    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo) {}
    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo) {}
    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo) {}
    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo) {}
    public void visitSourceDirAttrInfo(ClassFile classFile, SourceDirAttrInfo sourceDirAttrInfo) {}
    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo) {}
    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo) {}
    public void visitSignatureAttrInfo(ClassFile classFile, SignatureAttrInfo signatureAttrInfo) {}


    /**
     * Checks whether the given code is an implementation of class$(String) or
     * class$(String, boolean).
     */
    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        // Check whether the first instruction recalls the first argument of
        // this method.
        firstInstructionOk = false;
        codeAttrInfo.instructionAccept(classFile, this, 0);

        // Continue checking whether the second instruction invokes
        // Class.forName.
        if (firstInstructionOk)
        {
            codeAttrInfo.instructionAccept(classFile, this, 1);
        }
    }


    // Implementations for InstructionVisitor.

    /**
     * Checks whether this is a valid first instruction for a .class implementation.
     */
    public void visitInstruction(ClassFile classFile, Instruction instruction)
    {
        firstInstructionOk = instruction.getOpcode() == Instruction.OP_ALOAD_0;
    }

    /**
     * Checks whether this is a valid second instruction for a .class implementation.
     */
    public void visitCpInstruction(ClassFile classFile, CpInstruction cpInstruction)
    {
        if (firstInstructionOk &&
            cpInstruction.getOpcode() == Instruction.OP_INVOKESTATIC)
        {
            classFile.constantPoolEntryAccept(this, cpInstruction.getCpIndex());
        }
    }
}
