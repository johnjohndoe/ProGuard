/* $Id: LibraryClassFile.java,v 1.22 2003/12/06 22:15:38 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 1999      Mark Welsh (markw@retrologic.com)
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
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


import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.io.*;

/**
 * This is a compact representation of the essential data in a Java class file.
 * A LibraryClassFile instance representing a *.class file can be generated
 * using the static create(DataInput) method, but not persisted back.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class LibraryClassFile implements ClassFile
{
    // Some objects and arrays that can be reused.
    private static LibraryClassFile    reusableLibraryClassFile;
    private static CpInfo[]            reusableConstantPool;
    private static LibraryFieldInfo[]  reusableFields;
    private static LibraryMethodInfo[] reusableMethods;


    public int                 u2accessFlags;
    public String              thisClassName;
    public String              superClassName;
    public String[]            interfaceNames;
    public LibraryFieldInfo[]  fields;
    public LibraryMethodInfo[] methods;

    /**
     * An extra field pointing to the superclass of this class.
     * This field is filled out by the <code>{@link
     * proguard.classfile.util.ClassFileInitializer ClassFileInitializer}</code>.
     */
    public ClassFile   superClass          = null;

    /**
     * An extra field pointing to the interfaces of this class.
     * This field is filled out by the <code>{@link
     * proguard.classfile.util.ClassFileInitializer ClassFileInitializer}</code>.
     */
    public ClassFile[] interfaceClasses    = null;

    /**
     * An extra field pointing to the subclasses of this class.
     * This field is filled out by the <code>{@link
     * proguard.classfile.util.ClassFileInitializer ClassFileInitializer}</code>.
     */
    public ClassFile[] subClasses          = null;

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    /**
     * Creates a new LibraryClassFile from the class file format data in the DataInput
     * stream. If specified, this method may return <code>null</code> if the
     * class file is not visible.
     *
     * @throws IOException if the class file is corrupt or incomplete
     */
    public static LibraryClassFile create(DataInput din, boolean skipNonPublic)
    throws IOException
    {
        // See if we have to create a new library class file object.
        if (reusableLibraryClassFile == null)
        {
            reusableLibraryClassFile = new LibraryClassFile();
        }

        // We'll start using the reusable object.
        LibraryClassFile libraryClassFile = reusableLibraryClassFile;

        libraryClassFile.read(din, skipNonPublic);

        // Did we actually read a useful library class file?
        if (libraryClassFile.thisClassName != null)
        {
            // We can't reuse this library class file object next time.
            reusableLibraryClassFile = null;
        }
        else
        {
            // We don't have a useful library class file to return.
            libraryClassFile = null;
        }

        return libraryClassFile;
    }


    // Private constructor.
    private LibraryClassFile() {}

    // Import the class data to internal representation.
    private void read(DataInput din, boolean skipNonPublic) throws IOException
    {
        // Read and check the class file magic number.
        int u4magic = din.readInt();
        ClassUtil.checkMagicNumber(u4magic);

        // Read and check the class file version numbers.
        int u2minorVersion = din.readUnsignedShort();
        int u2majorVersion = din.readUnsignedShort();
        ClassUtil.checkVersionNumbers(u2majorVersion, u2minorVersion);

        // Read the constant pool.
        int u2constantPoolCount = din.readUnsignedShort();

        // Make sure there's sufficient space in the reused constant pool array.
        if (reusableConstantPool == null ||
            reusableConstantPool.length < u2constantPoolCount)
        {
            reusableConstantPool = new CpInfo[u2constantPoolCount];
        }

        // Fill the constant pool. The zero entry is not used, nor are the
        // entries following a Long or Double.
        for (int i = 1; i < u2constantPoolCount; i++)
        {
            reusableConstantPool[i] = CpInfo.createOrShare(din);
            int tag = reusableConstantPool[i].getTag();
            if (tag == ClassConstants.CONSTANT_Long ||
                tag == ClassConstants.CONSTANT_Double)
            {
                i++;
            }
        }

        u2accessFlags = din.readUnsignedShort();

        // We may stop parsing this library class file if it's not public anyway.
        // E.g. only about 60% of all rt.jar classes need to be parsed.
        if (skipNonPublic && !isVisible())
        {
            return;
        }

        // Read the class and super class indices.
        int u2thisClass  = din.readUnsignedShort();
        int u2superClass = din.readUnsignedShort();

        // Store their actual names.
        thisClassName  = toName(reusableConstantPool, u2thisClass);
        superClassName = (u2superClass == 0) ? null :
                         toName(reusableConstantPool, u2superClass);

        // Read the interface indices.
        int u2interfacesCount = din.readUnsignedShort();

        // Store their actual names.
        interfaceNames = new String[u2interfacesCount];
        for (int i = 0; i < u2interfacesCount; i++)
        {
            int u2interface = din.readUnsignedShort();
            interfaceNames[i] = toName(reusableConstantPool, u2interface);
        }

        // Read the fields.
        int u2fieldsCount = din.readUnsignedShort();

        // Make sure there's sufficient space in the reused fields array.
        if (reusableFields == null ||
            reusableFields.length < u2fieldsCount)
        {
            reusableFields = new LibraryFieldInfo[u2fieldsCount];
        }

        int visibleFieldsCount = 0;
        for (int i = 0; i < u2fieldsCount; i++)
        {
            LibraryFieldInfo field = LibraryFieldInfo.create(din, reusableConstantPool);

            // Only store fields that are visible.
            if (field.isVisible())
            {
                reusableFields[visibleFieldsCount++] = field;
            }
        }

        // Copy the visible fields into a fields array of the right size.
        fields = new LibraryFieldInfo[visibleFieldsCount];
        System.arraycopy(reusableFields, 0, fields, 0, visibleFieldsCount);


        // Read the methods.
        int u2methodsCount = din.readUnsignedShort();

        // Make sure there's sufficient space in the reused methods array.
        if (reusableMethods == null ||
            reusableMethods.length < u2methodsCount)
        {
            reusableMethods = new LibraryMethodInfo[u2methodsCount];
        }

        int visibleMethodsCount = 0;
        for (int i = 0; i < u2methodsCount; i++)
        {
            LibraryMethodInfo method = LibraryMethodInfo.create(din, reusableConstantPool);

            // Only store methods that are visible.
            if (method.isVisible())
            {
                reusableMethods[visibleMethodsCount++] = method;
            }
        }

        // Copy the visible methods into a methods array of the right size.
        methods = new LibraryMethodInfo[visibleMethodsCount];
        System.arraycopy(reusableMethods, 0, methods, 0, visibleMethodsCount);


        // Skip the attributes.
        int u2attributesCount = din.readUnsignedShort();
        for (int i = 0; i < u2attributesCount; i++)
        {
            LibraryAttrInfo.skip(din);
        }
    }


    /**
     * Returns whether this library class file is visible to the outside world.
     */
    boolean isVisible()
    {
        return (u2accessFlags & ClassConstants.INTERNAL_ACC_PUBLIC) != 0;
    }


    /**
     * Returns the class name of the ClassCpInfo at the specified index in the
     * given constant pool.
     */
    private String toName(CpInfo[] constantPool, int cpIndex)
    {
        ClassCpInfo classEntry = (ClassCpInfo)constantPool[cpIndex];
        Utf8CpInfo  nameEntry  = (Utf8CpInfo)constantPool[classEntry.getNameIndex()];

        return nameEntry.getString();
    }


    /**
     * Returns the field with the given name and descriptor.
     */
    private LibraryFieldInfo findLibraryField(String name, String descriptor)
    {
        for (int i = 0; i < fields.length; i++)
        {
            LibraryFieldInfo field = fields[i];
            if (field != null &&
                field.getName(this).equals(name) &&
                field.getDescriptor(this).equals(descriptor))
                return field;
        }

        return null;
    }


    /**
     * Returns the method with the given name and descriptor.
     */
    private LibraryMethodInfo findLibraryMethod(String name, String descriptor)
    {
        for (int i = 0; i < methods.length; i++)
        {
            LibraryMethodInfo method = methods[i];
            if (method != null &&
                method.getName(this).equals(name) &&
                method.getDescriptor(this).equals(descriptor))
                return method;
        }

        return null;
    }


    // Implementations for ClassFile.

    public int getAccessFlags()
    {
        return u2accessFlags;
    }

    public String getName()
    {
        return thisClassName;
    }

    public String getSuperName()
    {
        // This may be java/lang/Object, in which case there is no super.
        return superClassName;
    }

    public int getCpTag(int cpIndex)
    {
        return -1;
    }

    public String getCpString(int cpIndex)
    {
        return null;
    }

    public String getCpClassNameString(int cpIndex)
    {
        return null;
    }

    public String getCpNameString(int cpIndex)
    {
        return null;
    }

    public String getCpTypeString(int cpIndex)
    {
        return null;
    }


    public void addSubClass(ClassFile classFile)
    {
        if (subClasses == null)
        {
            subClasses = new ClassFile[1];
        }
        else
        {
            // Copy the old elements into new larger array.
            ClassFile[] temp = new ClassFile[subClasses.length+1];
            System.arraycopy(subClasses, 0, temp, 0, subClasses.length);
            subClasses = temp;
        }

        subClasses[subClasses.length-1] = classFile;
    }


    public ClassFile getSuperClass()
    {
        return superClass;
    }


    public FieldInfo findField(String name, String descriptor)
    {
        return findLibraryField(name, descriptor);
    }

    public MethodInfo findMethod(String name, String descriptor)
    {
        return findLibraryMethod(name, descriptor);
    }


    public void accept(ClassFileVisitor classFileVisitor)
    {
        classFileVisitor.visitLibraryClassFile(this);
    }

    public void constantPoolEntriesAccept(CpInfoVisitor cpInfoVisitor)
    {
        // This class doesn't keep references to its constant pool entries.
    }

    public void constantPoolEntryAccept(CpInfoVisitor cpInfoVisitor, int index)
    {
        // This class doesn't keep references to its constant pool entries.
    }

    public void fieldsAccept(MemberInfoVisitor memberInfoVisitor)
    {
        for (int i = 0; i < fields.length; i++)
        {
            if (fields[i] != null)
            {
                fields[i].accept(this, memberInfoVisitor);
            }
        }
    }

    public void fieldAccept(MemberInfoVisitor memberInfoVisitor, String name, String descriptor)
    {
        LibraryMemberInfo libraryMemberInfo = findLibraryField(name, descriptor);
        if (libraryMemberInfo != null)
        {
            libraryMemberInfo.accept(this, memberInfoVisitor);
        }
    }

    public void methodsAccept(MemberInfoVisitor memberInfoVisitor)
    {
        for (int i = 0; i < methods.length; i++)
        {
            if (methods[i] != null)
            {
                methods[i].accept(this, memberInfoVisitor);
            }
        }
    }

    public void methodAccept(MemberInfoVisitor memberInfoVisitor, String name, String descriptor)
    {
        LibraryMemberInfo libraryMemberInfo = findLibraryMethod(name, descriptor);
        if (libraryMemberInfo != null)
        {
            libraryMemberInfo.accept(this, memberInfoVisitor);
        }
    }

    public void attributesAccept(AttrInfoVisitor attrInfoVisitor)
    {
        // This class doesn't keep references to its attributes.
    }


    // Implementations for VisitorAccepter.

    public Object getVisitorInfo() {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
