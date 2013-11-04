/* $Id: LibraryClassFile.java,v 1.15 2002/07/04 16:16:58 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 1999 Mark Welsh (markw@retrologic.com)
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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


import proguard.classfile.visitor.*;
import java.io.*;
import java.util.*;

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
     * stream.
     *
     * @throws IOException if class file is corrupt or incomplete
     */
    public static LibraryClassFile create(DataInput din) throws IOException
    {
        LibraryClassFile cf = new LibraryClassFile();
        cf.read(din);
        return cf;
    }


    // Private constructor.
    private LibraryClassFile() {}

    // Import the class data to internal representation.
    private void read(DataInput din) throws IOException
    {
        // Read the class file
        int u4magic        = din.readInt();
        int u2minorVersion = din.readUnsignedShort();
        int u2majorVersion = din.readUnsignedShort();

        // Check this is a valid classfile that we can handle
        if (u4magic != ClassConstants.MAGIC)
        {
            throw new IOException("Invalid magic number in class file.");
        }
        if (u2majorVersion < ClassConstants.MAJOR_VERSION_MIN ||
            (u2majorVersion == ClassConstants.MAJOR_VERSION_MIN &&
             u2minorVersion <  ClassConstants.MINOR_VERSION_MIN) ||
            (u2majorVersion == ClassConstants.MAJOR_VERSION_MAX &&
             u2minorVersion >  ClassConstants.MINOR_VERSION_MAX) ||
            u2majorVersion > ClassConstants.MAJOR_VERSION_MAX)
        {
            throw new IOException("Unsupported version number ["+u2majorVersion+"."+u2minorVersion+"] for class file format.");
        }

        int      u2constantPoolCount = din.readUnsignedShort();
        CpInfo[] constantPool        = new CpInfo[u2constantPoolCount];

        // Fill the constant pool. The zero entry is not used, nor are the
        // entries following a Long or Double.
        for (int i = 1; i < u2constantPoolCount; i++)
        {
            constantPool[i] = CpInfo.create(din);
            if ((constantPool[i] instanceof LongCpInfo) ||
                (constantPool[i] instanceof DoubleCpInfo))
            {
                i++;
            }
        }

            u2accessFlags = din.readUnsignedShort();
        int u2thisClass   = din.readUnsignedShort();
        int u2superClass  = din.readUnsignedShort();

        // Store the actual fields.

        thisClassName  = toName(constantPool, u2thisClass);
        superClassName = (u2superClass == 0) ? null :
                         toName(constantPool, u2superClass);

        int u2interfacesCount = din.readUnsignedShort();

        interfaceNames = new String[u2interfacesCount];

        for (int i = 0; i < u2interfacesCount; i++)
        {
            int u2interface = din.readUnsignedShort();
            interfaceNames[i] = toName(constantPool, u2interface);
        }

        int u2fieldsCount = din.readUnsignedShort();
        fields = new LibraryFieldInfo[u2fieldsCount];
        for (int i = 0; i < u2fieldsCount; i++)
        {
            LibraryFieldInfo field = LibraryFieldInfo.create(din, constantPool);

            // Only store fields that are visible.
            if ((field.u2accessFlags & ClassConstants.INTERNAL_ACC_PRIVATE) == 0)
            {
                fields[i] = field;
            }
        }

        int u2methodsCount = din.readUnsignedShort();
        methods = new LibraryMethodInfo[u2methodsCount];
        for (int i = 0; i < u2methodsCount; i++)
        {
            LibraryMethodInfo method = LibraryMethodInfo.create(din, constantPool);

            // Only store methods that are visible.
            if ((method.u2accessFlags & ClassConstants.INTERNAL_ACC_PRIVATE) == 0)
            {
                methods[i] = method;
            }
        }

        int u2attributesCount = din.readUnsignedShort();
        for (int i = 0; i < u2attributesCount; i++)
        {
            LibraryAttrInfo.skip(din);
        }
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


    // Implementations for ClassFile

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


    // Implementations for VisitorAccepter

    public Object getVisitorInfo() {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
