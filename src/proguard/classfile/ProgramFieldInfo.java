/* $Id: ProgramFieldInfo.java,v 1.11 2003/02/09 15:22:28 eric Exp $
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
 * Representation of a field from a program class file.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class ProgramFieldInfo extends ProgramMemberInfo implements FieldInfo
{


    /**
     * Creates a new ProgramFieldInfo from the file format data in the DataInput stream.
     *
     * @throws IOException if class file is corrupt or incomplete
     */
    public static ProgramFieldInfo create(DataInput din, ClassFile cf) throws IOException
    {
        ProgramFieldInfo fi = new ProgramFieldInfo();
        fi.read(din, cf);
        return fi;
    }

    /**
     * Accepts the given visitor.
     */
    public void accept(ProgramClassFile programClassFile, MemberInfoVisitor memberInfoVisitor)
    {
        memberInfoVisitor.visitProgramFieldInfo(programClassFile, this);
    }


    protected ProgramFieldInfo() {}
}
