/* $Id: AllMemberInfoVisitor.java,v 1.4 2002/05/23 19:19:57 eric Exp $
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
 * This ClassFileVisitor visits all ProgramMemberInfo objects of the class files
 * it visits.
 *
 * @author Eric Lafortune
 */
public class AllMemberInfoVisitor implements ClassFileVisitor
{
    private MemberInfoVisitor memberInfoVisitor;


    public AllMemberInfoVisitor(MemberInfoVisitor memberInfoVisitor)
    {
        this.memberInfoVisitor = memberInfoVisitor;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        programClassFile.fieldsAccept(memberInfoVisitor);
        programClassFile.methodsAccept(memberInfoVisitor);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        libraryClassFile.fieldsAccept(memberInfoVisitor);
        libraryClassFile.methodsAccept(memberInfoVisitor);
    }
}
