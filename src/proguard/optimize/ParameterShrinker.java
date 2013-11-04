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
package proguard.optimize;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.VariableEditor;
import proguard.classfile.util.SimplifiedVisitor;
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
        if ((method.getAccessFlags() & ClassConstants.INTERNAL_ACC_ABSTRACT) == 0)
        {
            // Get the total size of the local variable frame.
            int variablesSize = codeAttribute.u2maxLocals;

            // The descriptor may have been been shrunk already, so get the
            // original parameter size.
            int parameterSize = ParameterUsageMarker.getParameterSize(method);

            if (DEBUG)
            {
                System.out.println("ParameterShrinker: "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
                System.out.println("  parameter size = " + parameterSize);
            }

            // Delete unused variables from the local variable frame.
            variableEditor.reset(variablesSize);

            for (int parameterIndex = 0; parameterIndex < parameterSize; parameterIndex++)
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
