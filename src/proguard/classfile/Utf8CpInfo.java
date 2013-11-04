/* $Id: Utf8CpInfo.java,v 1.8 2002/07/04 16:16:58 eric Exp $
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
 * Representation of a 'UTF8' entry in the ConstantPool.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class Utf8CpInfo extends CpInfo
{
    private static final String ENCODING   = "UTF-8";
    private static final byte   NULL_BYTE1 = (byte)0xc0;
    private static final byte   NULL_BYTE2 = (byte)0x80;


    // We're just keeping the String.
    //private int u2length;
    //private byte[] bytes;

    public String utf8string;


    protected Utf8CpInfo()
    {
        super(ClassConstants.CONSTANT_Utf8);
    }

    /**
     * Constructor used when appending fresh Utf8 entries to the constant pool.
     */
    public Utf8CpInfo(String s)
    {
        this();

        utf8string = s;
    }

    /**
     * Returns UTF8 data as a String.
     */
    public String getString()
    {
        return utf8string;
    }

    /**
     * Sets UTF8 data as String.
     */
    public void setString(String s) throws Exception
    {
        utf8string = s;
    }

    /**
     * Reads the 'info' data following the u1tag byte.
     */
    protected void readInfo(DataInput din) throws IOException
    {
        int    u2length = din.readUnsignedShort();
        byte[] bytes    = new byte[u2length];
        din.readFully(bytes);

        utf8string = new String(bytes, ENCODING);
    }

    /**
     * Writes the 'info' data following the u1tag byte.
     */
    protected void writeInfo(DataOutput dout) throws IOException
    {
        byte[] bytes    = modifiedUtf8(utf8string.getBytes(ENCODING));
        int    u2length = bytes.length;

        dout.writeShort(u2length);
        dout.write(bytes);
    }

    /**
     * Accepts the given visitor.
     */
    public void accept(ClassFile classFile, CpInfoVisitor cpInfoVisitor)
    {
        cpInfoVisitor.visitUtf8CpInfo(classFile, this);
    }


    /**
     * Transforms Utf8 bytes to the slightly modified Utf8 representation that
     * is used by class files.
     */
    private byte[] modifiedUtf8(byte[] bytes)
    {
        int length = bytes.length;

        // Check for embedded null bytes.
        int count = 0;
        for (int index = 0; index < length; index++)
        {
            if (bytes[index] == 0)
            {
                count++;
            }
        }

        // Return the original array if it doesn't need to be modified.
        if (count == 0)
        {
            return bytes;
        }

        // Create a new array with all null bytes properly replaced.
        byte[] newBytes = new byte[length + count];
        int    newIndex = 0;

        for (int index = 0; index < length; index++)
        {
            if (bytes[index] == 0)
            {
                newBytes[newIndex++] = NULL_BYTE1;
                newBytes[newIndex++] = NULL_BYTE2;
            }
            else
            {
                newBytes[newIndex++] = bytes[index];
            }
        }

        return newBytes;
    }
}
