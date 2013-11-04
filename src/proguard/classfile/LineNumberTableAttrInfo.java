/* $Id: LineNumberTableAttrInfo.java,v 1.6 2002/05/12 14:29:08 eric Exp $
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
 * Representation of an attribute.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class LineNumberTableAttrInfo extends AttrInfo
{
    public static final int CONSTANT_FIELD_SIZE = 2;


    public int              u2lineNumberTableLength;
    public LineNumberInfo[] lineNumberTable;


    protected LineNumberTableAttrInfo(int attrNameIndex, int attrLength)
    {
        super(attrNameIndex, attrLength);
    }

    /**
     * Returns the length in bytes of the attribute.
     */
    protected int getAttrInfoLength()
    {
        return CONSTANT_FIELD_SIZE +
               u2lineNumberTableLength * LineNumberInfo.CONSTANT_FIELD_SIZE;
    }

    /**
     * Reads the data following the header.
     */
    protected void readInfo(DataInput din, ClassFile cf) throws IOException
    {
        u2lineNumberTableLength = din.readUnsignedShort();
        lineNumberTable = new LineNumberInfo[u2lineNumberTableLength];
        for (int i = 0; i < u2lineNumberTableLength; i++)
        {
            lineNumberTable[i] = LineNumberInfo.create(din);
        }
    }

    /**
     * Exports data following the header to a DataOutput stream.
     */
    public void writeInfo(DataOutput dout) throws IOException
    {
        dout.writeShort(u2lineNumberTableLength);
        for (int i = 0; i < u2lineNumberTableLength; i++)
        {
            lineNumberTable[i].write(dout);
        }
    }


    public int getLineNumber(int pc)
    {
        for (int i = u2lineNumberTableLength-1 ; i >= 0 ; i--)
        {
            LineNumberInfo info = lineNumberTable[i];
            if (pc >= info.u2startpc)
            {
                return info.u2lineNumber;
            }
        }

        return u2lineNumberTableLength > 0 ?
            lineNumberTable[0].u2lineNumber :
            0;
    }


    /**
     * Accepts the given visitor.
     */
    public void accept(ClassFile classFile, AttrInfoVisitor attrInfoVisitor)
    {
        attrInfoVisitor.visitLineNumberTableAttrInfo(classFile, this);
    }

    /**
     * Applies the given visitor to all line numbers.
     */
    public void lineNumbersAccept(ClassFile classFile, LineNumberInfoVisitor lineNumberInfoVisitor)
    {
        for (int i = 0; i < u2lineNumberTableLength; i++)
        {
            // We don't need double dispatching here, since there is only one
            // type of LineNumberInfo.
            lineNumberInfoVisitor.visitLineNumberInfo(classFile, lineNumberTable[i]);
        }
    }
}
