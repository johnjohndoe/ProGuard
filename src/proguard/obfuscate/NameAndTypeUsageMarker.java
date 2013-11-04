/* $Id: NameAndTypeUsageMarker.java,v 1.6 2002/11/03 13:30:14 eric Exp $
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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.visitor.*;


/**
 * This ClassFileVisitor marks all NameAndType constant pool entries that are
 * being used in the classes it visits.
 *
 * @see NameAndTypeShrinker
 *
 * @author Eric Lafortune
 */
public class NameAndTypeUsageMarker
  implements ClassFileVisitor,
             CpInfoVisitor
{
    // A visitor info flag to indicate the NameAndType constant pool entry is being used.
    private static final Object USED = new Object();


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Mark the NameAndType entries referenced by all other constant pool entries.
        programClassFile.constantPoolEntriesAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
    }


    // Implementations for CpInfoVisitor

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo) {}
    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}


    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo)
    {
        visitRefCpInfo(classFile, fieldrefCpInfo);
    }


    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo)
    {
        visitRefCpInfo(classFile, interfaceMethodrefCpInfo);
    }


    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        visitRefCpInfo(classFile, methodrefCpInfo);
    }


    private void visitRefCpInfo(ClassFile classFile, RefCpInfo refCpInfo)
    {
        markNameAndTypeCpEntry(classFile, refCpInfo.u2nameAndTypeIndex);
    }


    // Small utility methods.

    /**
     * Marks the given UTF-8 constant pool entry of the given class.
     */
    private void markNameAndTypeCpEntry(ClassFile classFile, int index)
    {
         markAsUsed((NameAndTypeCpInfo)((ProgramClassFile)classFile).getCpEntry(index));
    }


    /**
     * Marks the given VisitorAccepter as being used.
     * In this context, the VisitorAccepter will be a NameAndTypeCpInfo object.
     */
    private static void markAsUsed(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(USED);
    }


    /**
     * Returns whether the given VisitorAccepter has been marked as being used.
     * In this context, the VisitorAccepter will be a NameAndTypeCpInfo object.
     */
    static boolean isUsed(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == USED;
    }
}
