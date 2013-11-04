/* $Id: FilteredMemberInfoVisitor.java,v 1.8 2002/05/23 19:19:57 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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


/**
 * This <code>MemberInfoVisitor</code> delegates its visits to another given
 * <code>MemberInfoVisitor</code>, but only when the visited class file
 * has the proper access flags.
 * <p>
 * If conflicting access flags (public/private/protected) are specified,
 * having one of them set will be considered sufficient.
 *
 * @see ClassConstants
 *
 * @author Eric Lafortune
 */
public class FilteredMemberInfoVisitor
  implements MemberInfoVisitor
{
    // A mask of conflicting access flags. These are interpreted in a special
    // way if more of them are required at the same time. In that case, one
    // of them set is sufficient.
    private static final int ACCESS_MASK =
        ClassConstants.INTERNAL_ACC_PUBLIC  |
        ClassConstants.INTERNAL_ACC_PRIVATE |
        ClassConstants.INTERNAL_ACC_PROTECTED;

    private MemberInfoVisitor memberInfoVisitor;
    private int                  requiredSetAccessFlags;
    private int                  requiredUnsetAccessFlags;
    private int                  requiredOneSetAccessFlags;


    /**
     * Creates a new FilteredMemberInfoVisitor.
     * @param memberInfoVisitor     the <code>MemberInfoVisitor</code> to
     *                                 which visits will be delegated.
     * @param requiredSetAccessFlags   the class access flags that should be
     *                                 set.
     * @param requiredUnsetAccessFlags the class access flags that should be
     *                                 unset.
     */
    public FilteredMemberInfoVisitor(MemberInfoVisitor memberInfoVisitor,
                                        int                  requiredSetAccessFlags,
                                        int                  requiredUnsetAccessFlags)
    {
        this.memberInfoVisitor     = memberInfoVisitor;
        this.requiredSetAccessFlags   = requiredSetAccessFlags & ~ACCESS_MASK;
        this.requiredUnsetAccessFlags = requiredUnsetAccessFlags;

        requiredOneSetAccessFlags = requiredSetAccessFlags & ACCESS_MASK;
        if (requiredOneSetAccessFlags == 0)
        {
            requiredOneSetAccessFlags = ~0;
        }
    }


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
    {
        int accessFlags = programfieldInfo.getAccessFlags();
        if ((requiredSetAccessFlags    & ~accessFlags) == 0 &&
            (requiredUnsetAccessFlags  &  accessFlags) == 0 &&
            (requiredOneSetAccessFlags &  accessFlags) != 0)
        {
            memberInfoVisitor.visitProgramFieldInfo(programClassFile, programfieldInfo);
        }
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        int accessFlags = programMethodInfo.getAccessFlags();
        if ((requiredSetAccessFlags    & ~accessFlags) == 0 &&
            (requiredUnsetAccessFlags  &  accessFlags) == 0 &&
            (requiredOneSetAccessFlags &  accessFlags) != 0)
        {
            memberInfoVisitor.visitProgramMethodInfo(programClassFile, programMethodInfo);
        }
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        int accessFlags = libraryFieldInfo.getAccessFlags();
        if ((requiredSetAccessFlags    & ~accessFlags) == 0 &&
            (requiredUnsetAccessFlags  &  accessFlags) == 0 &&
            (requiredOneSetAccessFlags &  accessFlags) != 0)
        {
            memberInfoVisitor.visitLibraryFieldInfo(libraryClassFile, libraryFieldInfo);
        }
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        int accessFlags = libraryMethodInfo.getAccessFlags();
        if ((requiredSetAccessFlags    & ~accessFlags) == 0 &&
            (requiredUnsetAccessFlags  &  accessFlags) == 0 &&
            (requiredOneSetAccessFlags &  accessFlags) != 0)
        {
            memberInfoVisitor.visitLibraryMethodInfo(libraryClassFile, libraryMethodInfo);
        }
    }
}
