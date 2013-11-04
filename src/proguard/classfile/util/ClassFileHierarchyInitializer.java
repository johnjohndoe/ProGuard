/* $Id: ClassFileHierarchyInitializer.java,v 1.4 2003/12/06 22:15:38 eric Exp $
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
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.visitor.*;


/**
 * This ClassFileVisitor initializes the class hierarchy of all class files it
 * visits.
 * <p>
 * The superclass and interfaces of each visited class file get the class file
 * in their subclass lists. These subclass lists make it more convenient to travel
 * down the class hierarchy.
 * <p>
 * Class constant pool entries pointing to superclasses and interfaces get
 * direct references to their classes. These references make it more convenient
 * to travel up the class hierarchy.
 * <p>
 * Library class files get direct references to their superclasses and
 * interfaces, replacing the superclass names and interface names. The direct
 * references are equivalent to the names, but they are more efficient to work
 * with. Again, these references make it more convenient to travel up the
 * (library) class hierarchy.
 *
 * @author Eric Lafortune
 */
public class ClassFileHierarchyInitializer
  implements ClassFileVisitor,
             CpInfoVisitor
{
    // A visitor info flag to indicate the class file has been initialized.
    private static final Object INITIALIZED = new Object();


    private ClassPool programClassPool;
    private ClassPool libraryClassPool;
    private boolean   warn;

    // Counter for warnings.
    private int warningCount;

    // A field acting as a parameter to the visitProgramClassFile and
    // visitLibraryClassFile methods. It indicates that the visited class
    // is a superclass of this subclass.
    private ClassFile subclass;


    /**
     * Creates a new ClassFileHierarchyInitializer that initializes the hierarchy
     * of all visited class files, printing warnings if some classes can't be found.
     */
    public ClassFileHierarchyInitializer(ClassPool programClassPool,
                                         ClassPool libraryClassPool)
    {
        this(programClassPool, libraryClassPool, true);
    }


    /**
     * Creates a new ClassFileHierarchyInitializer that initializes the hierarchy
     * of all visited class files, optionally printing warnings if some classes
     * can't be found.
     */
    public ClassFileHierarchyInitializer(ClassPool programClassPool,
                                         ClassPool libraryClassPool,
                                         boolean   warn)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
        this.warn             = warn;
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * superclasses or interfaces.
     */
    public int getWarningCount()
    {
        return warningCount;
    }


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Is this some other class's superclass?
        if (subclass != null)
        {
            programClassFile.addSubClass(subclass);
        }

        // Didn't we initialize this class before?
        if (!isInitialized(programClassFile))
        {
            // Mark this class.
            markAsInitialized(programClassFile);

            // Start marking superclasses and interfaces of this class.
            ClassFile oldSubclass = subclass;
            subclass = programClassFile;

            // Initialize the superclass recursively.
            if (programClassFile.u2superClass != 0)
            {
                initializeCpEntry(programClassFile,
                                  programClassFile.u2superClass);
            }

            // Initialize the interfaces recursively.
            for (int i = 0; i < programClassFile.u2interfacesCount; i++)
            {
                initializeCpEntry(programClassFile,
                                  programClassFile.u2interfaces[i]);
            }

            // Stop marking superclasses and interfaces of this class.
            subclass = oldSubclass;
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Is this some other class's superclass?
        if (subclass != null)
        {
            libraryClassFile.addSubClass(subclass);
        }

        // Didn't we initialize this class before?
        if (!isInitialized(libraryClassFile))
        {
            // Mark this class.
            markAsInitialized(libraryClassFile);

            // Start marking superclasses and interfaces of this class.
            ClassFile oldSubclass = subclass;
            subclass = libraryClassFile;

            String className = libraryClassFile.getName();

            // Initialize the superclass recursively.
            // Also save a reference to it.
            String superClassName = libraryClassFile.superClassName;
            if (superClassName != null)
            {
                libraryClassFile.superClass =
                    initializeReferencedClassFile(className,
                                                  superClassName);
            }

            // Initialize the interfaces recursively.
            // Also save references to them.
            if (libraryClassFile.interfaceNames != null)
            {
                String[]    interfaceNames   = libraryClassFile.interfaceNames;
                ClassFile[] interfaceClasses = new ClassFile[interfaceNames.length];

                for (int i = 0; i < interfaceNames.length; i++)
                {
                    interfaceClasses[i] =
                        initializeReferencedClassFile(className,
                                                      interfaceNames[i]);
                }

                libraryClassFile.interfaceClasses = interfaceClasses;
            }

            // Stop marking superclasses and interfaces of this class.
            subclass = oldSubclass;

            // Discard the name Strings. From now on, we'll use the object
            // references.
            libraryClassFile.superClassName = null;
            libraryClassFile.interfaceNames = null;
        }
    }


    // Implementations for CpInfoVisitor.

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo) {}
    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}


    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        classCpInfo.referencedClassFile =
            initializeReferencedClassFile(classFile.getName(),
                                          classCpInfo.getName(classFile));
    }


    // Small utility methods.

    /**
     * Initializes the given constant pool entry of the given class.
     */
    private void initializeCpEntry(ClassFile classFile, int index)
    {
         classFile.constantPoolEntryAccept(this, index);
    }


    /**
     * Finds and intializes a given referenced class.
     */
    private ClassFile initializeReferencedClassFile(String className,
                                                    String referencedClassName)
    {
        // Try to find it in the program class pool.
        ClassFile referencedClassFile = programClassPool.getClass(referencedClassName);

        // Did we find it yet?
        if (referencedClassFile == null)
        {
            // Try to find it in the library class pool.
            referencedClassFile = libraryClassPool.getClass(referencedClassName);
        }

        // Did we find the referenced class file in either class pool?
        if (referencedClassFile != null)
        {
            // Initialize it.
            referencedClassFile.accept(this);
        }
        else if (warn)
        {
            // We didn't find the superclass or interface. Print a warning.
            warningCount++;
            System.err.println("Warning: " +
                               ClassUtil.externalClassName(className) +
                               ": can't find superclass or interface " +
                               ClassUtil.externalClassName(referencedClassName));
        }

        return referencedClassFile;
    }


    private static void markAsInitialized(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(INITIALIZED);
    }


    private static boolean isInitialized(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == INITIALIZED;
    }
}
