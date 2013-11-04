/* $Id: ByteCpInstruction.java,v 1.7 2002/07/04 16:16:58 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile.instruction;

import proguard.classfile.*;

import java.io.*;


/**
 * This class describes an instruction that refers to an entry in the
 * constant pool.
 *
 * @author Eric Lafortune
 */
public class ByteCpInstruction
     extends GenericInstruction
  implements CpInstruction
{

    public int getCpIndex()
    {
        return code[offset+1] & 0xff;
    }

    public void setCpIndex(int cpIndex)
    {
        if (cpIndex > 255)
        {
            throw new IllegalArgumentException("Constant pool index larger than byte ["+cpIndex+"]");
        }

        code[offset+1] = (byte)cpIndex;
    }


    public void accept(ClassFile classFile, InstructionVisitor instructionVisitor)
    {
        instructionVisitor.visitCpInstruction(classFile, this);
    }
}
