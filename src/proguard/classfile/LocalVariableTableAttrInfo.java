/* $Id: LocalVariableTableAttrInfo.java,v 1.9 2003/02/09 15:22:28 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 1999      Mark Welsh (markw@retrologic.com)
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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

/**
 * Representation of an attribute.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class LocalVariableTableAttrInfo extends AttrInfo
{
    private static final int CONSTANT_FIELD_SIZE = 2;


    public int                 u2localVariableTableLength;
    public LocalVariableInfo[] localVariableTable;


    protected LocalVariableTableAttrInfo()
    {
    }


    /**
     * Returns the array of local variable table entries.
     */
    protected LocalVariableInfo[] getLocalVariableTable() throws Exception
    {
        return localVariableTable;
    }


    // Implementations for AttrInfo

    protected int getAttrInfoLength()
    {
        return CONSTANT_FIELD_SIZE +
               u2localVariableTableLength * LocalVariableInfo.CONSTANT_FIELD_SIZE;
    }

    protected void readInfo(DataInput din, ClassFile cf) throws IOException
    {
        u2localVariableTableLength = din.readUnsignedShort();
        localVariableTable = new LocalVariableInfo[u2localVariableTableLength];
        for (int i = 0; i < u2localVariableTableLength; i++)
        {
            localVariableTable[i] = LocalVariableInfo.create(din);
        }
    }

    protected void writeInfo(DataOutput dout) throws IOException
    {
        dout.writeShort(u2localVariableTableLength);
        for (int i = 0; i < u2localVariableTableLength; i++)
        {
            localVariableTable[i].write(dout);
        }
    }

    public void accept(ClassFile classFile, AttrInfoVisitor attrInfoVisitor)
    {
        attrInfoVisitor.visitLocalVariableTableAttrInfo(classFile, this);
    }


    /**
     * Applies the given visitor to all local variables.
     */
    public void localVariablesAccept(ClassFile classFile, LocalVariableInfoVisitor localVariableInfoVisitor)
    {
        for (int i = 0; i < u2localVariableTableLength; i++)
        {
            // We don't need double dispatching here, since there is only one
            // type of LocalVariableInfo.
            localVariableInfoVisitor.visitLocalVariableInfo(classFile, localVariableTable[i]);
        }
    }
}
