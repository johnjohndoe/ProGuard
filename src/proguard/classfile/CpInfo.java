/* $Id: CpInfo.java,v 1.9 2002/07/04 16:16:58 eric Exp $
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
 * Representation of an entry in the ConstantPool. Specific types of entry
 * have their representations sub-classed from this.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
abstract public class CpInfo implements VisitorAccepter
{
    public int u1tag;
    //private byte info[];

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    /**
     * Creates a new CpInfo from the data passed.
     *
     * @throws IOException if class file is corrupt or incomplete
     */
    public static CpInfo create(DataInput din) throws IOException
    {
        // Instantiate based on tag byte
        CpInfo ci = null;
        int type = din.readUnsignedByte();
        switch (type)
        {
        case ClassConstants.CONSTANT_Utf8:               ci = new Utf8CpInfo();              break;
        case ClassConstants.CONSTANT_Integer:            ci = new IntegerCpInfo();           break;
        case ClassConstants.CONSTANT_Float:              ci = new FloatCpInfo();             break;
        case ClassConstants.CONSTANT_Long:               ci = new LongCpInfo();              break;
        case ClassConstants.CONSTANT_Double:             ci = new DoubleCpInfo();            break;
        case ClassConstants.CONSTANT_Class:              ci = new ClassCpInfo();             break;
        case ClassConstants.CONSTANT_String:             ci = new StringCpInfo();            break;
        case ClassConstants.CONSTANT_Fieldref:           ci = new FieldrefCpInfo();          break;
        case ClassConstants.CONSTANT_Methodref:          ci = new MethodrefCpInfo();         break;
        case ClassConstants.CONSTANT_InterfaceMethodref: ci = new InterfaceMethodrefCpInfo();break;
        case ClassConstants.CONSTANT_NameAndType:        ci = new NameAndTypeCpInfo();       break;
        default: throw new IOException("Unknown constant type ["+type+"] in constant pool.");
        }
        ci.readInfo(din);
        return ci;
    }


    protected CpInfo(int tag)
    {
        u1tag = tag;
    }


    public int getTag()
    {
        return u1tag;
    }


    /**
     * Reads the 'info' data following the u1tag byte; override this in subclasses.
     */
    abstract protected void readInfo(DataInput din) throws IOException;


    /**
     * Exports the representation to a DataOutput stream.
     */
    public void write(DataOutput dout) throws IOException
    {
        dout.writeByte(u1tag);
        writeInfo(dout);
    }

    /**
     * Writes the 'info' data following the u1tag byte; override this in subclasses.
     */
    abstract protected void writeInfo(DataOutput dout) throws IOException;


    /**
     * Accepts the given visitor.
     */
    public abstract void accept(ClassFile classFile, CpInfoVisitor cpInfoVisitor);


    // Implementations for VisitorAccepter

    public Object getVisitorInfo()
    {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
