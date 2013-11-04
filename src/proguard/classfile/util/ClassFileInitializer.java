/* $Id: ClassFileInitializer.java,v 1.8 2002/05/23 21:21:12 eric Exp $
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
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.visitor.*;

import java.util.*;


/**
 * This ClassFileVisitor initializes itself and all of its elements.
 *
 * @author Eric Lafortune
 */
public class ClassFileInitializer
  implements ClassFileVisitor,
             MemberInfoVisitor,
             CpInfoVisitor
{
    // A visitor info flag to indicate the visitor accepter has been initialized.
    private static final Object INITIALIZED = new Object();


    private ClassPool programClassPool;
    private ClassPool libraryClassPool;
    private boolean   warn;
    private int       classFileWarningCount;
    private int       memberWarningCount;

    // A field acting as a parameter to the visitProgramClassFile and
    // visitLibraryClassFile methods.
    private ClassFile subclass;


    /**
     * Creates a new ClassFileInitializer that initializes all visited class files,
     * printing warnings if some items can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool)
    {
        this(programClassPool, libraryClassPool, true);
    }


    /**
     * Creates a new ClassFileInitializer that initializes all visited class files.
     * If desired, the initializer will print warnings if some items can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool,
                                boolean   warn)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
        this.warn             = warn;
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * super classes or interfaces.
     */
    public int getClassFileWarningCount()
    {
        return classFileWarningCount;
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * class members in program class files.
     */
    public int getMemberWarningCount()
    {
        return memberWarningCount;
    }


    // Implementations for ClassFileVisitor

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Is this some other classes superclass?
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
            subclass = null;

            // Initialize the constant pool entries.
            programClassFile.constantPoolEntriesAccept(this);

            // Initialize all fields and methods.
            programClassFile.fieldsAccept(this);
            programClassFile.methodsAccept(this);

            // Restore the subclass marking.
            subclass = oldSubclass;
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Is this some other classes superclass?
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

            // See if we can find the superclass.
            String superClassName = libraryClassFile.superClassName;
            if (superClassName != null)
            {
                // Find and initialize the corresponding class file, and save
                // a reference to it.
                libraryClassFile.superClass =
                    findAndInitializeClassFile(libraryClassFile,
                                               superClassName,
                                               null,
                                               libraryClassPool);
            }

            // See if we can find the interfaces.
            if (libraryClassFile.interfaceNames != null)
            {
                String[]    interfaceNames   = libraryClassFile.interfaceNames;
                ClassFile[] interfaceClasses = new ClassFile[interfaceNames.length];

                for (int i = 0; i < interfaceNames.length; i++)
                {
                    // Find and initialize the corresponding class file, and
                    // save a reference to it.
                    interfaceClasses[i] =
                        findAndInitializeClassFile(libraryClassFile,
                                                   interfaceNames[i],
                                                   null,
                                                   libraryClassPool);
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


    // Implementations for MemberInfoVisitor

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
    {
        visitMemberInfo(programClassFile, programfieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMemberInfo(programClassFile, programMethodInfo);
    }


    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        programMemberInfo.referencedClassFiles =
            findReferencedClassFiles(programMemberInfo.getDescriptor(programClassFile));
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Implementations for CpInfoVisitor

    public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
    public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
    public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
    public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
    public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo) {}
    public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}


    public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo)
    {
        visitRefCpInfo(classFile, fieldrefCpInfo, true);
    }


    public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo)
    {
        visitRefCpInfo(classFile, interfaceMethodrefCpInfo, false);
    }


    public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
    {
        visitRefCpInfo(classFile, methodrefCpInfo, false);
    }


    private void visitRefCpInfo(ClassFile classFile, RefCpInfo refCpInfo, boolean isFieldRef)
    {
        String className = refCpInfo.getClassName(classFile);

        // See if we can find the referenced class file.
        // Only look for referenced class files in the program class pool.
        // Unresolved references are assumed to refer to library class files.
        ProgramClassFile referencedClassFile =
            (ProgramClassFile)programClassPool.getClass(className);
        if (referencedClassFile != null)
        {
            // Make sure the referenced class and its super classes are initialized.
            referencedClassFile.accept(this);

            String name = refCpInfo.getName(classFile);
            String type = refCpInfo.getType(classFile);

            // For efficiency, first see if we can find the member in the
            // referenced class itself.
            ProgramMemberInfo referencedMemberInfo = isFieldRef ?
                (ProgramMemberInfo)referencedClassFile.findField(name, type) :
                (ProgramMemberInfo)referencedClassFile.findMethod(name, type);

            if (referencedMemberInfo != null)
            {
                // Save the references.
                refCpInfo.referencedClassFile  = referencedClassFile;
                refCpInfo.referencedMemberInfo = referencedMemberInfo;
                return;
            }

            // We didn't find the member yet. Organize a search in the hierarchy
            // of super classes and interfaces. This can happen with classes
            // compiled in JDK 1.4.
            MyMemberFinder memberFinder = new MyMemberFinder();
            try {
                referencedClassFile.accept(
                    new ClassFileUpDownTraveler(true, true, true, false,
                        isFieldRef ?
                            (ClassFileVisitor)new NamedFieldVisitor(memberFinder, name, type) :
                            (ClassFileVisitor)new NamedMethodVisitor(memberFinder, name, type)));
            }
            catch (MyMemberFinder.MemberFoundException ex)
            {
                // Save the references.
                refCpInfo.referencedClassFile  = memberFinder.programClassFile;
                refCpInfo.referencedMemberInfo = memberFinder.programMemberInfo;
                return;
            }

            // We've visited the entire hierarchy and still no member...
            if (warn)
            {
                memberWarningCount++;
                System.err.println("Warning: " +
                                   ClassUtil.externalClassName(classFile.getName()) +
                                   ": can't find referenced " +
                                   (isFieldRef ?
                                    "field '"  + ClassUtil.externalFullFieldDescription(0, name, type) :
                                    "method '" + ClassUtil.externalFullMethodDescription(referencedClassFile.getName(), 0, name, type)) +
                                   "' in class " +
                                   ClassUtil.externalClassName(referencedClassFile.getName()));
            }
        }
    }


    public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
    {
        // Didn't we initialize this entry before (for the superclass or interfaces)?
        if (!isInitialized(classCpInfo))
        {
            // Mark this entry.
            markAsInitialized(classCpInfo);

            // Find and initialize the corresponding class file, and save a
            // reference to it.
            classCpInfo.referencedClassFile =
                findAndInitializeClassFile(classFile,
                                           classCpInfo.getName(classFile),
                                           programClassPool,
                                           subclass != null ?
                                               libraryClassPool :
                                               null);
        }
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        nameAndTypeCpInfo.referencedClassFiles =
            findReferencedClassFiles(nameAndTypeCpInfo.getType(classFile));
    }


    // Small utility methods.

    /**
     * Finds and intializes a given class.
     */
    private ClassFile findAndInitializeClassFile(ClassFile classFile,
                                                 String    className,
                                                 ClassPool programClassPool,
                                                 ClassPool libraryClassPool)
    {
        ClassFile referencedClassFile = null;

        // Do we have a program class pool to look in?
        if (programClassPool != null)
        {
            referencedClassFile = programClassPool.getClass(className);
        }

        // Do we have a library class pool to look in?
        if (libraryClassPool != null)
        {
            // Did we find a referenced class file yet?
            if (referencedClassFile == null)
            {
                // Try to find one in the library class pool.
                referencedClassFile = libraryClassPool.getClass(className);
            }

            // Did we find a referenced class file in either class pool?
            if (referencedClassFile != null)
            {
                // Descend to initialize it.
                referencedClassFile.accept(this);
            }
            else if (warn)
            {
                // We're only warning if we were asked to look in the library
                // class pool and still didn't find the class file.
                classFileWarningCount++;
                System.err.println("Warning: " +
                                   ClassUtil.externalClassName(classFile.getName()) +
                                   ": can't find superclass or interface " +
                                   ClassUtil.externalClassName(className));
            }
        }

        return referencedClassFile;
    }


    /**
     * Returns an array of class files referenced by the given descriptor, or
     * <code>null</code> if there aren't any useful references.
     */
    private ClassFile[] findReferencedClassFiles(String aDescriptor)
    {
        DescriptorClassEnumeration enumeration =
            new DescriptorClassEnumeration(aDescriptor);

        int classCount = enumeration.classCount();
        if (classCount > 0)
        {
            ClassFile[] referencedClassFiles = new ClassFile[classCount];

            boolean foundReferencedClassFiles = false;

            for (int i = 0; i < classCount; i++)
            {
                String name = enumeration.nextClassName();

                ClassFile referencedClassFile = programClassPool.getClass(name);

                if (referencedClassFile != null)
                {
                    referencedClassFiles[i] = referencedClassFile;
                    foundReferencedClassFiles = true;
                }
            }

            if (foundReferencedClassFiles)
            {
                return referencedClassFiles;
            }
        }

        return null;
    }


    /**
     * Initializes the given constant pool entry of the given class.
     */
    private void initializeCpEntry(ClassFile classFile, int index)
    {
         classFile.constantPoolEntryAccept(this, index);
    }


    static void markAsInitialized(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(INITIALIZED);
    }


    static boolean isInitialized(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == INITIALIZED;
    }


    /**
     * A utility class that throws a MemberFoundException whenever it visits
     * a member. For program class files, it then also stores the class file
     * and member info.
     */
    private static class MyMemberFinder implements MemberInfoVisitor
    {
        private static class MemberFoundException extends IllegalArgumentException {};
        private static final MemberFoundException MEMBER_FOUND = new MemberFoundException();

        private ProgramClassFile  programClassFile;
        private ProgramMemberInfo programMemberInfo;


        // Implementations for MemberInfoVisitor

        public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
        {
            this.programClassFile  = programClassFile;
            this.programMemberInfo = programFieldInfo;
            throw MEMBER_FOUND;
        }

        public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
        {
            this.programClassFile  = programClassFile;
            this.programMemberInfo = programMethodInfo;
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
