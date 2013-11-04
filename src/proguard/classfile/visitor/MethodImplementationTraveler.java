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
package proguard.classfile.visitor;

import proguard.classfile.*;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This <code>MemberVisitor</code> lets a given <code>MemberVisitor</code>
 * travel to all concrete implementations of the visited methods in their class
 * hierarchies.
 *
 * @author Eric Lafortune
 */
public class MethodImplementationTraveler
extends      SimplifiedVisitor
implements   MemberVisitor
{
    private final boolean       visitThisMethod;
    private final MemberVisitor memberVisitor;


    /**
     * Creates a new MethodImplementationTraveler.
     * @param visitThisMethod   specifies whether to visit the originally
     *                          visited methods.
     * @param memberVisitor     the <code>MemberVisitor</code> to which
     *                          visits will be delegated.
     */
    public MethodImplementationTraveler(boolean       visitThisMethod,
                                        MemberVisitor memberVisitor)
    {
        this.visitThisMethod = visitThisMethod;
        this.memberVisitor   = memberVisitor;
    }


    // Implementations for MemberVisitor.

    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programClass.methodImplementationsAccept(programMethod,
                                                 visitThisMethod,
                                                 memberVisitor);
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.methodImplementationsAccept(libraryMethod,
                                                 visitThisMethod,
                                                 memberVisitor);
    }
}
