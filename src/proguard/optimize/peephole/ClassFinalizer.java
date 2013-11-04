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
package proguard.optimize.peephole;

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.optimize.KeepMarker;

/**
 * This <code>ClassVisitor</code> and <code>MemberVisitor</code>
 * makes the classes it visits, and their methods, final, if possible.
 *
 * @author Eric Lafortune
 */
public class ClassFinalizer
extends      SimplifiedVisitor
implements   ClassVisitor,
             MemberVisitor
{
    private final ClassVisitor  extraClassVisitor;
    private final MemberVisitor extraMemberVisitor;

    private final MemberFinder memberFinder = new MemberFinder();


    /**
     * Creates a new ClassFinalizer.
     */
    public ClassFinalizer()
    {
        this(null, null);
    }


    /**
     * Creates a new ClassFinalizer.
     * @param extraClassVisitor  an optional extra visitor for all finalized
     *                           classes.
     * @param extraMemberVisitor an optional extra visitor for all finalized
     *                           methods.
     */
    public ClassFinalizer(ClassVisitor  extraClassVisitor,
                          MemberVisitor extraMemberVisitor)
    {
        this.extraClassVisitor  = extraClassVisitor;
        this.extraMemberVisitor = extraMemberVisitor;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // If the class is not final/interface/abstract,
        // and it is not being kept,
        // and it doesn't have any subclasses,
        // then make it final.
        if ((programClass.u2accessFlags & (ClassConstants.INTERNAL_ACC_FINAL     |
                                           ClassConstants.INTERNAL_ACC_INTERFACE |
                                           ClassConstants.INTERNAL_ACC_ABSTRACT)) == 0 &&
            !KeepMarker.isKept(programClass)                                           &&
            programClass.subClasses == null)
        {
            programClass.u2accessFlags |= ClassConstants.INTERNAL_ACC_FINAL;

            // Visit the class, if required.
            if (extraClassVisitor != null)
            {
                extraClassVisitor.visitProgramClass(programClass);
            }
        }

        // Check all methods.
        programClass.methodsAccept(this);
    }


    // Implementations for MemberVisitor.

    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        String name = programMethod.getName(programClass);

        // If the method is not already private/static/final/abstract,
        // and it is not a constructor,
        // and its class is final,
        //     or it is not being kept and it is not overridden,
        // then make it final.
        if ((programMethod.u2accessFlags & (ClassConstants.INTERNAL_ACC_PRIVATE |
                                            ClassConstants.INTERNAL_ACC_STATIC  |
                                            ClassConstants.INTERNAL_ACC_FINAL   |
                                            ClassConstants.INTERNAL_ACC_ABSTRACT)) == 0 &&
            !name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT)                      &&
            ((programClass.u2accessFlags & ClassConstants.INTERNAL_ACC_FINAL) != 0 ||
             (!KeepMarker.isKept(programMethod) &&
              (programClass.subClasses == null ||
               !memberFinder.isOverriden(programClass, programMethod)))))
        {
            programMethod.u2accessFlags |= ClassConstants.INTERNAL_ACC_FINAL;

            // Visit the method, if required.
            if (extraMemberVisitor != null)
            {
                extraMemberVisitor.visitProgramMethod(programClass, programMethod);
            }
        }
    }
}
