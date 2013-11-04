/* $Id: ConstantPoolEditor.java,v 1.3 2004/09/04 16:29:33 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
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
package proguard.classfile.editor;

import proguard.classfile.*;

/**
 * This class can add constant pool entries to given class files.
 *
 * @author Eric Lafortune
 */
public class ConstantPoolEditor
{
    /**
     * Finds or creates a FieldrefCpInfo constant pool entry with the given
     * class constant pool entry index, field name, and descriptor, in the
     * given class file.
     * @return the constant pool index of the FieldrefCpInfo.
     */
    public int addFieldrefCpInfo(ProgramClassFile programClassFile,
                                 int              classIndex,
                                 String           name,
                                 String           descriptor,
                                 ClassFile        referencedClassFile,
                                 ClassFile[]      referencedClassFiles,
                                 MemberInfo       referencedMemberInfo)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Fieldref)
            {
                FieldrefCpInfo fieldrefCpInfo = (FieldrefCpInfo)cpInfo;
                if (fieldrefCpInfo.u2classIndex == classIndex             &&
                    fieldrefCpInfo.getName(programClassFile).equals(name) &&
                    fieldrefCpInfo.getType(programClassFile).equals(descriptor))
                {
                    return index;
                }
            }
        }

        int u2nameAndTypeIndex = addNameAndTypeCpInfo(programClassFile,
                                                      name,
                                                      descriptor,
                                                      referencedClassFiles);

        return addCpInfo(programClassFile,
                         new FieldrefCpInfo(classIndex,
                                            u2nameAndTypeIndex,
                                            referencedClassFile,
                                            referencedMemberInfo));
    }


    /**
     * Finds or creates a FieldrefCpInfo constant pool entry with the given
     * class name, field name, and descriptor, in the given class file.
     * @return the constant pool index of the FieldrefCpInfo.
     */
    public int addFieldrefCpInfo(ProgramClassFile programClassFile,
                                 String           className,
                                 String           name,
                                 String           descriptor,
                                 ClassFile        referencedClassFile,
                                 ClassFile[]      referencedClassFiles,
                                 MemberInfo       referencedMemberInfo)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Fieldref)
            {
                FieldrefCpInfo fieldrefCpInfo = (FieldrefCpInfo)cpInfo;
                if (fieldrefCpInfo.getClassName(programClassFile).equals(className) &&
                    fieldrefCpInfo.getName(programClassFile).equals(name)           &&
                    fieldrefCpInfo.getType(programClassFile).equals(descriptor))
                {
                    return index;
                }
            }
        }

        int u2classIndex       = addClassCpInfo(programClassFile,
                                                className,
                                                referencedClassFile);

        int u2nameAndTypeIndex = addNameAndTypeCpInfo(programClassFile,
                                                      name,
                                                      descriptor,
                                                      referencedClassFiles);

        return addCpInfo(programClassFile,
                         new FieldrefCpInfo(u2classIndex,
                                            u2nameAndTypeIndex,
                                            referencedClassFile,
                                            referencedMemberInfo));
    }

    /**
     * Finds or creates a MethodrefCpInfo constant pool entry with the given
     * class constant pool entry index, method name, and descriptor, in the
     * given class file.
     * @return the constant pool index of the MethodrefCpInfo.
     */
    public int addMethodrefCpInfo(ProgramClassFile programClassFile,
                                  int              classIndex,
                                  String           name,
                                  String           descriptor,
                                  ClassFile        referencedClassFile,
                                  ClassFile[]      referencedClassFiles,
                                  MemberInfo       referencedMemberInfo)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Methodref)
            {
                MethodrefCpInfo methodrefCpInfo = (MethodrefCpInfo)cpInfo;
                if (methodrefCpInfo.u2classIndex == classIndex             &&
                    methodrefCpInfo.getName(programClassFile).equals(name) &&
                    methodrefCpInfo.getType(programClassFile).equals(descriptor))
                {
                    return index;
                }
            }
        }

        int u2nameAndTypeIndex = addNameAndTypeCpInfo(programClassFile,
                                                      name,
                                                      descriptor,
                                                      referencedClassFiles);

        return addCpInfo(programClassFile,
                         new MethodrefCpInfo(classIndex,
                                             u2nameAndTypeIndex,
                                             referencedClassFile,
                                             referencedMemberInfo));
    }


    /**
     * Finds or creates a MethodrefCpInfo constant pool entry with the given
     * class name, method name, and descriptor, in the given class file.
     * @return the constant pool index of the MethodrefCpInfo.
     */
    public int addMethodrefCpInfo(ProgramClassFile programClassFile,
                                  String           className,
                                  String           name,
                                  String           descriptor,
                                  ClassFile        referencedClassFile,
                                  ClassFile[]      referencedClassFiles,
                                  MemberInfo       referencedMemberInfo)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Methodref)
            {
                MethodrefCpInfo methodrefCpInfo = (MethodrefCpInfo)cpInfo;
                if (methodrefCpInfo.getClassName(programClassFile).equals(className) &&
                    methodrefCpInfo.getName(programClassFile).equals(name)           &&
                    methodrefCpInfo.getType(programClassFile).equals(descriptor))
                {
                    return index;
                }
            }
        }

        int u2classIndex       = addClassCpInfo(programClassFile,
                                                className,
                                                referencedClassFile);

        int u2nameAndTypeIndex = addNameAndTypeCpInfo(programClassFile,
                                                      name,
                                                      descriptor,
                                                      referencedClassFiles);

        return addCpInfo(programClassFile,
                         new MethodrefCpInfo(u2classIndex,
                                             u2nameAndTypeIndex,
                                             referencedClassFile,
                                             referencedMemberInfo));
    }


    /**
     * Finds or creates a ClassCpInfo constant pool entry with the given name,
     * in the given class file.
     * @return the constant pool index of the ClassCpInfo.
     */
    public int addClassCpInfo(ProgramClassFile programClassFile,
                              String           name,
                              ClassFile        referencedClassFile)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Class)
            {
                ClassCpInfo classCpInfo = (ClassCpInfo)cpInfo;
                if (classCpInfo.getName(programClassFile).equals(name))
                {
                    return index;
                }
            }
        }

        int u2nameIndex = addUtf8CpInfo(programClassFile, name);

        return addCpInfo(programClassFile,
                         new ClassCpInfo(u2nameIndex,
                                         referencedClassFile));
    }


    /**
     * Finds or creates a NameAndTypeCpInfo constant pool entry with the given
     * name and type, in the given class file.
     * @return the constant pool index of the NameAndTypeCpInfo.
     */
    public int addNameAndTypeCpInfo(ProgramClassFile programClassFile,
                                    String           name,
                                    String           type,
                                    ClassFile[]      referencedClassFiles)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_NameAndType)
            {
                NameAndTypeCpInfo nameAndTypeCpInfo = (NameAndTypeCpInfo)cpInfo;
                if (nameAndTypeCpInfo.getName(programClassFile).equals(name) &&
                    nameAndTypeCpInfo.getType(programClassFile).equals(type))
                {
                    return index;
                }
            }
        }

        int u2nameIndex       = addUtf8CpInfo(programClassFile, name);
        int u2descriptorIndex = addUtf8CpInfo(programClassFile, type);

        return addCpInfo(programClassFile,
                         new NameAndTypeCpInfo(u2nameIndex,
                                               u2descriptorIndex,
                                               referencedClassFiles));
    }


    /**
     * Finds or creates an Utf8CpInfo constant pool entry for the given string,
     * in the given class file.
     * @return the constant pool index of the Utf8CpInfo.
     */
    private int addUtf8CpInfo(ProgramClassFile programClassFile,
                              String           string)
    {
        CpInfo[] constantPool        = programClassFile.constantPool;
        int      u2constantPoolCount = programClassFile.u2constantPoolCount;

        // Check if the entry already exists.
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            CpInfo cpInfo = constantPool[index];

            if (cpInfo != null &&
                cpInfo.getTag() == ClassConstants.CONSTANT_Utf8)
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
     * Adds a given constant pool entry to the end of the constant pool
     * in the given class file.
     * @return the constant pool index for the added entry.
     */
    private int addCpInfo(ProgramClassFile programClassFile,
                          CpInfo           cpInfo)
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
}
