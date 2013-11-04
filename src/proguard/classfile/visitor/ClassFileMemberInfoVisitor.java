/* $Id: ClassFileMemberInfoVisitor.java,v 1.1 2002/09/07 16:05:13 eric Exp $
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
 * This MemberInfoVisitor delegates all visits to a given ClassFileVisitor.
 * The latter visits the class file of each visited class member.
 *
 * @author Eric Lafortune
 */
public class ClassFileMemberInfoVisitor implements MemberInfoVisitor
{
    private ClassFileVisitor classFileVisitor;


    public ClassFileMemberInfoVisitor(ClassFileVisitor classFileVisitor)
    {
        this.classFileVisitor = classFileVisitor;
    }


    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
    {
        classFileVisitor.visitProgramClassFile(programClassFile);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        classFileVisitor.visitProgramClassFile(programClassFile);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
    {
        classFileVisitor.visitLibraryClassFile(libraryClassFile);
    }


    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
    {
        classFileVisitor.visitLibraryClassFile(libraryClassFile);
    }
}
