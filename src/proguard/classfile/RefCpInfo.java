/* $Id: RefCpInfo.java,v 1.10 2002/05/23 21:21:12 eric Exp $
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

import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.io.*;
import java.util.*;

/**
 * Representation of a 'ref'-type entry in the ConstantPool.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
abstract public class RefCpInfo extends CpInfo
{
    public int u2classIndex;
    public int u2nameAndTypeIndex;

    /**
     * An extra field pointing to the referenced ProgramClassFile object.
     * This field is filled out by the <code>{@link
     * proguard.classfile.util.ClassFileInitializer ClassFileInitializer}</code>.
     * References to library classes are left blank (<code>null</code>).
     */
    public ProgramClassFile referencedClassFile;

    /**
     * An extra field optionally pointing to the referenced ProgramMemberInfo object.
     * This field is filled out by the <code>{@link
     * proguard.classfile.util.ClassFileInitializer ClassFileInitializer}</code>.
     * References to library class members are left blank (<code>null</code>).
     */
    public ProgramMemberInfo referencedMemberInfo;


    protected RefCpInfo(int tag)
    {
        super(tag);
    }


    /**
     * Lets the referenced class file accept the given visitor.
     */
    public void referencedClassAccept(ClassFileVisitor classFileVisitor)
    {
        if (referencedClassFile != null)
        {
            referencedClassFile.accept(classFileVisitor);
        }
    }


    /**
     * Lets the referenced class member accept the given visitor.
     */
    public void referencedMemberInfoAccept(MemberInfoVisitor memberInfoVisitor)
    {
        if (referencedMemberInfo != null)
        {
            referencedMemberInfo.accept(referencedClassFile,
                                        memberInfoVisitor);
        }
    }


    /**
     * Returns the class index.
     */
    public int getClassIndex()
    {
        return u2classIndex;
    }

    /**
     * Returns the name-and-type index.
     */
    public int getNameAndTypeIndex()
    {
        return u2nameAndTypeIndex;
    }

    /**
     * Sets the name-and-type index.
     */
    public void setNameAndTypeIndex(int index)
    {
        u2nameAndTypeIndex = index;
    }

    /**
     * Returns the class name.
     */
    public String getClassName(ClassFile classFile)
    {
        return classFile.getCpClassNameString(u2classIndex);
    }

    /**
     * Returns the method/field name.
     */
    public String getName(ClassFile classFile)
    {
        return classFile.getCpNameString(u2nameAndTypeIndex);
    }

    /**
     * Returns the type.
     */
    public String getType(ClassFile classFile)
    {
        return classFile.getCpTypeString(u2nameAndTypeIndex);
    }

    /**
     * Reads the 'info' data following the u1tag byte.
     */
    protected void readInfo(DataInput din) throws IOException
    {
        u2classIndex = din.readUnsignedShort();
        u2nameAndTypeIndex = din.readUnsignedShort();
    }

    /**
     * Writes the 'info' data following the u1tag byte.
     */
    protected void writeInfo(DataOutput dout) throws IOException
    {
        dout.writeShort(u2classIndex);
        dout.writeShort(u2nameAndTypeIndex);
    }
}
