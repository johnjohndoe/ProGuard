/* $Id: AttrInfo.java,v 1.17 2003/12/06 22:15:38 eric Exp $
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


import proguard.classfile.visitor.*;

import java.io.*;

/**
 * Representation of an attribute. Specific attributes have their representations
 * sub-classed from this.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public abstract class AttrInfo implements VisitorAccepter
{
    protected static final int CONSTANT_FIELD_SIZE = 6;


    public int u2attrNameIndex;
    //public int  u4attrLength;
    //public byte info[];

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
        int u2attrNameIndex = din.readUnsignedShort();
        int u4attrLength    = din.readInt();
        String attrName = cf.getCpString(u2attrNameIndex);
        AttrInfo attrInfo =
            attrName.equals(ClassConstants.ATTR_InnerClasses)       ? (AttrInfo)new InnerClassesAttrInfo():
            attrName.equals(ClassConstants.ATTR_ConstantValue)      ? (AttrInfo)new ConstantValueAttrInfo():
            attrName.equals(ClassConstants.ATTR_Exceptions)         ? (AttrInfo)new ExceptionsAttrInfo():
            attrName.equals(ClassConstants.ATTR_Code)               ? (AttrInfo)new CodeAttrInfo():
            attrName.equals(ClassConstants.ATTR_LineNumberTable)    ? (AttrInfo)new LineNumberTableAttrInfo():
            attrName.equals(ClassConstants.ATTR_LocalVariableTable) ? (AttrInfo)new LocalVariableTableAttrInfo():
            attrName.equals(ClassConstants.ATTR_SourceFile)         ? (AttrInfo)new SourceFileAttrInfo():
            attrName.equals(ClassConstants.ATTR_SourceDir)          ? (AttrInfo)new SourceDirAttrInfo():
            attrName.equals(ClassConstants.ATTR_Deprecated)         ? (AttrInfo)new DeprecatedAttrInfo():
            attrName.equals(ClassConstants.ATTR_Synthetic)          ? (AttrInfo)new SyntheticAttrInfo():
            attrName.equals(ClassConstants.ATTR_Signature)          ? (AttrInfo)new SignatureAttrInfo():
                                                                      (AttrInfo)new UnknownAttrInfo(u4attrLength);

        attrInfo.u2attrNameIndex = u2attrNameIndex;
        attrInfo.readInfo(din, cf);
        return attrInfo;
    }


    protected AttrInfo()
    {
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
     * Returns the String name of the attribute; over-ride this in sub-classes.
     */
    public String getAttributeName(ClassFile classFile)
    {
        return classFile.getCpString(u2attrNameIndex);
    }


    // Abstract methods to be implemented by extensions.

    /**
     * Returns the length in bytes of the attribute.
     */
    protected abstract int getAttrInfoLength();


    /**
     * Reads the data following the header.
     */
    protected abstract void readInfo(DataInput din, ClassFile cf) throws IOException;


    /**
     * Exports data following the header to a DataOutput stream.
     */
    protected abstract void writeInfo(DataOutput dout) throws IOException;


    /**
     * Accepts the given visitor.
     */
    public abstract void accept(ClassFile classFile, AttrInfoVisitor attrInfoVisitor);


    // Implementations for VisitorAccepter.

    public Object getVisitorInfo() {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
