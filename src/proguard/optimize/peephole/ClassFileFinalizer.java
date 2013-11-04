/* $Id: ClassFileFinalizer.java,v 1.3 2004/08/15 12:39:30 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
import proguard.classfile.visitor.*;
import proguard.optimize.*;


/**
 * This <code>ClassFileVisitor</code> and <code>MemberInfoVisitor</code>
 * makes the class files it visits, and their class members, final, if possible.
 *
 * @author Eric Lafortune
 */
public class ClassFileFinalizer
  implements ClassFileVisitor,
             MemberInfoVisitor
{
    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // If the class is not final/interface/abstract,
        // and it is not being kept,
        // then make it final.
        if ((programClassFile.u2accessFlags & (ClassConstants.INTERNAL_ACC_FINAL     |
                                               ClassConstants.INTERNAL_ACC_INTERFACE |
                                               ClassConstants.INTERNAL_ACC_ABSTRACT)) == 0 &&
            !KeepMarker.isKept(programClassFile)                                           &&
            programClassFile.subClasses == null)
        {
            programClassFile.u2accessFlags |= ClassConstants.INTERNAL_ACC_FINAL;
        }

        // Check all methods.
        programClassFile.methodsAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile) {}


    // Implementations for MemberInfoVisitor.

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo) {}


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        String name = programMethodInfo.getName(programClassFile);

        // If the method is not final/abstract.
        // and it is not an initialization method,
        // and its class is final,
        //     or it is not being kept and it is not overridden,
        // then make it final.
        if ((programMethodInfo.u2accessFlags & (ClassConstants.INTERNAL_ACC_FINAL |
                                                ClassConstants.INTERNAL_ACC_ABSTRACT)) == 0 &&
            !name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT)                        &&
            !name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT)                          &&
            ((programClassFile.u2accessFlags & ClassConstants.INTERNAL_ACC_FINAL) != 0 ||
             (!KeepMarker.isKept(programMethodInfo) &&
              !isOverriden(programClassFile, programMethodInfo))))
        {
            programMethodInfo.u2accessFlags |= ClassConstants.INTERNAL_ACC_FINAL;
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Small utility methods.

    private boolean isOverriden(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        // If the class file doesn't have any subclasses, we can stop looking
        // right here.
        if (programClassFile.subClasses == null)
        {
            return false;
        }

        // Go looking for the method down the class hierarchy.
        try
        {
            String name       = programMethodInfo.getName(programClassFile);
            String descriptor = programMethodInfo.getDescriptor(programClassFile);

            programClassFile.hierarchyAccept(false, false, false, true,
                                             new NamedMethodVisitor(
                                             new MyMemberFinder(), name, descriptor));
        }
        catch (MyMemberFinder.MemberFoundException ex)
        {
            return true;
        }

        return false;
    }


    /**
     * This utility class throws a MemberFoundException whenever it visits
     * a member. For program class files, it then also stores the class file
     * and member info.
     */
    private static class MyMemberFinder implements MemberInfoVisitor
    {
        private static class MemberFoundException extends IllegalArgumentException {};
        private static final MemberFoundException MEMBER_FOUND = new MemberFoundException();


        // Implementations for MemberInfoVisitor.

        public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
        {
            throw MEMBER_FOUND;
        }

        public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
        {
            throw MEMBER_FOUND;
        }


        public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
        {
            throw MEMBER_FOUND;
        }

        public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
        {
            throw MEMBER_FOUND;
        }
    }
}
