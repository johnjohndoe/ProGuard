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
import proguard.classfile.constant.*;

/**
 * This class can add constant pool entries to given classes.
 *
 * @author Eric Lafortune
 */
public class ConstantPoolEditor
{
    private static final boolean DEBUG = false;


    /**
     * Finds or creates a IntegerConstant constant pool entry with the given value,
     * in the given class.
     * @return the constant pool index of the   Utf8Constant.
     */
    public int addIntegerConstant(ProgramClass programClass,
                                  int          value)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Integer)
            {
                IntegerConstant integerConstant = (IntegerConstant)constant;
                if (integerConstant.getValue() == value)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass, new IntegerConstant(value));
    }


    /**
     * Finds or creates a LongConstant constant pool entry with the given value,
     * in the given class.
     * @return the constant pool index of the   Utf8Constant.
     */
    public int addLongConstant(ProgramClass programClass,
                               long         value)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Long)
            {
                LongConstant longConstant = (LongConstant)constant;
                if (longConstant.getValue() == value)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass, new LongConstant(value));
    }


    /**
     * Finds or creates a FloatConstant constant pool entry with the given value,
     * in the given class.
     * @return the constant pool index of the   Utf8Constant.
     */
    public int addFloatConstant(ProgramClass programClass,
                                float        value)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Float)
            {
                FloatConstant floatConstant = (FloatConstant)constant;
                if (floatConstant.getValue() == value)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass, new FloatConstant(value));
    }


    /**
     * Finds or creates a DoubleConstant constant pool entry with the given value,
     * in the given class.
     * @return the constant pool index of the   Utf8Constant.
     */
    public int addDoubleConstant(ProgramClass programClass,
                                  double      value)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Double)
            {
                DoubleConstant doubleConstant = (DoubleConstant)constant;
                if (doubleConstant.getValue() == value)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass, new DoubleConstant(value));
    }


    /**
     * Finds or creates a StringConstant constant pool entry with the given value,
     * in the given class.
     * @return the constant pool index of the ClassConstant.
     */
    public int addStringConstant(ProgramClass programClass,
                                 String       string,
                                 Clazz        referencedClass,
                                 Member       referencedMember)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_String)
            {
                StringConstant classConstant = (StringConstant)constant;
                if (classConstant.getString(programClass).equals(string))
                {
                    return index;
                }
            }
        }

        int nameIndex = addUtf8Constant(programClass, string);

        return addConstant(programClass,
                           new StringConstant(nameIndex,
                                              referencedClass,
                                              referencedMember));
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry for the given
     * class and field, in the given class.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(ProgramClass programClass,
                                   Clazz        referencedClass,
                                   Member       referencedMember)
    {
        return addFieldrefConstant(programClass,
                                   referencedClass.getName(),
                                   referencedMember.getName(referencedClass),
                                   referencedMember.getDescriptor(referencedClass),
                                   referencedClass,
                                   referencedMember);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class name, field name, and descriptor, in the given class.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(ProgramClass programClass,
                                   String       className,
                                   String       name,
                                   String       descriptor,
                                   Clazz        referencedClass,
                                   Member       referencedMember)
    {
        return addFieldrefConstant(programClass,
                                   className,
                                   addNameAndTypeConstant(programClass,
                                                          name,
                                                          descriptor),
                                   referencedClass,
                                   referencedMember);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class name, field name, and descriptor, in the given class.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(ProgramClass programClass,
                                   String       className,
                                   int          nameAndTypeIndex,
                                   Clazz        referencedClass,
                                   Member       referencedMember)
    {
        return addFieldrefConstant(programClass,
                                   addClassConstant(programClass,
                                                    className,
                                                    referencedClass),
                                   nameAndTypeIndex,
                                   referencedClass,
                                   referencedMember);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class constant pool entry index, field name, and descriptor, in the
     * given class.
     * @return the constant pool index of the   FieldrefConstant.
     */
    public int addFieldrefConstant(ProgramClass programClass,
                                   int          classIndex,
                                   String       name,
                                   String       descriptor,
                                   Clazz        referencedClass,
                                   Member       referencedMember)
    {
        return addFieldrefConstant(programClass,
                                   classIndex,
                                   addNameAndTypeConstant(programClass,
                                                          name,
                                                          descriptor),
                                   referencedClass,
                                   referencedMember);
    }


    /**
     * Finds or creates a FieldrefConstant constant pool entry with the given
     * class constant pool entry index and name and type constant pool entry index
     * the given class.
     * @return the constant pool index of the FieldrefConstant.
     */
    public int addFieldrefConstant(ProgramClass programClass,
                                   int          classIndex,
                                   int          nameAndTypeIndex,
                                   Clazz        referencedClass,
                                   Member       referencedMember)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Fieldref)
            {
                FieldrefConstant fieldrefConstant = (FieldrefConstant)constant;
                if (fieldrefConstant.u2classIndex         == classIndex &&
                    fieldrefConstant.u2nameAndTypeIndex   == nameAndTypeIndex)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass,
                           new FieldrefConstant(classIndex,
                                                nameAndTypeIndex,
                                                referencedClass,
                                                referencedMember));
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class name, method name, and descriptor, in the given class.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(ProgramClass programClass,
                                             String       className,
                                             String       name,
                                             String       descriptor,
                                             Clazz        referencedClass,
                                             Member       referencedMember)
    {
        return addInterfaceMethodrefConstant(programClass,
                                             className,
                                             addNameAndTypeConstant(programClass,
                                                                    name,
                                                                    descriptor),
                                                                    referencedClass,
                                                                    referencedMember);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class name, method name, and descriptor, in the given class.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(ProgramClass programClass,
                                             String       className,
                                             int          nameAndTypeIndex,
                                             Clazz        referencedClass,
                                             Member       referencedMember)
    {
        return addInterfaceMethodrefConstant(programClass,
                                             addClassConstant(programClass,
                                                              className,
                                                              referencedClass),
                                                              nameAndTypeIndex,
                                                              referencedClass,
                                                              referencedMember);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry for the
     * given class and method, in the given class.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(ProgramClass programClass,
                                             Clazz        referencedClass,
                                             Member       referencedMember)
    {
        return addInterfaceMethodrefConstant(programClass,
                                             referencedClass.getName(),
                                             referencedMember.getName(referencedClass),
                                             referencedMember.getDescriptor(referencedClass),
                                             referencedClass,
                                             referencedMember);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class constant pool entry index, method name, and descriptor, in
     * the given class.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(ProgramClass programClass,
                                             int          classIndex,
                                             String       name,
                                             String       descriptor,
                                             Clazz        referencedClass,
                                             Member       referencedMember)
    {
        return addInterfaceMethodrefConstant(programClass,
                                             classIndex,
                                             addNameAndTypeConstant(programClass,
                                                                    name,
                                                                    descriptor),
                                             referencedClass,
                                             referencedMember);
    }


    /**
     * Finds or creates a InterfaceMethodrefConstant constant pool entry with the
     * given class constant pool entry index and name and type constant pool
     * entry index the given class.
     * @return the constant pool index of the InterfaceMethodrefConstant.
     */
    public int addInterfaceMethodrefConstant(ProgramClass programClass,
                                             int          classIndex,
                                             int          nameAndTypeIndex,
                                             Clazz        referencedClass,
                                             Member       referencedMember)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                            constant.getTag() == ClassConstants.CONSTANT_InterfaceMethodref)
            {
                InterfaceMethodrefConstant methodrefConstant = (InterfaceMethodrefConstant)constant;
                if (methodrefConstant.u2classIndex       == classIndex &&
                                methodrefConstant.u2nameAndTypeIndex ==   nameAndTypeIndex)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass,
                           new InterfaceMethodrefConstant(classIndex,
                                                          nameAndTypeIndex,
                                                          referencedClass,
                                                          referencedMember));
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry for the given
     * class and method, in the given class.
     * @return the constant pool index of   the MethodrefConstant.
     */
    public int addMethodrefConstant(ProgramClass programClass,
                                    Clazz        referencedClass,
                                    Member       referencedMember)
    {
        return addMethodrefConstant(programClass,
                                    referencedClass.getName(),
                                    referencedMember.getName(referencedClass),
                                    referencedMember.getDescriptor(referencedClass),
                                    referencedClass,
                                    referencedMember);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class name, method name, and descriptor, in the given class.
     * @return the constant pool index of   the MethodrefConstant.
     */
    public int addMethodrefConstant(ProgramClass programClass,
                                    String       className,
                                    String       name,
                                    String       descriptor,
                                    Clazz        referencedClass,
                                    Member       referencedMember)
    {
        return addMethodrefConstant(programClass,
                                    className,
                                    addNameAndTypeConstant(programClass,
                                                           name,
                                                           descriptor),
                                    referencedClass,
                                    referencedMember);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class name, method name, and descriptor, in the given class.
     * @return the constant pool index of   the MethodrefConstant.
     */
    public int addMethodrefConstant(ProgramClass programClass,
                                    String       className,
                                    int          nameAndTypeIndex,
                                    Clazz        referencedClass,
                                    Member       referencedMember)
    {
        return addMethodrefConstant(programClass,
                                    addClassConstant(programClass,
                                                     className,
                                                     referencedClass),
                                    nameAndTypeIndex,
                                    referencedClass,
                                    referencedMember);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class constant pool entry index, method name, and descriptor, in the
     * given class.
     * @return the constant pool index of   the MethodrefConstant.
     */
    public int addMethodrefConstant(ProgramClass programClass,
                                    int          classIndex,
                                    String       name,
                                    String       descriptor,
                                    Clazz        referencedClass,
                                    Member       referencedMember)
    {
        return addMethodrefConstant(programClass,
                                    classIndex,
                                    addNameAndTypeConstant(programClass,
                                                           name,
                                                           descriptor),
                                    referencedClass,
                                    referencedMember);
    }


    /**
     * Finds or creates a MethodrefConstant constant pool entry with the given
     * class constant pool entry index and name and type constant pool entry index
     * the given class.
     * @return the constant pool index of the MethodrefConstant.
     */
    public int addMethodrefConstant(ProgramClass programClass,
                                    int          classIndex,
                                    int          nameAndTypeIndex,
                                    Clazz        referencedClass,
                                    Member       referencedMember)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Methodref)
            {
                MethodrefConstant methodrefConstant = (MethodrefConstant)constant;
                if (methodrefConstant.u2classIndex         == classIndex &&
                    methodrefConstant.u2nameAndTypeIndex   == nameAndTypeIndex)
                {
                    return index;
                }
            }
        }

        return addConstant(programClass,
                           new MethodrefConstant(classIndex,
                                                 nameAndTypeIndex,
                                                 referencedClass,
                                                 referencedMember));
    }


    /**
     * Finds or creates a ClassConstant constant pool entry for the given class,
     * in the given class.
     * @return the constant pool index of the ClassConstant.
     */
    public int addClassConstant(ProgramClass programClass,
                                Clazz        referencedClass)
    {
        return addClassConstant(programClass,
                                referencedClass.getName(),
                                referencedClass);
    }


    /**
     * Finds or creates a ClassConstant constant pool entry with the given name,
     * in the given class.
     * @return the constant pool index of the ClassConstant.
     */
    public int addClassConstant(ProgramClass programClass,
                                String       name,
                                Clazz        referencedClass)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Class)
            {
                ClassConstant classConstant = (ClassConstant)constant;
                if (classConstant.getName(programClass).equals(name))
                {
                    return index;
                }
            }
        }

        int nameIndex = addUtf8Constant(programClass, name);

        return addConstant(programClass,
                           new ClassConstant(nameIndex,
                                             referencedClass));
    }


    /**
     * Finds or creates a NameAndTypeConstant constant pool entry with the given
     * name and type, in the given class.
     * @return the constant pool index of the NameAndTypeConstant.
     */
    public int addNameAndTypeConstant(ProgramClass programClass,
                                      String       name,
                                      String       type)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_NameAndType)
            {
                NameAndTypeConstant nameAndTypeConstant = (NameAndTypeConstant)constant;
                if (nameAndTypeConstant.getName(programClass).equals(name) &&
                    nameAndTypeConstant.getType(programClass).equals(type))
                {
                    return index;
                }
            }
        }

        int nameIndex       = addUtf8Constant(programClass, name);
        int descriptorIndex = addUtf8Constant(programClass, type);

        return addConstant(programClass,
                           new NameAndTypeConstant(nameIndex,
                                                   descriptorIndex));
    }


    /**
     * Finds or creates a Utf8Constant constant pool entry for the given string,
     * in the given class.
     * @return the constant pool index of the   Utf8Constant.
     */
    public int addUtf8Constant(ProgramClass programClass,
                               String       string)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Check if the entry already exists.
        for (int index = 1; index < constantPoolCount; index++)
        {
            Constant constant = constantPool[index];

            if (constant != null &&
                constant.getTag() == ClassConstants.CONSTANT_Utf8)
            {
                Utf8Constant utf8Constant = (Utf8Constant)constant;
                if (utf8Constant.getString().equals(string))
                {
                    return index;
                }
            }
        }

        return addConstant(programClass, new Utf8Constant(string));
    }


    /**
     * Adds a given constant pool entry to the end of the constant pool
     * in the given class.
     * @return the constant pool index for the added entry.
     */
    public int addConstant(ProgramClass programClass,
                           Constant     constant)
    {
        int        constantPoolCount = programClass.u2constantPoolCount;
        Constant[] constantPool      = programClass.constantPool;

        // Make sure there is enough space for another constant pool entry.
        if (constantPool.length < constantPoolCount+2)
        {
            programClass.constantPool = new Constant[constantPoolCount+2];
            System.arraycopy(constantPool, 0,
                             programClass.constantPool, 0,
                             constantPoolCount);
            constantPool = programClass.constantPool;
        }

        if (DEBUG)
        {
            System.out.println(programClass.getName()+": adding ["+constant.getClass().getName()+"] at index "+programClass.u2constantPoolCount);
        }

        // Create a new Utf8Constant for the given string.
        constantPool[programClass.u2constantPoolCount++] = constant;

        // Long constants and double constants take up two entries in the
        // constant pool.
        int tag = constant.getTag();
        if (tag == ClassConstants.CONSTANT_Long ||
            tag == ClassConstants.CONSTANT_Double)
        {
            constantPool[programClass.u2constantPoolCount++] = null;
        }

        return constantPoolCount;
    }
}
