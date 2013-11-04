/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2010 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This AttributeVisitor trims and marks all local variable (type) table
 * attributes that it visits. It keeps parameter names and types and removes
 * the ordinary local variable names and types.
 *
 * @see AttributeUsageMarker
 *
 * @author Eric Lafortune
 */
public class ParameterNameMarker
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    private AttributeUsageMarker attributeUsageMarker = new AttributeUsageMarker();


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        if (!AttributeUsageMarker.isUsed(localVariableTableAttribute))
        {
            for (int index = 0; index < localVariableTableAttribute.u2localVariableTableLength; index++)
            {
                LocalVariableInfo localVariableInfo =
                    localVariableTableAttribute.localVariableTable[index];

                // Trim the table if we've found an ordinary local variable.
                // We're assuming the parameters all come first.
                if (localVariableInfo.u2startPC > 0)
                {
                    localVariableTableAttribute.u2localVariableTableLength = index;
                    break;
                }
            }

            if (localVariableTableAttribute.u2localVariableTableLength > 0)
            {
                attributeUsageMarker.visitLocalVariableTableAttribute(clazz, method, codeAttribute, localVariableTableAttribute);
            }
        }
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        if (!AttributeUsageMarker.isUsed(localVariableTypeTableAttribute))
        {
            for (int index = 0; index < localVariableTypeTableAttribute.u2localVariableTypeTableLength; index++)
            {
                LocalVariableTypeInfo localVariableTypeInfo =
                    localVariableTypeTableAttribute.localVariableTypeTable[index];

                // Trim the table if we've found an ordinary local variable.
                // We're assuming the parameters all come first.
                if (localVariableTypeInfo.u2startPC > 0)
                {
                    localVariableTypeTableAttribute.u2localVariableTypeTableLength = index;
                    break;
                }
            }

            if (localVariableTypeTableAttribute.u2localVariableTypeTableLength > 0)
            {
                attributeUsageMarker.visitLocalVariableTypeTableAttribute(clazz, method, codeAttribute, localVariableTypeTableAttribute);
            }
        }
    }
}