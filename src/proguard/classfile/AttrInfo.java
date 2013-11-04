/* $Id: AttrInfo.java,v 1.9 2002/07/04 16:16:58 eric Exp $
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
 * Representation of an attribute. Specific attributes have their representations
 * sub-classed from this.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class AttrInfo implements VisitorAccepter
{
    public static final int CONSTANT_FIELD_SIZE = 6;


    public int  u2attrNameIndex;
    public int  u4attrLength;
    public byte info[];

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    /**
     * Creates a new AttrInfo from the data passed.
     *
     * @throws IOException if class file is corrupt or incomplete
     */
    public static AttrInfo create(DataInput din, ClassFile cf) throws IOException
    {
        // Instantiate based on attribute name
        AttrInfo ai = null;
        int attrNameIndex = din.readUnsignedShort();
        int attrLength = din.readInt();
        String attrName = cf.getCpString(attrNameIndex);
        ai = attrName.equals(ClassConstants.ATTR_Code)               ? (AttrInfo)new CodeAttrInfo(              attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_ConstantValue)      ? (AttrInfo)new ConstantValueAttrInfo(     attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_Deprecated)         ? (AttrInfo)new DeprecatedAttrInfo(        attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_Exceptions)         ? (AttrInfo)new ExceptionsAttrInfo(        attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_LineNumberTable)    ? (AttrInfo)new LineNumberTableAttrInfo(   attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_SourceFile)         ? (AttrInfo)new SourceFileAttrInfo(        attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_LocalVariableTable) ? (AttrInfo)new LocalVariableTableAttrInfo(attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_InnerClasses)       ? (AttrInfo)new InnerClassesAttrInfo(      attrNameIndex, attrLength):
             attrName.equals(ClassConstants.ATTR_Synthetic)          ? (AttrInfo)new SyntheticAttrInfo(         attrNameIndex, attrLength):
                                                                       (AttrInfo)new AttrInfo(                  attrNameIndex, attrLength);

        ai.readInfo(din, cf);
        return ai;
    }


    protected AttrInfo(int attrNameIndex, int attrLength)
    {
        u2attrNameIndex = attrNameIndex;
        u4attrLength = attrLength;
    }

    /**
     * Returns the length in bytes of the attribute; over-ride this in sub-classes.
     */
    protected int getAttrInfoLength()
    {
        return u4attrLength;
    }

    /**
     * Returns the String name of the attribute; over-ride this in sub-classes.
     */
    public String getAttributeName(ClassFile classFile)
    {
        return classFile.getCpString(u2attrNameIndex);
    }

    /**
     * Reads the data following the header; over-ride this in sub-classes.
     */
    protected void readInfo(DataInput din, ClassFile cf) throws IOException
    {
        info = new byte[u4attrLength];
        din.readFully(info);
    }

    /**
     * Exports the representation to a DataOutput stream.
     */
    public final void write(DataOutput dout) throws IOException
    {
        dout.writeShort(u2attrNameIndex);
        dout.writeInt(getAttrInfoLength());
        writeInfo(dout);
    }

    /**
     * Exports data following the header to a DataOutput stream; over-ride this in sub-classes.
     */
    public void writeInfo(DataOutput dout) throws IOException
    {
        dout.write(info);
    }

    /**
     * Accepts the given visitor.
     */
    public void accept(ClassFile classFile, AttrInfoVisitor attrInfoVisitor)
    {
        attrInfoVisitor.visitAttrInfo(classFile, this);
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
