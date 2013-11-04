/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile;

import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.ClassSubHierarchyInitializer;
import proguard.classfile.visitor.*;

/**
 * This Clazz is a complete representation of the data in a Java class.
 *
 * @author Eric Lafortune
 */
public class ProgramClass implements Clazz
{
    public int             u4magic;
    public int             u4version;
    public int             u2constantPoolCount;
    public Constant[]      constantPool;
    public int             u2accessFlags;
    public int             u2thisClass;
    public int             u2superClass;
    public int             u2interfacesCount;
    public int[]           u2interfaces;
    public int             u2fieldsCount;
    public ProgramField[]  fields;
    public int             u2methodsCount;
    public ProgramMethod[] methods;
    public int             u2attributesCount;
    public Attribute[]     attributes;

    /**
     * An extra field pointing to the subclasses of this class.
     * This field is filled out by the {@link ClassSubHierarchyInitializer}.
     */
    public Clazz[] subClasses;

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    /**
     * Creates an uninitialized ProgramClass.
     */
    public ProgramClass() {}


    /**
     * Returns the Constant at the given index in the constant pool.
     */
    public Constant getConstant(int constantIndex)
    {
        return constantPool[constantIndex];
    }


    // Implementations for Clazz.

    public int getAccessFlags()
    {
        return u2accessFlags;
    }

    public String getName()
    {
        return getClassName(u2thisClass);
    }

    public String getSuperName()
    {
        return u2superClass == 0 ? null : getClassName(u2superClass);
    }

    public int getInterfaceCount()
    {
        return u2interfacesCount;
    }

    public String getInterfaceName(int index)
    {
        return getClassName(u2interfaces[index]);
    }

    public int getTag(int constantIndex)
    {
        return constantPool[constantIndex].getTag();
    }

    public String getString(int constantIndex)
    {
        try
        {
            return ((Utf8Constant)constantPool[constantIndex]).getString();
        }
        catch (ClassCastException ex)
        {
            new ClassPrinter().visitProgramClass(this);
            throw new ClassCastException("Expected Utf8Constant at index ["+constantIndex+"] in class ["+getName()+"], found ["+ex.getMessage()+"]");
        }
    }

    public String getStringString(int constantIndex)
    {
        try
        {
            return ((StringConstant)constantPool[constantIndex]).getString(this);
        }
        catch (ClassCastException ex)
        {
            throw new ClassCastException("Expected StringConstant at index ["+constantIndex+"] in class ["+getName()+"], found ["+ex.getMessage()+"]");
        }
    }

    public String getClassName(int constantIndex)
    {
        try
        {
            return ((ClassConstant)constantPool[constantIndex]).getName(this);
        }
        catch (ClassCastException ex)
        {
            throw new ClassCastException("Expected ClassConstant at index ["+constantIndex+"] in class ["+getName()+"], found ["+ex.getMessage()+"]");
        }
    }

    public String getName(int constantIndex)
    {
        try
        {
            return ((NameAndTypeConstant)constantPool[constantIndex]).getName(this);
        }
        catch (ClassCastException ex)
        {
            throw new ClassCastException("Expected NameAndTypeConstant at index ["+constantIndex+"] in class ["+getName()+"], found ["+ex.getMessage()+"]");
        }
    }

    public String getType(int constantIndex)
    {
        try
        {
            return ((NameAndTypeConstant)constantPool[constantIndex]).getType(this);
        }
        catch (ClassCastException ex)
        {
            throw new ClassCastException("Expected NameAndTypeConstant at index ["+constantIndex+"] in class ["+getName()+"], found ["+ex.getMessage()+"]");
        }
    }


    public void addSubClass(Clazz clazz)
    {
        if (subClasses == null)
        {
            subClasses = new Clazz[1];
        }
        else
        {
            // Copy the old elements into new larger array.
            Clazz[] temp = new Clazz[subClasses.length+1];
            System.arraycopy(subClasses, 0, temp, 0, subClasses.length);
            subClasses = temp;
        }

        subClasses[subClasses.length-1] = clazz;
    }


    public Clazz getSuperClass()
    {
        return u2superClass != 0 ?
            ((ClassConstant)constantPool[u2superClass]).referencedClass :
            null;
    }


    public Clazz getInterface(int index)
    {
        return ((ClassConstant)constantPool[u2interfaces[index]]).referencedClass;
    }


    public boolean extends_(Clazz clazz)
    {
        if (this.equals(clazz))
        {
            return true;
        }

        Clazz superClass = getSuperClass();
        return superClass != null &&
               superClass.extends_(clazz);
    }


    public boolean extendsOrImplements(Clazz clazz)
    {
        if (this.equals(clazz))
        {
            return true;
        }

        Clazz superClass = getSuperClass();
        if (superClass != null &&
            superClass.extendsOrImplements(clazz))
        {
            return true;
        }

        for (int index = 0; index < u2interfacesCount; index++)
        {
            Clazz interfaceClass = getInterface(index);
            if (interfaceClass != null &&
                interfaceClass.extendsOrImplements(clazz))
            {
                return true;
            }
        }

        return false;
    }


    public Field findField(String name, String descriptor)
    {
        for (int index = 0; index < u2fieldsCount; index++)
        {
            Field field = fields[index];
            if ((name       == null || field.getName(this).equals(name)) &&
                (descriptor == null || field.getDescriptor(this).equals(descriptor)))
            {
                return field;
            }
        }

        return null;
    }


    public Method findMethod(String name, String descriptor)
    {
        for (int index = 0; index < u2methodsCount; index++)
        {
            Method method = methods[index];
            if ((name       == null || method.getName(this).equals(name)) &&
                (descriptor == null || method.getDescriptor(this).equals(descriptor)))
            {
                return method;
            }
        }

        return null;
    }


    public void accept(ClassVisitor classVisitor)
    {
        classVisitor.visitProgramClass(this);
    }


    public void hierarchyAccept(boolean      visitThisClass,
                                boolean      visitSuperClass,
                                boolean      visitInterfaces,
                                boolean      visitSubclasses,
                                ClassVisitor classVisitor)
    {
        // First visit the current classfile.
        if (visitThisClass)
        {
            accept(classVisitor);
        }

        // Then visit its superclass, recursively.
        if (visitSuperClass)
        {
            Clazz superClass = getSuperClass();
            if (superClass != null)
            {
                superClass.hierarchyAccept(true,
                                           true,
                                           visitInterfaces,
                                           false,
                                           classVisitor);
            }
        }

        // Then visit its interfaces, recursively.
        if (visitInterfaces)
        {
            for (int index = 0; index < u2interfacesCount; index++)
            {
                Clazz interfaceClass = getInterface(index);
                if (interfaceClass != null)
                {
                    interfaceClass.hierarchyAccept(true,
                                                   true,
                                                   true,
                                                   false,
                                                   classVisitor);
                }
            }
        }

        // Then visit its subclasses, recursively.
        if (visitSubclasses)
        {
            if (subClasses != null)
            {
                for (int index = 0; index < subClasses.length; index++)
                {
                    Clazz subClass = subClasses[index];
                    subClass.hierarchyAccept(true,
                                             false,
                                             false,
                                             true,
                                             classVisitor);
                }
            }
        }
    }


    public void constantPoolEntriesAccept(ConstantVisitor constantVisitor)
    {
        for (int index = 1; index < u2constantPoolCount; index++)
        {
            if (constantPool[index] != null)
            {
                constantPool[index].accept(this, constantVisitor);
            }
        }
    }


    public void constantPoolEntryAccept(int index, ConstantVisitor constantVisitor)
    {
        constantPool[index].accept(this, constantVisitor);
    }


    public void fieldsAccept(MemberVisitor memberVisitor)
    {
        for (int index = 0; index < u2fieldsCount; index++)
        {
            fields[index].accept(this, memberVisitor);
        }
    }


    public void fieldAccept(String name, String descriptor, MemberVisitor memberVisitor)
    {
        Field field = findField(name, descriptor);
        if (field != null)
        {
            field.accept(this, memberVisitor);
        }
    }


    public void methodsAccept(MemberVisitor memberVisitor)
    {
        for (int index = 0; index < u2methodsCount; index++)
        {
            methods[index].accept(this, memberVisitor);
        }
    }


    public void methodAccept(String name, String descriptor, MemberVisitor memberVisitor)
    {
        Method method = findMethod(name, descriptor);
        if (method != null)
        {
            method.accept(this, memberVisitor);
        }
    }


    public boolean mayHaveImplementations(Method method)
    {
        return
            (u2accessFlags & ClassConstants.INTERNAL_ACC_FINAL) == 0 &&
            (method == null ||
             ((method.getAccessFlags() & (ClassConstants.INTERNAL_ACC_PRIVATE |
                                          ClassConstants.INTERNAL_ACC_STATIC  |
                                          ClassConstants.INTERNAL_ACC_FINAL)) == 0 &&
              !method.getName(this).equals(ClassConstants.INTERNAL_METHOD_NAME_INIT)));
    }


    private boolean isSpecial(Method method)
    {
        return
            (method.getAccessFlags() & (ClassConstants.INTERNAL_ACC_PRIVATE |
                                        ClassConstants.INTERNAL_ACC_STATIC)) != 0 ||
            method.getName(this).equals(ClassConstants.INTERNAL_METHOD_NAME_INIT);
    }


    public void methodImplementationsAccept(Method        method,
                                            boolean       visitThisMethod,
                                            MemberVisitor memberVisitor)
    {
        methodImplementationsAccept(method.getName(this),
                                    method.getDescriptor(this),
                                    method,
                                    visitThisMethod,
                                    true,
                                    true,
                                    true,
                                    memberVisitor);
    }


    public void methodImplementationsAccept(String        name,
                                            String        descriptor,
                                            boolean       visitThisMethod,
                                            MemberVisitor memberVisitor)
    {
        methodImplementationsAccept(name,
                                    descriptor,
                                    visitThisMethod,
                                    true,
                                    true,
                                    true,
                                    memberVisitor);
    }


    public void methodImplementationsAccept(String        name,
                                            String        descriptor,
                                            boolean       visitThisMethod,
                                            boolean       visitSpecialMethods,
                                            boolean       visitSuperMethods,
                                            boolean       visitOverridingMethods,
                                            MemberVisitor memberVisitor)
    {
        methodImplementationsAccept(name,
                                    descriptor,
                                    findMethod(name, descriptor),
                                    visitThisMethod,
                                    visitSpecialMethods,
                                    visitSuperMethods,
                                    visitOverridingMethods,
                                    memberVisitor);
    }


    public void methodImplementationsAccept(String        name,
                                            String        descriptor,
                                            Method        method,
                                            boolean       visitThisMethod,
                                            boolean       visitSpecialMethods,
                                            boolean       visitSuperMethods,
                                            boolean       visitOverridingMethods,
                                            MemberVisitor memberVisitor)
    {
        // Do we have the method in this class?
        if (method != null)
        {
            // Is it a special method?
            if (isSpecial(method))
            {
                // Visit the special method in this class, if allowed.
                if (visitSpecialMethods)
                {
                    method.accept(this, memberVisitor);

                    // The method can't have any other implementations.
                    return;
                }
            }
            else
            {
                // Visit the method in this class, if allowed.
                if (visitThisMethod)
                {
                    method.accept(this, memberVisitor);
                }

                // We don't have to look in subclasses if there can't be
                // any overriding implementations.
                if (!mayHaveImplementations(method))
                {
                    visitOverridingMethods = false;
                }

                // We don't have to look in superclasses if we have a concrete
                // implementation here.
                if ((method.getAccessFlags() & ClassConstants.INTERNAL_ACC_ABSTRACT) == 0)
                {
                    visitSuperMethods = false;
                }
            }
        }

        // Then visit the method in its subclasses, recursively.
        if (visitOverridingMethods)
        {
            // Go looking for implementations in all of the subclasses.
            if (subClasses != null)
            {
                for (int index = 0; index < subClasses.length; index++)
                {
                    Clazz subClass = subClasses[index];
                    subClass.methodImplementationsAccept(name,
                                                         descriptor,
                                                         true,
                                                         false,
                                                         visitSuperMethods,
                                                         true,
                                                         memberVisitor);
                }
            }

            // We don't have to look in superclasses right away if we dont't
            // have a concrete class here.
            if ((u2accessFlags & (ClassConstants.INTERNAL_ACC_INTERFACE |
                                  ClassConstants.INTERNAL_ACC_ABSTRACT)) != 0)
            {
                visitSuperMethods = false;
            }
        }

        // Then visit the method in its superclass, recursively.
        if (visitSuperMethods)
        {
            Clazz superClass = getSuperClass();
            if (superClass != null)
            {
                superClass.methodImplementationsAccept(name,
                                                       descriptor,
                                                       true,
                                                       false,
                                                       true,
                                                       false,
                                                       memberVisitor);
            }
        }
    }


    public void attributesAccept(AttributeVisitor attributeVisitor)
    {
        for (int index = 0; index < u2attributesCount; index++)
        {
            attributes[index].accept(this, attributeVisitor);
        }
    }


    // Implementations for VisitorAccepter.

    public Object getVisitorInfo()
    {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
