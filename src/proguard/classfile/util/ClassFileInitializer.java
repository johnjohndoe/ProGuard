/* $Id: ClassFileInitializer.java,v 1.16 2004/08/15 12:39:30 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
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
 * This ClassFileVisitor initializes the references and the class hierarchy of
 * all class files it visits.
 * <p>
 * Visited class files are added to the subclass list of their superclasses and
 * interfaces. These subclass lists make it more convenient to travel down the
 * class hierarchy.
 * <p>
 * All class constant pool entries get direct references to the corresponding
 * classes. These references make it more convenient to travel up and across
 * the class hierarchy.
 * <p>
 * All field and method reference constant pool entries get direct references
 * to the corresponding classes, fields, and methods.
 * <p>
 * All name and type constant pool entries get a list of direct references to
 * the classes listed in the type.
 * <p>
 * Visited library class files get direct references to their superclasses and
 * interfaces, replacing the superclass names and interface names. The direct
 * references are equivalent to the names, but they are more efficient to work
 * with.
 * <p>
 * This visitor optionally prints warnings if some items can't be found, and
 * notes on the usage of <code>(SomeClass)Class.forName(variable).newInstance()</code>.
 *
 * @author Eric Lafortune
 */
public class ClassFileInitializer
  implements ClassFileVisitor,
             MemberInfoVisitor,
             CpInfoVisitor,
             AttrInfoVisitor
{
    // A visitor info flag to indicate the class file has been initialized.
    private static final Object INITIALIZED = new Object();

    // A reusable object for checking whether referenced methods are Class.forName,...
    private ClassFileClassForNameReferenceInitializer classFileClassForNameReferenceInitializer;

    private ClassPool programClassPool;
    private ClassPool libraryClassPool;
    private boolean   warn;

    // Counters for warnings.
    private int hierarchyWarningCount;
    private int referenceWarningCount;


    /**
     * Creates a new ClassFileInitializer that initializes the hierarchy
     * of all visited class files, printing warnings if some classes can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool)
    {
        this(programClassPool, libraryClassPool, true, true);
    }


    /**
     * Creates a new ClassFileInitializer that initializes the hierarchy
     * of all visited class files, optionally printing warnings if some classes
     * can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool,
                                boolean   warn,
                                boolean   note)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
        this.warn             = warn;

        classFileClassForNameReferenceInitializer =
            new ClassFileClassForNameReferenceInitializer(programClassPool, note);
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * superclasses or interfaces.
     */
    public int getHierarchyWarningCount()
    {
        return hierarchyWarningCount;
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * class members in program class files.
     */
    public int getReferenceWarningCount()
    {
        return referenceWarningCount;
    }


    /**
     * Returns the number of notes printed about occurrences of
     * '<code>(SomeClass)Class.forName(variable).newInstance()</code>'.
     */
    public int getNoteCount()
    {
        return classFileClassForNameReferenceInitializer.getNoteCount();
    }


    // Implementations for ClassFileVisitor.

    public void visitProgramClassFile(ProgramClassFile programClassFile)
    {
        // Haven't we initialized this class before?
        if (!isInitialized(programClassFile))
        {
            // Mark this class.
            markAsInitialized(programClassFile);

            // Initialize the constant pool entries.
            programClassFile.constantPoolEntriesAccept(this);

            // Add this class to the subclasses of its superclass.
            if (programClassFile.u2superClass != 0)
            {
                addSubclass(programClassFile,
                            programClassFile.getSuperClass(),
                            programClassFile.getSuperName());
            }

            // Add this class to the subclasses of its interfaces.
            for (int i = 0; i < programClassFile.u2interfacesCount; i++)
            {
                addSubclass(programClassFile,
                            programClassFile.getInterface(i),
                            programClassFile.getInterfaceName(i));
            }

            // Initialize all fields and methods.
            programClassFile.fieldsAccept(this);
            programClassFile.methodsAccept(this);
        }
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Haven't we initialized this class before?
        if (!isInitialized(libraryClassFile))
        {
            // Mark this class.
            markAsInitialized(libraryClassFile);

            // Have a closer look at the superclass.
            String superClassName = libraryClassFile.superClassName;
            if (superClassName != null)
            {
                // Find and initialize the super class.
                ClassFile superClass = findAndInitializeClass(superClassName);

                // Add this class to the subclasses of its superclass,
                addSubclass(libraryClassFile,
                            superClass,
                            superClassName);

                // Keep a reference to the superclass.
                libraryClassFile.superClass = superClass;
            }

            // Have a closer look at the interface classes.
            if (libraryClassFile.interfaceNames != null)
            {
                String[]    interfaceNames   = libraryClassFile.interfaceNames;
                ClassFile[] interfaceClasses = new ClassFile[interfaceNames.length];

                for (int i = 0; i < interfaceNames.length; i++)
                {
                    // Find and initialize the interface class.
                    String    interfaceName  = interfaceNames[i];
                    ClassFile interfaceClass = findAndInitializeClass(interfaceName);

                    // Add this class to the subclasses of the interface class.
                    addSubclass(libraryClassFile,
                                interfaceClass,
                                interfaceName);

                    // Keep a reference to the interface class.
                    interfaceClasses[i] = interfaceClass;
                }

                libraryClassFile.interfaceClasses = interfaceClasses;
            }

            // Discard the name Strings. From now on, we'll use the object
            // references.
            libraryClassFile.superClassName = null;
            libraryClassFile.interfaceNames = null;
        }
    }


    // Implementations for MemberInfoVisitor.

    public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programFieldInfo)
    {
        visitMemberInfo(programClassFile, programFieldInfo);
    }


    public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
    {
        visitMemberInfo(programClassFile, programMethodInfo);
    }


    private void visitMemberInfo(ProgramClassFile programClassFile, ProgramMemberInfo programMemberInfo)
    {
        programMemberInfo.referencedClassFiles =
            findReferencedClassFiles(programMemberInfo.getDescriptor(programClassFile));

        // Initialize the attributes.
        programMemberInfo.attributesAccept(programClassFile, this);
    }


    public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo) {}
    public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo) {}


    // Implementations for CpInfoVisitor.

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
        // Unresolved references are assumed to refer to library class files
        // that will not change anyway.
        ClassFile referencedClassFile = findClass(className);

        if (referencedClassFile != null)
        {
            String name = refCpInfo.getName(classFile);
            String type = refCpInfo.getType(classFile);

            // For efficiency, first see if we can find the member in the
            // referenced class itself.
            MemberInfo referencedMemberInfo = isFieldRef ?
                (MemberInfo)referencedClassFile.findField(name, type) :
                (MemberInfo)referencedClassFile.findMethod(name, type);

            if (referencedMemberInfo != null)
            {
                // Save the references.
                refCpInfo.referencedClassFile  = referencedClassFile;
                refCpInfo.referencedMemberInfo = referencedMemberInfo;

                return;
            }

            // We didn't find the member yet. Organize a search in the hierarchy
            // of superclasses and interfaces. This can happen with classes
            // compiled with "-target 1.2" (the default in JDK 1.4).
            MyMemberFinder memberFinder = new MyMemberFinder();
            try {
                referencedClassFile.hierarchyAccept(true, true, true, false,
                    isFieldRef ?
                        (ClassFileVisitor)new NamedFieldVisitor(memberFinder, name, type) :
                        (ClassFileVisitor)new NamedMethodVisitor(memberFinder, name, type));
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
                referenceWarningCount++;
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
        classCpInfo.referencedClassFile =
            findAndInitializeClass(classCpInfo.getName(classFile));
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        nameAndTypeCpInfo.referencedClassFiles =
            findReferencedClassFiles(nameAndTypeCpInfo.getType(classFile));
    }


    // Implementations for AttrInfoVisitor.

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo) {}
    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo) {}
    public void visitConstantValueAttrInfo(ClassFile classFile, FieldInfo fieldInfo, ConstantValueAttrInfo constantValueAttrInfo) {}
    public void visitExceptionsAttrInfo(ClassFile classFile, MethodInfo methodInfo, ExceptionsAttrInfo exceptionsAttrInfo) {}
    public void visitLineNumberTableAttrInfo(ClassFile classFile, MethodInfo methodInfo, CodeAttrInfo codeAttrInfo, LineNumberTableAttrInfo lineNumberTableAttrInfo) {}
    public void visitLocalVariableTableAttrInfo(ClassFile classFile, MethodInfo methodInfo, CodeAttrInfo codeAttrInfo, LocalVariableTableAttrInfo localVariableTableAttrInfo) {}
    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo) {}
    public void visitSourceDirAttrInfo(ClassFile classFile, SourceDirAttrInfo sourceDirAttrInfo) {}
    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo) {}
    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo) {}
    public void visitSignatureAttrInfo(ClassFile classFile, SignatureAttrInfo signatureAttrInfo) {}


    public void visitCodeAttrInfo(ClassFile classFile, MethodInfo methodInfo, CodeAttrInfo codeAttrInfo)
    {
        codeAttrInfo.instructionsAccept(classFile, methodInfo, classFileClassForNameReferenceInitializer);
    }


    // Small utility methods.

    /**
     * Finds and initializes a class with the given name.
     *
     * @see #findClass(String)
     */
    private ClassFile findAndInitializeClass(String name)
    {
        // Try to find the class file.
        ClassFile referencedClassFile = findClass(name);

        // Did we find the referenced class file in either class pool?
        if (referencedClassFile != null)
        {
            // Initialize it.
            referencedClassFile.accept(this);
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

                ClassFile referencedClassFile = findClass(name);

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
     * Returns the class with the given name, either for the program class pool
     * or from the library class pool, or <code>null</code> if it can't be found.
     */
    private ClassFile findClass(String name)
    {
        // First look for the class in the program class pool.
        ClassFile classFile = programClassPool.getClass(name);

        // Otherwise look for the class in the library class pool.
        if (classFile == null)
        {
            classFile = libraryClassPool.getClass(name);
        }

        return classFile;
    }


    private static void markAsInitialized(VisitorAccepter visitorAccepter)
    {
        visitorAccepter.setVisitorInfo(INITIALIZED);
    }


    private static boolean isInitialized(VisitorAccepter visitorAccepter)
    {
        return visitorAccepter.getVisitorInfo() == INITIALIZED;
    }


    private void addSubclass(ClassFile subclass,
                             ClassFile classFile,
                             String    className)
    {
        if (classFile != null)
        {
            classFile.addSubClass(subclass);
        }
        else if (warn)
        {
            // We didn't find the superclass or interface. Print a warning.
            hierarchyWarningCount++;
            System.err.println("Warning: " +
                               ClassUtil.externalClassName(subclass.getName()) +
                               ": can't find superclass or interface " +
                               ClassUtil.externalClassName(className));
        }
    }


    /**
     * This utility class throws a MemberFoundException whenever it visits
     * a member. For program class files, it then also stores the class file
     * and member info.
     */
    private static class MyMemberFinder implements MemberInfoVisitor
    {
        private static class MemberFoundException extends IllegalArgumentException {};
        private static final MemberFoundException MEMBER_FOUND = new MemberFoundException();

        private ProgramClassFile  programClassFile;
        private ProgramMemberInfo programMemberInfo;


        // Implementations for MemberInfoVisitor.

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
