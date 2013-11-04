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
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.evaluation.*;
import proguard.evaluation.value.Value;
import proguard.optimize.info.ParameterUsageMarker;

/**
 * This InvocationUnit removes unused parameters from the stack before invoking
 * a method, and then delegates to another given InvocationUnit.
 *
 * @see ParameterUsageMarker
 * @author Eric Lafortune
 */
public class UnusedParameterInvocationUnit
extends      SimplifiedVisitor
implements   InvocationUnit,
             ConstantVisitor,
             MemberVisitor
{
    private static final boolean DEBUG = false;


    private final InvocationUnit invocationUnit;

    private Stack stack;


    public UnusedParameterInvocationUnit(InvocationUnit invocationUnit)
    {
        this.invocationUnit = invocationUnit;
    }


    // Implementations for InvocationUnit.


    public void enterMethod(Clazz clazz, Method method, Variables variables)
    {
        invocationUnit.enterMethod(clazz, method, variables);
    }


    public void exitMethod(Clazz clazz, Method method, Value returnValue)
    {
        invocationUnit.exitMethod(clazz, method, returnValue);
    }


    public void invokeMember(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction, Stack stack)
    {
        // Fix the stack if this is a method invocation of which some
        // parameters are marked as unused.
        this.stack = stack;
        clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);

        invocationUnit.invokeMember(clazz,
                                    method,
                                    codeAttribute,
                                    offset,
                                    constantInstruction,
                                    stack);
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitAnyMethodrefConstant(Clazz clazz, RefConstant refConstant)
    {
        refConstant.referencedMemberAccept(this);
    }


    // Implementations for MemberVisitor.

    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        if (DEBUG)
        {
            System.out.println("UnusedParameterInvocationUnit: "+programMethod.getName(programClass)+programMethod.getDescriptor(programClass));
        }

        // Get the total size of the parameters.
        int parameterSize = ParameterUsageMarker.getParameterSize(programMethod);

        // Remove unused parameters.
        for (int index = 0; index < parameterSize; index++)
        {
            if (!ParameterUsageMarker.isParameterUsed(programMethod, index))
            {
                if (DEBUG)
                {
                    System.out.println("  removing stack entry #"+(parameterSize - index - 1)+" ("+stack.getTop(parameterSize - index - 1)+")");
                }

                stack.removeTop(parameterSize - index - 1);
            }
        }
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod) {}
}
