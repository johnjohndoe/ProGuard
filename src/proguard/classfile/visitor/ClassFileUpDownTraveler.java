/* $Id: ClassFileUpDownTraveler.java,v 1.9 2003/02/09 15:22:29 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
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
package proguard.classfile.visitor;

import proguard.classfile.*;


/**
 * This <code>ClassFileVisitor</code> lets a given <code>ClassFileVisitor</code>
 * optionally travel to the visited class, its superclass, its interfaces, and
 * its subclasses.
 *
 * @author Eric Lafortune
 */
public class ClassFileUpDownTraveler
  implements ClassFileVisitor,
             CpInfoVisitor
{
    private boolean visitThisClass;
    private boolean visitSuperClass;
    private boolean visitInterfaces;
    private boolean visitSubclasses;

    private ClassFileVisitor classFileVisitor;

    private ClassFileUpDownTraveler classFileUpTraveler;
    private ClassFileUpDownTraveler classFileDownTraveler;


    /**
     * Creates a new ClassFileUpDownTraveler.
     * @param visitThisClass   specifies whether to visit the originally visited
     *                         classes.
     * @param visitSuperClass  specifies whether to visit the super classes of
     *                         the visited classes.
     * @param visitInterfaces  specifies whether to visit the interfaces of
     *                         the visited classes.
     * @param visitSubclasses  specifies whether to visit the subclasses of
     *                         the visited classes.
     * @param classFileVisitor the <code>ClassFileVisitor</code> to
     *                         which visits will be delegated.
     */
    public ClassFileUpDownTraveler(boolean          visitThisClass,
                                   boolean          visitSuperClass,
                                   boolean          visitInterfaces,
                                   boolean          visitSubclasses,
                                   ClassFileVisitor classFileVisitor)
    {
        this.visitThisClass  = visitThisClass;
        this.visitSuperClass = visitSuperClass;
        this.visitInterfaces = visitInterfaces;
        this.visitSubclasses = visitSubclasses;

        this.classFileVisitor = classFileVisitor;

        if (visitSuperClass || visitInterfaces)
        {
            // If this class is also traveling down, we'll have to create a new
            // traveler that doesn't.
            classFileUpTraveler = !visitThisClass || visitSubclasses ?
                new ClassFileUpDownTraveler(true,
                                            visitSuperClass,
                                            visitInterfaces,
                                            false,
                                            classFileVisitor) :
                this;
        }

        if (visitSubclasses)
        {
            // If this class is also traveling up, we'll have to create a new
            // traveler that doesn't.
            classFileDownTraveler = !visitThisClass || visitSuperClass || visitInterfaces ?
                new ClassFileUpDownTraveler(true,
                                            false,
                                            false,
                                            visitSubclasses,
                                            classFileVisitor) :
                this;
        }
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // First visit the current classfile.
        if (visitThisClass)
        {
            programClassFile.accept(classFileVisitor);
        }

        // Then visit its superclass, recursively.
        if (visitSuperClass)
        {
            int u2superClass = programClassFile.u2superClass;
            if (u2superClass != 0)
            {
                programClassFile.constantPoolEntryAccept(classFileUpTraveler, u2superClass);
            }
        }

        // Then visit its interfaces, recursively.
        if (visitInterfaces)
        {
            int[] u2interfaces = programClassFile.u2interfaces;
            for (int i = 0; i < programClassFile.u2interfacesCount; i++)
            {
                programClassFile.constantPoolEntryAccept(classFileUpTraveler, u2interfaces[i]);
            }
        }

        // Then visit its subclasses, recursively.
        if (visitSubclasses)
        {
            ClassFile[] subClasses = programClassFile.subClasses;
            if (subClasses != null)
            {
                for (int i = 0; i < subClasses.length; i++)
                {
                    subClasses[i].accept(classFileDownTraveler);
                }
            }
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // First visit the current classfile.
        if (visitThisClass)
        {
            libraryClassFile.accept(classFileVisitor);
        }

        // Then visit its superclass, recursively.
        if (visitSuperClass)
        {
            ClassFile superClass = libraryClassFile.superClass;
            if (superClass != null)
            {
                superClass.accept(classFileUpTraveler);
            }
        }

        // Then visit its interfaces, recursively.
        if (visitInterfaces)
        {
            ClassFile[] interfaceClasses = libraryClassFile.interfaceClasses;
            if (interfaceClasses != null)
            {
                for (int i = 0; i < interfaceClasses.length; i++)
                {
                    interfaceClasses[i].accept(classFileUpTraveler);
                }
            }
        }

        // Then visit its subclasses, recursively.
        if (visitSubclasses)
        {
            ClassFile[] subClasses = libraryClassFile.subClasses;
            if (subClasses != null)
            {
                for (int i = 0; i < subClasses.length; i++)
                {
                    subClasses[i].accept(classFileDownTraveler);
                }
            }
        }
    }


    // Implementations for CpInfoVisitor

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo) {}
    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}


    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        ClassFile referencedClassFile = classCpInfo.referencedClassFile;
        if (referencedClassFile != null)
        {
            referencedClassFile.accept(this);
        }
    }
}
