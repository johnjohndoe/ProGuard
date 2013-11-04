/* $Id: ClassFilePrinter.java,v 1.15 2003/12/06 22:15:38 eric Exp $
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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.instruction.*;

import java.io.*;


/**
 * This <code>ClassFileVisitor</code> prints out the complete internal
 * structure of the class files it visits.
 *
 * @author Eric Lafortune
 */
public class ClassFilePrinter
  implements ClassFileVisitor,
             CpInfoVisitor,
             MemberInfoVisitor,
             AttrInfoVisitor,
             InstructionVisitor,
             ExceptionInfoVisitor,
             InnerClassesInfoVisitor
{
    private static final String INDENTATION = "  ";

    private PrintStream ps;
    private int         indentation;


    /**
     * Creates a new ClassFilePrinter that prints to <code>System.out</code>.
     */
    public ClassFilePrinter()
    {
        this(System.out);
    }


    /**
     * Creates a new ClassFilePrinter that prints to the given
     * <code>PrintStream</code>.
     */
    public ClassFilePrinter(PrintStream printStream)
    {
        ps = printStream;
    }


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        println("_____________________________________________________________________");
        println(visitorInfo(programClassFile) + " CLASS: " + programClassFile.getName());
        println("Magic: " + Integer.toHexString(programClassFile.u4magic));
        println("Minor version: " + Integer.toHexString(programClassFile.u2minorVersion));
        println("Major version: " + Integer.toHexString(programClassFile.u2majorVersion));
        println("Access: " + Integer.toHexString(programClassFile.u2accessFlags));
        println("Superclass: " + programClassFile.getSuperName());
        println();

        println("Interfaces (count = " + programClassFile.u2interfacesCount + "):");
        indent();
        for (int i = 0; i < programClassFile.u2interfacesCount; i++)
        {
            println("+ " + programClassFile.getCpClassNameString(programClassFile.u2interfaces[i]));
        }
        outdent();
        println();

        println("Constant Pool (count = " + programClassFile.u2constantPoolCount + "):");
        indent();
        programClassFile.constantPoolEntriesAccept(this);
        outdent();
        println();

        println("Fields (count = " + programClassFile.u2fieldsCount + "):");
        indent();
        programClassFile.fieldsAccept(this);
        outdent();
        println();

        println("Methods (count = " + programClassFile.u2methodsCount + "):");
        indent();
        programClassFile.methodsAccept(this);
        outdent();
        println();

        println("Class file attributes (count = " + programClassFile.u2attributesCount + "):");
        indent();
        programClassFile.attributesAccept(this);
        outdent();
        println();
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
/*
        println("_____________________________________________________________________");
        println(visitorInfo(libraryClassFile) + "LIBRARY CLASS: " + libraryClassFile.getName());
//      println("Access: " + Integer.toHexString(libraryClassFile.u2accessFlags));
        println("Superclass: " + libraryClassFile.getSuperName());
        println();

        println("Interfaces (count = " + libraryClassFile.interfaceClasses.length + "):");
        for (int i = 0; i < libraryClassFile.interfaceClasses.length; i++)
        {
            println("  + " + libraryClassFile.interfaceClasses[i].getName());
        }

        println("Fields (count = " + libraryClassFile.fields.length + "):");
        libraryClassFile.fieldsAccept(this);

        println("Methods (count = " + libraryClassFile.methods.length + "):");
        libraryClassFile.methodsAccept(this);
*/
    }


    // Implementations for CpInfoVisitor.

    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        println(visitorInfo(classCpInfo) + " Class [" +
                classFile.getCpString(classCpInfo.u2nameIndex) + "]");
    }


    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo)
    {
        println(visitorInfo(doubleCpInfo) + " Double [" +
                Double.longBitsToDouble(((long)doubleCpInfo.u4highBytes << 32) |
                                         (long)doubleCpInfo.u4lowBytes) + "]");
    }


    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo)
    {
        println(visitorInfo(fieldrefCpInfo) + " Fieldref [" +
                classFile.getCpClassNameString(fieldrefCpInfo.u2classIndex)  + "." +
                classFile.getCpNameString(fieldrefCpInfo.u2nameAndTypeIndex) + " " +
                classFile.getCpTypeString(fieldrefCpInfo.u2nameAndTypeIndex) + "]");
    }


    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo)
    {
        println(visitorInfo(floatCpInfo) + " Float [" +
                Float.intBitsToFloat(floatCpInfo.u4bytes) + "]");
    }


    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo)
    {
        println(visitorInfo(integerCpInfo) + " Integer [" +
                integerCpInfo.u4bytes + "]");
    }


    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo)
    {
        println(visitorInfo(interfaceMethodrefCpInfo) + " InterfaceMethodref [" +
                classFile.getCpClassNameString(interfaceMethodrefCpInfo.u2classIndex)  + "." +
                classFile.getCpNameString(interfaceMethodrefCpInfo.u2nameAndTypeIndex) + " " +
                classFile.getCpTypeString(interfaceMethodrefCpInfo.u2nameAndTypeIndex) + "]");
    }


    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo)
    {
        println(visitorInfo(longCpInfo) + " Long [" +
                (((long)longCpInfo.u4highBytes << 32) |
                  (long)longCpInfo.u4lowBytes) + "]");
    }


    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        println(visitorInfo(methodrefCpInfo) + " Methodref [" +
                classFile.getCpClassNameString(methodrefCpInfo.u2classIndex)  + "." +
                classFile.getCpNameString(methodrefCpInfo.u2nameAndTypeIndex) + " " +
                classFile.getCpTypeString(methodrefCpInfo.u2nameAndTypeIndex) + "]");
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        println(visitorInfo(nameAndTypeCpInfo) + " NameAndType [" +
                classFile.getCpString(nameAndTypeCpInfo.u2nameIndex) + " " +
                classFile.getCpString(nameAndTypeCpInfo.u2descriptorIndex) + "]");
    }


    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
    {
        println(visitorInfo(stringCpInfo) + " String [" +
                classFile.getCpString(stringCpInfo.u2stringIndex) + "]");
    }


    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo)
    {
        println(visitorInfo(utf8CpInfo) + " Utf8 [" +
                utf8CpInfo.getString() + "]");
    }


    // Implementations for MemberInfoVisitor.

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
        println(visitorInfo(programMemberInfo) + " " +
                programClassFile.getCpString(programMemberInfo.u2nameIndex) + " " +
                programClassFile.getCpString(programMemberInfo.u2descriptorIndex));

        if (programMemberInfo.u2attributesCount > 0)
        {
            indent();
            println("Class member attributes (count = " + programMemberInfo.u2attributesCount + "):");
            programMemberInfo.attributesAccept(programClassFile, this);
            outdent();
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        visitLibraryMemberInfo(libraryClassFile, libraryFieldInfo);
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        visitLibraryMemberInfo(libraryClassFile, libraryMethodInfo);
    }


    private void visitLibraryMemberInfo(LibraryClassFile libraryClassFile, LibraryMemberInfo libraryMemberInfo)
    {
        println(visitorInfo(libraryMemberInfo) + " " +
                libraryMemberInfo.getName(libraryClassFile) + " " +
                libraryMemberInfo.getDescriptor(libraryClassFile));
    }


    // Implementations for AttrInfoVisitor.
    // Note that attributes are typically only referenced once, so we don't
    // test if they are marked already.

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo)
    {
        println(visitorInfo(unknownAttrInfo) +
                " Unknown attribute (" + classFile.getCpString(unknownAttrInfo.u2attrNameIndex) + ")");
    }


    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo)
    {
        println(visitorInfo(innerClassesAttrInfo) +
                " Inner classes attribute (count = " + innerClassesAttrInfo.u2numberOfClasses + ")");

        indent();
        innerClassesAttrInfo.innerClassEntriesAccept(classFile, this);
        outdent();
    }


    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo)
    {
        println(visitorInfo(constantValueAttrInfo) +
                " Constant value attribute:");

        classFile.constantPoolEntryAccept(this, constantValueAttrInfo.u2constantValueIndex);
    }


    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo)
    {
        println(visitorInfo(exceptionsAttrInfo) +
                " Exceptions attribute (count = " + exceptionsAttrInfo.u2numberOfExceptions + ")");

        indent();
        exceptionsAttrInfo.exceptionEntriesAccept((ProgramClassFile)classFile, this);
        outdent();
    }


    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        println(visitorInfo(codeAttrInfo) +
                " Code attribute instructions (code length = "+
                codeAttrInfo.u4codeLength + "):");

        indent();
        indent();
        codeAttrInfo.instructionsAccept(classFile, this);
        outdent();

        println("Code attribute exceptions (count = " +
                codeAttrInfo.u2exceptionTableLength + "):");

        indent();
        codeAttrInfo.exceptionsAccept(classFile, this);
        outdent();

        println("Code attribute attributes (attribute count = " +
                codeAttrInfo.u2attributesCount + "):");

        indent();
        codeAttrInfo.attributesAccept(classFile, this);
        outdent();
        outdent();
    }


    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo)
    {
        println(visitorInfo(lineNumberTableAttrInfo) +
                " Line number table attribute (count = " + lineNumberTableAttrInfo.u2lineNumberTableLength + ")");
        // ...
    }


    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo)
    {
        println(visitorInfo(localVariableTableAttrInfo) +
                " Local variable table attribute (count = "+localVariableTableAttrInfo.u2localVariableTableLength + ")");
        // ...
    }


    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo)
    {
        println(visitorInfo(sourceFileAttrInfo) +
                " Source file attribute:");

        indent();
        classFile.constantPoolEntryAccept(this, sourceFileAttrInfo.u2sourceFileIndex);
        outdent();
    }


    public void visitSourceDirAttrInfo(ClassFile classFile, SourceDirAttrInfo sourceDirAttrInfo)
    {
        println(visitorInfo(sourceDirAttrInfo) +
                " Source dir attribute:");

        indent();
        classFile.constantPoolEntryAccept(this, sourceDirAttrInfo.u2sourceDirIndex);
        outdent();
    }


    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo)
    {
        println(visitorInfo(deprecatedAttrInfo) +
                " Deprecated attribute");
    }


    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo)
    {
        println(visitorInfo(syntheticAttrInfo) +
                " Synthetic attribute");
    }


    public void visitSignatureAttrInfo(ClassFile classFile, SignatureAttrInfo signatureAttrInfo)
    {
        println(visitorInfo(signatureAttrInfo) +
                " Signature attribute:");

        indent();
        classFile.constantPoolEntryAccept(this, signatureAttrInfo.u2signatureIndex);
        outdent();
    }


    // Implementations for InstructionVisitor.

    public void visitInstruction(ClassFile classFile, Instruction instruction)
    {
        // Just ignore generic instructions.
    }


    public void visitCpInstruction(ClassFile classFile, CpInstruction cpInstruction)
    {
        Instruction in = (Instruction)cpInstruction;
        println("- CpInstruction ["+in.getOpcode()+
                "] at offset ["+in.getOffset()+
                "], wide = ["+in.isWide()+
                "], CpIndex = ["+cpInstruction.getCpIndex()+
                "]");
        //println("      - CpInstruction [" +
        //           cpInstruction.getCpIndex() + "]");
        indent();
        classFile.constantPoolEntryAccept(this, cpInstruction.getCpIndex());
        outdent();
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(ClassFile classFile, ExceptionInfo exceptionInfo)
    {
        println(visitorInfo(exceptionInfo) +
                " ExceptionInfo:");

        if (exceptionInfo.u2catchType != 0)
        {
            classFile.constantPoolEntryAccept(this, exceptionInfo.u2catchType);
        }
    }


    // Implementations for InnerClassesInfoVisitor.

    public void visitInnerClassesInfo(ClassFile classFile, InnerClassesInfo innerClassesInfo)
    {
        println(visitorInfo(innerClassesInfo) +
                " InnerClassesInfo:");
        indent();
        if (innerClassesInfo.u2innerClassInfoIndex != 0)
        {
            classFile.constantPoolEntryAccept(this, innerClassesInfo.u2innerClassInfoIndex);
        }

        if (innerClassesInfo.u2outerClassInfoIndex != 0)
        {
            classFile.constantPoolEntryAccept(this, innerClassesInfo.u2outerClassInfoIndex);
        }

        if (innerClassesInfo.u2innerNameIndex != 0)
        {
            classFile.constantPoolEntryAccept(this, innerClassesInfo.u2innerNameIndex);
        }
        outdent();
    }


    // Small utility methods.

    private void indent()
    {
        indentation++;
    }

    private void outdent()
    {
        indentation--;
    }

    private void println()
    {
        ps.println();
    }

    private void println(String string)
    {
        for (int i = 0; i < indentation; i++)
        {
            ps.print(INDENTATION);
        }

        ps.println(string);
    }


    private String visitorInfo(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == null ? "-" : "+";
    }
}
