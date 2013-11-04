/* $Id: LibraryFilteredClassFileVisitor.java,v 1.1 2002/09/01 16:41:35 eric Exp $
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
 * This <code>ClassFileVisitor</code> delegates its visits to another given
 * <code>ClassFileVisitor</code>, but only when visiting library class files.
 *
 * @see ClassConstants
 *
 * @author Eric Lafortune
 */
public class LibraryFilteredClassFileVisitor implements ClassFileVisitor
{
    private ClassFileVisitor classFileVisitor;


    /**
     * Creates a new LibraryFilteredClassFileVisitor.
     * @param classFileVisitor the <code>ClassFileVisitor</code> to which visits
     *                         will be delegated.
     */
    public LibraryFilteredClassFileVisitor(ClassFileVisitor classFileVisitor)
    {
        this.classFileVisitor = classFileVisitor;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Don't delegate visits to program class files.
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        classFileVisitor.visitLibraryClassFile(libraryClassFile);
    }
}
