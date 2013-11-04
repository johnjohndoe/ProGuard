/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.optimize.info;

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.ClassVisitor;
import proguard.optimize.KeepMarker;

/**
 * This ClassVisitor investigates all classes that it visits to see whether
 * they have/are the sole (non-abstract) implementation of an interface.
 * It may already modify the access of the single implementing class to match
 * the access of the interface.
 *
 * @author Eric Lafortune
 */
public class SingleImplementationMarker
extends      SimplifiedVisitor
implements   ClassVisitor
{
    private static final boolean DEBUG = false;


    private final boolean      allowAccessModification;
    private final ClassVisitor extraClassVisitor;


    /**
     * Creates a new SingleImplementationMarker.
     * @param allowAccessModification indicates whether the access modifiers of
     *                                a class can be changed in order to inline
     *                                it.
     */
    public SingleImplementationMarker(boolean allowAccessModification)
    {
        this(allowAccessModification, null);
    }


    /**
     * Creates a new SingleImplementationMarker.
     * @param allowAccessModification indicates whether the access modifiers of
     *                                a class can be changed in order to inline
     *                                it.
     * @param extraClassVisitor       an optional extra visitor for all inlinable
     *                                interfaces.
     */
    public SingleImplementationMarker(boolean      allowAccessModification,
                                      ClassVisitor extraClassVisitor)
    {
        this.allowAccessModification = allowAccessModification;
        this.extraClassVisitor       = extraClassVisitor;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // The program class must be an interface class that cannot be
        // implemented again.
        if ((programClass.getAccessFlags() & ClassConstants.INTERNAL_ACC_INTERFACE) == 0 ||
            KeepMarker.isKept(programClass))
        {
            return;
        }

        // The interface class must have a single implementation.
        Clazz[] subClasses = programClass.subClasses;
        if (subClasses == null ||
            subClasses.length != 1)
        {
            return;
        }

        // If the single implementation is an interface, check it recursively.
        Clazz singleImplementationClass = subClasses[0];
        int singleImplementationAccessFlags = singleImplementationClass.getAccessFlags();
        if ((singleImplementationAccessFlags & ClassConstants.INTERNAL_ACC_INTERFACE) != 0)
        {
            singleImplementationClass.accept(this);

            // See if the subinterface has a single implementation.
            singleImplementationClass = singleImplementation(singleImplementationClass);
            if (singleImplementationClass == null)
            {
                return;
            }

            singleImplementationAccessFlags = singleImplementationClass.getAccessFlags();
        }

        // The single implementation must contain all non-static methods of this
        // interface, so invocations can easily be diverted.
        for (int index = 0; index < programClass.u2methodsCount; index++)
        {
            Method method = programClass.methods[index];
            if ((method.getAccessFlags() & ClassConstants.INTERNAL_ACC_STATIC) == 0 &&
                singleImplementationClass.findMethod(method.getName(programClass),
                                                     method.getDescriptor(programClass)) == null)
            {
                return;
            }
        }

        // Doesn't the implementation have at least the same access as the
        // interface?
        if (AccessUtil.accessLevel(singleImplementationAccessFlags) <
            AccessUtil.accessLevel(programClass.getAccessFlags()))
        {
            // Are we allowed to fix the access?
            if (allowAccessModification)
            {
                // Fix the access.
                ((ProgramClass)singleImplementationClass).u2accessFlags =
                    AccessUtil.replaceAccessFlags(singleImplementationAccessFlags,
                                                  programClass.getAccessFlags());
            }
            else
            {
                // We can't give the implementation the access of the interface.
                // Forget about inlining it after all.
                return;
            }
        }

        if (DEBUG)
        {
            System.out.println("Single implementation of ["+programClass.getName()+"]: ["+singleImplementationClass.getName()+"]");
        }

        // Mark the interface and its single implementation.
        markSingleImplementation(programClass, singleImplementationClass);

        // Visit the interface, if required.
        if (extraClassVisitor != null)
        {
            singleImplementationClass.accept(extraClassVisitor);
        }
    }


    // Small utility methods.

    public static void markSingleImplementation(VisitorAccepter visitorAccepter,
                                                Clazz           singleImplementation)
    {
        // The interface has a single implementation.
        visitorAccepter.setVisitorInfo(singleImplementation);
    }


    public static Clazz singleImplementation(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter != null &&
               visitorAccepter.getVisitorInfo() instanceof Clazz ?
                   (Clazz)visitorAccepter.getVisitorInfo() :
                   null;
    }
}
