/* $Id: LibraryMemberInfo.java,v 1.11 2002/05/23 19:19:57 eric Exp $
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
 * Representation of a field or method from a library class file.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
abstract public class LibraryMemberInfo implements VisitorAccepter
{
    public int    u2accessFlags;
    public String name;
    public String descriptor;

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    protected LibraryMemberInfo() {}


    /**
     * Accepts the given member info visitor.
     */
    public abstract void accept(LibraryClassFile  libraryClassFile,
                                MemberInfoVisitor memberInfoVisitor);


    /**
     * Imports the field or method data to internal representation.
     */
    protected void read(DataInput din, CpInfo[] constantPool) throws IOException
    {
            u2accessFlags     = din.readUnsignedShort();
        int u2nameIndex       = din.readUnsignedShort();
        int u2descriptorIndex = din.readUnsignedShort();
        int u2attributesCount = din.readUnsignedShort();
        for (int i = 0; i < u2attributesCount; i++)
        {
            LibraryAttrInfo.skip(din);
        }

        // Store the actual fields.

        name       = ((Utf8CpInfo)constantPool[u2nameIndex]).getString();
        descriptor = ((Utf8CpInfo)constantPool[u2descriptorIndex]).getString();
    }


    // Implementations for MemberInfo

    public int getAccessFlags()
    {
        return u2accessFlags;
    }

    public String getName(ClassFile classFile)
    {
        return name;
    }

    public String getDescriptor(ClassFile classFile)
    {
        return descriptor;
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
