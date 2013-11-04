/* $Id: MethodPrivatizer.java,v 1.5 2005/06/11 13:21:35 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
 *
 * Copyright (c) 2002-2005 Eric Lafortune (eric@graphics.cornell.edu)
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
import proguard.classfile.util.AccessUtil;
import proguard.classfile.visitor.MemberInfoVisitor;
import proguard.optimize.NonPrivateMethodMarker;

/**
 * This MemberInfoVisitor makes all final methods that it visits private,
 * unless they have been marked by a NonPrivateMethodMarker.
 *
 * @see NonPrivateMethodMarker
 * @author Eric Lafortune
 */
public class MethodPrivatizer
  implements MemberInfoVisitor
{
    // Implementations for MemberInfoVisitor.

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo) {}

    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        int accessFlags = programMethodInfo.getAccessFlags();

        // Is the method unmarked?
        if (NonPrivateMethodMarker.canBeMadePrivate(programMethodInfo))
        {
            // Make the method private.
            programMethodInfo.u2accessFlags =
                AccessUtil.replaceAccessFlags(accessFlags,
                                              ClassConstants.INTERNAL_ACC_PRIVATE);
        }
    }

    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}
}
