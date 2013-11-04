/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
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
package proguard.classfile.attribute;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This Attribute represents an exceptions attribute.
 *
 * @author Eric Lafortune
 */
public class ExceptionsAttribute extends Attribute
{
    public int   u2exceptionIndexTableLength;
    public int[] u2exceptionIndexTable;


    /**
     * Creates an uninitialized ExceptionsAttribute.
     */
    public ExceptionsAttribute()
    {
    }


    // Implementations for Attribute.

    public void accept(Clazz clazz, Method method, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitExceptionsAttribute(clazz, method, this);
    }


    /**
     * Applies the given constant pool visitor to all exception class pool info
     * entries.
     */
    public void exceptionEntriesAccept(ProgramClass programClass, ConstantVisitor constantVisitor)
    {
        for (int index = 0; index < u2exceptionIndexTableLength; index++)
        {
            programClass.constantPoolEntryAccept(u2exceptionIndexTable[index],
                                                 constantVisitor);
        }
    }
}
