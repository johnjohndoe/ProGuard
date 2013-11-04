/* $Id: Utf8CpInfo.java,v 1.11 2002/08/02 16:40:28 eric Exp $
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
 * Representation of a 'UTF-8' entry in the ConstantPool.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class Utf8CpInfo extends CpInfo
{
    private static final String ENCODING   = "UTF-8";
    private static final byte   NULL_BYTE1 = (byte)0xc0;
    private static final byte   NULL_BYTE2 = (byte)0x80;

    // There are a lot of Utf8CpInfo objects, so we're optimising their storage.
    // Initially, we're storing the UTF-8 bytes in a byte array.
    // When the corresponding String is requested, we ditch the array and just
    // store the String.

    //private int u2length;
    private byte[] bytes;

    private String utf8string;


    protected Utf8CpInfo()
    {
    }

    /**
     * Constructor used when appending fresh UTF-8 entries to the constant pool.
     */
    public Utf8CpInfo(String utf8string)
    {
        this.bytes      = null;
        this.utf8string = utf8string;
    }

    /**
     * Returns UTF-8 data as a String.
     */
    public String getString()
    {
        try
        {
            switchToStringRepresentation();
        }
        catch (UnsupportedEncodingException ex)
        {
        }

        return utf8string;
    }

    /**
     * Sets UTF-8 data as String.
     */
    public void setString(String utf8String) throws Exception
    {
        this.bytes      = null;
        this.utf8string = utf8String;
    }


    // Implementations for CpInfo

    public int getTag()
    {
        return ClassConstants.CONSTANT_Utf8;
    }

    protected void readInfo(DataInput din) throws IOException
    {
        int u2length = din.readUnsignedShort();
        bytes = new byte[u2length];
        din.readFully(bytes);
    }

    protected void writeInfo(DataOutput dout) throws IOException
    {
        byte[] bytes    = getByteArrayRepresentation();
        int    u2length = bytes.length;

        dout.writeShort(u2length);
        dout.write(bytes);
    }

    public void accept(ClassFile classFile, CpInfoVisitor cpInfoVisitor)
    {
        cpInfoVisitor.visitUtf8CpInfo(classFile, this);
    }


    /**
     * Switches to a String representation of the UTF-8 data.
     */
    private void switchToStringRepresentation() throws UnsupportedEncodingException
    {
        if (utf8string == null)
        {
            utf8string = new String(bytes, ENCODING);
            bytes = null;
        }
    }


    /**
     * Transforms UTF-8 bytes to the slightly modified UTF-8 representation that
     * is used by class files.
     */
    private byte[] getByteArrayRepresentation() throws UnsupportedEncodingException
    {
        // Do we still have the byte array representation?
        if (bytes != null)
        {
            // Then return that one.
            return bytes;
        }

        // Otherwise reconstruct it from the String representation.
        byte[] bytes  = utf8string.getBytes(ENCODING);
        int    length = bytes.length;

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
