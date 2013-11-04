/* $Id: ClassFileRenamer.java,v 1.12 2002/07/10 16:13:48 eric Exp $
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
import proguard.classfile.util.*;

import java.util.*;


/**
 * This <code>ClassFileVisitor</code> renames the class names and class member
 * names of the classes it visits, using names previously determined by the
 * obfuscator.
 *
 * @see ClassFileObfuscator
 *
 * @author Eric Lafortune
 */
public class ClassFileRenamer
  implements ClassFileVisitor,
             MemberInfoVisitor,
             CpInfoVisitor
{
    private DescriptorClassEnumeration descriptorClassEnumeration =
        new DescriptorClassEnumeration();


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Rename class members.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);

        // Rename NameAndTypeCpInfo type references in the constant pool.
        programClassFile.constantPoolEntriesAccept(new MyNameAndTypeTypeRenamer());

        // Rename class references and class member references in the constant pool.
        programClassFile.constantPoolEntriesAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile) {}


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
    {
        visitMemberInfo(programClassFile, programfieldInfo);
    }

    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMemberInfo(programClassFile, programMethodInfo);
    }

    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        // The new name is stored with the class member.
        String newName = MemberObfuscator.newMemberName(programMemberInfo);
        if (newName != null)
        {
            programMemberInfo.u2nameIndex =
                createUtf8CpInfo(programClassFile, newName);
        }

        // The new descriptor can be computed.
        String newDescriptor = newDescriptor(programMemberInfo.getDescriptor(programClassFile),
                                             programMemberInfo.referencedClassFiles);
        if (newDescriptor != null)
        {
            programMemberInfo.u2descriptorIndex =
                createUtf8CpInfo(programClassFile, newDescriptor);
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    /**
     * This CpInfoVisitor renames all type elements in all NameAndTypeCpInfo
     * constant pool entries it visits.
     */
    private class MyNameAndTypeTypeRenamer
       implements CpInfoVisitor
    {
        // Implementations for CpInfoVisitor

        public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
        public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
        public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
        public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
        public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
        public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
        public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo) {}
        public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
        public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
        public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo) {}


        public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
        {
            // The new descriptor can be computed.
            String newDescriptor = newDescriptor(nameAndTypeCpInfo.getType(classFile),
                                                 nameAndTypeCpInfo.referencedClassFiles);
            if (newDescriptor != null)
            {
                nameAndTypeCpInfo.u2descriptorIndex =
                    createUtf8CpInfo((ProgramClassFile)classFile, newDescriptor);
            }
        }
    }


    // Implementations for CpInfoVisitor

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}

    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
    {
        // If the string is being used in a Class.forName construct, the new
        // class name can be retrieved from the referenced ClassFile.
        String newClassName = newClassName(stringCpInfo.getString(classFile),
                                           stringCpInfo.referencedClassFile);
        if (newClassName != null)
        {
            String newExternalClassName = ClassUtil.externalClassName(newClassName);

            // Refer to a new Utf8 entry.
            stringCpInfo.u2stringIndex =
                createUtf8CpInfo((ProgramClassFile)classFile, newExternalClassName);
        }
    }

    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        // The new class name can be retrieved from the referenced ClassFile.
        String newClassName = newClassName(classCpInfo.getName(classFile),
                                           classCpInfo.referencedClassFile);
        if (newClassName != null)
        {
            // Refer to a new Utf8 entry.
            classCpInfo.u2nameIndex =
                createUtf8CpInfo((ProgramClassFile)classFile, newClassName);
        }
    }


    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo)
    {
        visitRefCpInfo(classFile, fieldrefCpInfo);
    }

    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo)
    {
        visitRefCpInfo(classFile, interfaceMethodrefCpInfo);
    }

    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        visitRefCpInfo(classFile, methodrefCpInfo);
    }

    private void visitRefCpInfo(ClassFile classFile, RefCpInfo refCpInfo)
    {
        // The new class member name to be set in this entry's NameAndTypeCpInfo
        // can be retrieved from the referenced class member.
        ProgramMemberInfo referencedMemberInfo = refCpInfo.referencedMemberInfo;
        if (referencedMemberInfo != null)
        {
            String newMemberName =
                MemberObfuscator.newMemberName(referencedMemberInfo);

            if (newMemberName != null)
            {
                refCpInfo.u2nameAndTypeIndex =
                    createNameAndTypeCpInfo((ProgramClassFile)classFile,
                                            newMemberName,
                                            refCpInfo.getType(classFile));
            }
        }
    }


    // Small utility methods.

    /**
     * Finds and exisiting NameAndTypeCpInfo class pool entry, or creates a new one,
     * for the given name and type.
     * @return the constant pool index of the Utf8CpInfo.
     */
    private int createNameAndTypeCpInfo(ProgramClassFile programClassFile,
                                        String           name,
                                        String           type)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Pick up the right list of referenced class files, in case we need to
        // create a new NameAndTypeCpInfo.
        ClassFile[] referencedClassFiles = null;

        // Check if there is a NameAndTypeCpInfo with the given name and type already.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null && cpInfo instanceof NameAndTypeCpInfo)
            {
                NameAndTypeCpInfo nameAndTypeCpInfo = (NameAndTypeCpInfo)cpInfo;
                if (nameAndTypeCpInfo.getType(programClassFile).equals(type))
                {
                    if (nameAndTypeCpInfo.getName(programClassFile).equals(name))
                    {
                        return index;
                    }

                    referencedClassFiles = nameAndTypeCpInfo.referencedClassFiles;
                }
            }
        }

        int u2nameIndex       = createUtf8CpInfo(programClassFile, name);
        int u2descriptorIndex = createUtf8CpInfo(programClassFile, type);

        return addCpInfo(programClassFile, new NameAndTypeCpInfo(u2nameIndex,
                                                                 u2descriptorIndex,
                                                                 referencedClassFiles));
    }


    /**
     * Finds and exisiting Utf8CpInfo class pool entry, or creates a new one,
     * for the given string.
     * @return the constant pool index of the Utf8CpInfo.
     */
    private int createUtf8CpInfo(ProgramClassFile programClassFile, String string)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if there is a Utf8CpInfo with the given string already.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null && cpInfo instanceof Utf8CpInfo)
            {
                Utf8CpInfo utf8CpInfo = (Utf8CpInfo)cpInfo;
                if (utf8CpInfo.getString().equals(string))
                {
                    return index;
                }
            }
        }

        return addCpInfo(programClassFile, new Utf8CpInfo(string));
    }


    /**
     * Adds a given constant pool entry to the end of the constant pool.
     * @return the constant pool index for the added entry.
     */
    private int addCpInfo(ProgramClassFile programClassFile, CpInfo cpInfo)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Make sure there is enough space for another constant pool entry.
        if (u2constantPoolCount == constantPool.length)
        {
            programClassFile.constantPool = new CpInfo[u2constantPoolCount+1];
            System.arraycopy(constantPool, 0,
                             programClassFile.constantPool, 0,
                             u2constantPoolCount);
            constantPool = programClassFile.constantPool;

        }

        // Create a new Utf8CpInfo for the given string.
        constantPool[programClassFile.u2constantPoolCount++] = cpInfo;

        return u2constantPoolCount;
    }


    /**
     * Returns the new descriptor based on the given descriptor and the new
     * names of the given referenced class files.
     */
    private String newDescriptor(String      descriptor,
                                 ClassFile[] referencedClassFiles)
    {
        if (referencedClassFiles == null)
        {
            // There are no referenced program classes, so the descriptor
            // doesn't change.
            return null;
        }

        descriptorClassEnumeration.setDescriptor(descriptor);

        String newDescriptor = descriptorClassEnumeration.nextFluff();

        int index = 0;
        while (descriptorClassEnumeration.hasMoreClassNames())
        {
            String className = descriptorClassEnumeration.nextClassName();
            String fluff     = descriptorClassEnumeration.nextFluff();

            ClassFile referencedClassFile = referencedClassFiles[index++];

            // Fall back on the original class name if there is no new name.
            String newClassName = referencedClassFile != null ?
                ClassFileObfuscator.newClassName(referencedClassFile) :
                className;

            newDescriptor = newDescriptor + newClassName + fluff;
        }

        return newDescriptor;
    }

    /**
     * Returns the new class name based on the given class name and the new
     * name of the given referenced class file. Class names of array types
     * are handled properly.
     */
    private String newClassName(String    className,
                                ClassFile referencedClassFile)
    {
        if (referencedClassFile == null)
        {
            // There is no referenced program class, so the descriptor doesn't
            // change.
            return null;
        }

        String newClassName =
            ClassFileObfuscator.newClassName(referencedClassFile);

        // Is it an array type?
        if (className.charAt(0) == ClassConstants.INTERNAL_TYPE_ARRAY)
        {
            // Add the array prefixes and suffix "[L...;".
            newClassName =
                 className.substring(0, className.indexOf(ClassConstants.INTERNAL_TYPE_CLASS_START)+1) +
                 newClassName +
                 ClassConstants.INTERNAL_TYPE_CLASS_END;
        }

        return newClassName;
    }
}
