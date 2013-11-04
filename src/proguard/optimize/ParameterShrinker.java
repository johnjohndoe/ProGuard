/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2008 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.optimize;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.VariableEditor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.MemberVisitor;
import proguard.optimize.info.ParameterUsageMarker;

/**
 * This MemberVisitor removes unused parameters from the code of the methods
 * that it visits.
 *
 * @see ParameterUsageMarker
 * @see MethodStaticizer
 * @see MethodDescriptorShrinker
 * @author Eric Lafortune
 */
public class ParameterShrinker
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    private static final boolean DEBUG = false;


    private final MemberVisitor extraVariableMemberVisitor;

    private final VariableEditor variableEditor = new VariableEditor();


    /**
     * Creates a new ParameterShrinker.
     */
    public ParameterShrinker()
    {
        this(null);
    }


    /**
     * Creates a new ParameterShrinker with an extra visitor.
     * @param extraVariableMemberVisitor an optional extra visitor for all
     *                                   removed parameters.
     */
    public ParameterShrinker(MemberVisitor extraVariableMemberVisitor)
    {
        this.extraVariableMemberVisitor = extraVariableMemberVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Get the original parameter size that was saved.
        int oldParameterSize = ParameterUsageMarker.getParameterSize(method);

        // Compute the new parameter size from the shrunk descriptor.
        int newParameterSize =
            ClassUtil.internalMethodParameterSize(method.getDescriptor(clazz),
                                                  method.getAccessFlags());

        if (oldParameterSize > newParameterSize)
        {
            // Get the total size of the local variable frame.
            int maxLocals = codeAttribute.u2maxLocals;

            if (DEBUG)
            {
                System.out.println("ParameterShrinker: "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
                System.out.println("  Old parameter size = " + oldParameterSize);
                System.out.println("  New parameter size = " + newParameterSize);
                System.out.println("  Max locals         = " + maxLocals);
            }

            // Delete unused variables from the local variable frame.
            variableEditor.reset(maxLocals);

            for (int parameterIndex = 0; parameterIndex < oldParameterSize; parameterIndex++)
            {
                // Is the variable not required as a parameter?
                if (!ParameterUsageMarker.isParameterUsed(method, parameterIndex))
                {
                    if (DEBUG)
                    {
                        System.out.println("  Deleting parameter #"+parameterIndex);
                    }

                    // Delete the unused variable.
                    variableEditor.deleteVariable(parameterIndex);

                    // Visit the method, if required.
                    if (extraVariableMemberVisitor != null)
                    {
                        method.accept(clazz, extraVariableMemberVisitor);
                    }
                }
            }

            // Shift all remaining parameters and variables in the byte code.
            variableEditor.visitCodeAttribute(clazz, method, codeAttribute);
        }
    }
}
