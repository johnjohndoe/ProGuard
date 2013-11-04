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
import proguard.classfile.attribute.visitor.*;

/**
 * This Attribute represents a line number table attribute.
 *
 * @author Eric Lafortune
 */
public class LineNumberTableAttribute extends Attribute
{
    public int              u2lineNumberTableLength;
    public LineNumberInfo[] lineNumberTable;


    /**
     * Creates an uninitialized LineNumberTableAttribute.
     */
    public LineNumberTableAttribute()
    {
    }


    /**
     * Returns the line number corresponding to the given byte code program
     * counter.
     */
    public int getLineNumber(int pc)
    {
        for (int index = u2lineNumberTableLength-1 ; index >= 0 ; index--)
        {
            LineNumberInfo info = lineNumberTable[index];
            if (pc >= info.u2startPC)
            {
                return info.u2lineNumber;
            }
        }

        return u2lineNumberTableLength > 0 ?
            lineNumberTable[0].u2lineNumber :
            0;
    }


    // Implementations for Attribute.

    public void accept(Clazz clazz, Method method, CodeAttribute codeAttribute, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitLineNumberTableAttribute(clazz, method, codeAttribute, this);
    }


    /**
     * Applies the given visitor to all line numbers.
     */
    public void lineNumbersAccept(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberInfoVisitor lineNumberInfoVisitor)
    {
        for (int index = 0; index < u2lineNumberTableLength; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of LineNumberInfo.
            lineNumberInfoVisitor.visitLineNumberInfo(clazz, method, codeAttribute, lineNumberTable[index]);
        }
    }
}
