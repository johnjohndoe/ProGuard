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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.visitor.*;


/**
 * This <code>ClassVisitor</code> and <code>MemberVisitor</code>
 * marks names of the classes and class members it visits. The marked names
 * will remain unchanged in the obfuscation step.
 *
 * @see ClassObfuscator
 * @see MemberObfuscator
 *
 * @author Eric Lafortune
 */
class      NameMarker
implements ClassVisitor,
           MemberVisitor
{
    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        keepClassName(programClass);
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
        keepClassName(libraryClass);
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        keepFieldName(programClass, programField);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        keepMethodName(programClass, programMethod);
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        keepFieldName(libraryClass, libraryField);
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        keepMethodName(libraryClass, libraryMethod);
    }


    // Small utility method.

    /**
     * Ensures the name of the given class name will be kept.
     */
    public void keepClassName(Clazz clazz)
    {
        ClassObfuscator.setNewClassName(clazz,
                                        clazz.getName());
    }


    /**
     * Ensures the name of the given field name will be kept.
     */
    private void keepFieldName(Clazz clazz, Field field)
    {
        MemberObfuscator.setFixedNewMemberName(field,
                                               field.getName(clazz));
    }


    /**
     * Ensures the name of the given method name will be kept.
     */
    private void keepMethodName(Clazz clazz, Method method)
    {
        String name = method.getName(clazz);

        if (!name.equals(ClassConstants.INTERNAL_METHOD_NAME_CLINIT) &&
            !name.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT))
        {
            MemberObfuscator.setFixedNewMemberName(method,
                                                   method.getName(clazz));
        }
    }
}
