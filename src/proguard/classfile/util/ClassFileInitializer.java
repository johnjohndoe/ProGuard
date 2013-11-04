/* $Id: ClassFileInitializer.java,v 1.11 2002/07/13 16:55:21 eric Exp $
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
import proguard.classfile.instruction.*;
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
             CpInfoVisitor,
             AttrInfoVisitor,
             InstructionVisitor
{
    // A visitor info flag to indicate the visitor accepter has been initialized.
    private static final Object INITIALIZED = new Object();


    private ClassPool programClassPool;
    private ClassPool libraryClassPool;
    private boolean   warn;
    private boolean   note;
    private int       classFileWarningCount;
    private int       memberWarningCount;
    private int       noteCount;

    // A field acting as a parameter to the visitProgramClassFile and
    // visitLibraryClassFile methods.
    private ClassFile subclass;

    // Helper class for checking whether refernced methods are Class.forName,...
    private MyClassForNameFinder classForNameFinder = new MyClassForNameFinder();

    // Fields to remember the previous StringCpInfo and MethodRefCpInfo objects
    // while visiting all instructions (to find Class.forName, class$, and
    // Class.newInstance invocations, and possible class casts afterwards).
    private int ldcStringCpIndex              = -1;
    private int invokestaticMethodRefCpIndex  = -1;
    private int invokevirtualMethodRefCpIndex = -1;


    /**
     * Creates a new ClassFileInitializer that initializes all visited class files,
     * printing warnings if some items can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool)
    {
        this(programClassPool, libraryClassPool, true, true);
    }


    /**
     * Creates a new ClassFileInitializer that initializes all visited class files.
     * If desired, the initializer will print warnings if some items can't be found.
     */
    public ClassFileInitializer(ClassPool programClassPool,
                                ClassPool libraryClassPool,
                                boolean   warn,
                                boolean   note)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
        this.warn             = warn;
        this.note             = note;
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


    /**
     * Returns the number of notes printed about occurrences of
     * '<code>(SomeClass)Class.forName(variable).newInstance()</code>'.
     */
    public int getNoteCount()
    {
        return noteCount;
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

      // Initialize the attributes.
      programMemberInfo.attributesAccept(programClassFile, this);
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


    // Implementations for AttrInfoVisitor

    public void visitAttrInfo(ClassFile classFile, AttrInfo attrInfo) {}
    public void visitInnerClassesAttrInfo(ClassFile classFile, InnerClassesAttrInfo innerClassesAttrInfo) {}
    public void visitConstantValueAttrInfo(ClassFile classFile, ConstantValueAttrInfo constantValueAttrInfo) {}
    public void visitExceptionsAttrInfo(ClassFile classFile, ExceptionsAttrInfo exceptionsAttrInfo) {}
    public void visitLineNumberTableAttrInfo(ClassFile classFile, LineNumberTableAttrInfo lineNumberTableAttrInfo) {}
    public void visitLocalVariableTableAttrInfo(ClassFile classFile, LocalVariableTableAttrInfo localVariableTableAttrInfo) {}
    public void visitSourceFileAttrInfo(ClassFile classFile, SourceFileAttrInfo sourceFileAttrInfo) {}
    public void visitDeprecatedAttrInfo(ClassFile classFile, DeprecatedAttrInfo deprecatedAttrInfo) {}
    public void visitSyntheticAttrInfo(ClassFile classFile, SyntheticAttrInfo syntheticAttrInfo) {}


    public void visitCodeAttrInfo(ClassFile classFile, CodeAttrInfo codeAttrInfo)
    {
        codeAttrInfo.instructionsAccept(classFile, this);
    }


    // Implementations for InstructionVisitor

    public void visitInstruction(ClassFile classFile, Instruction instruction)
    {
        // Just ignore generic instructions and reset the constant pool indices.
        ldcStringCpIndex              = -1;
        invokestaticMethodRefCpIndex  = -1;
        invokevirtualMethodRefCpIndex = -1;
    }


    public void visitCpInstruction(ClassFile classFile, CpInstruction cpInstruction)
    {
        int currentCpIndex = cpInstruction.getCpIndex();

        switch (cpInstruction.getOpcode())
        {
            case Instruction.OP_LDC:
            case Instruction.OP_LDC_WIDE:
                // Are we loading a constant String?
                int currentCpTag = classFile.getCpTag(currentCpIndex);
                if (currentCpTag == ClassConstants.CONSTANT_String)
                {
                    // Remember it; it might be the argument of Class.forName.
                    ldcStringCpIndex = currentCpIndex;
                }
                invokestaticMethodRefCpIndex  = -1;
                invokevirtualMethodRefCpIndex = -1;
                break;

            case Instruction.OP_INVOKESTATIC:
                // Are we invoking a static method that might have a constant
                // String argument?
                if (ldcStringCpIndex > 0)
                {
                    classForNameFinder.reset();
                    // First check whether the method reference points to Class.forName.
                    classFile.constantPoolEntryAccept(classForNameFinder, currentCpIndex);
                    // Then fill out the class file reference in the String, if applicable.
                    classFile.constantPoolEntryAccept(classForNameFinder, ldcStringCpIndex);

                    invokestaticMethodRefCpIndex = -1;
                }
                else
                {
                    // Just remember it; it might still be a Class.forName.
                    invokestaticMethodRefCpIndex = currentCpIndex;
                }

                ldcStringCpIndex              = -1;
                invokevirtualMethodRefCpIndex = -1;
                break;

            case Instruction.OP_INVOKEVIRTUAL:
                // Are we invoking a virtual method right after a static method?
                if (invokestaticMethodRefCpIndex > 0)
                {
                    // Remember it; it might be Class.newInstance after a Class.forName.
                    invokevirtualMethodRefCpIndex = currentCpIndex;
                }
                else
                {
                    invokestaticMethodRefCpIndex  = -1;
                    invokevirtualMethodRefCpIndex = -1;
                }

                ldcStringCpIndex = -1;
                break;

            case Instruction.OP_CHECKCAST:
                // Are we checking a cast right after a static method and a
                // virtual method?
                if (invokestaticMethodRefCpIndex  > 0 &&
                    invokevirtualMethodRefCpIndex > 0)
                {
                    classForNameFinder.reset();
                    // First check whether the first method reference points to Class.forName.
                    classFile.constantPoolEntryAccept(classForNameFinder, invokestaticMethodRefCpIndex);
                    // Then check whether the second method reference points to Class.newInstance.
                    classFile.constantPoolEntryAccept(classForNameFinder, invokevirtualMethodRefCpIndex);
                    // Then figure out which class is being cast to.
                    classFile.constantPoolEntryAccept(classForNameFinder, currentCpIndex);
                }

                ldcStringCpIndex              = -1;
                invokestaticMethodRefCpIndex  = -1;
                invokevirtualMethodRefCpIndex = -1;
                break;

            default:
                // Nothing interesting; just forget about previous indices.
                ldcStringCpIndex              = -1;
                invokestaticMethodRefCpIndex  = -1;
                invokevirtualMethodRefCpIndex = -1;
                break;
        }
    }


    /**
     * This CpInfoVisitor is designed to visit one or two method references first,
     * and then a string or a class reference.
     * If the method reference is Class.forName or .class, the class file
     * reference of the string is filled out.
     * If the method reference is Class.forName and then Class.newInstance,
     * a note of it is made.
     */
    private class MyClassForNameFinder implements CpInfoVisitor
    {
        private boolean isClassForNameInvocation;
        private boolean isDotClassInvocation;
        private boolean isClassForNameInstanceInvocation;

        public void reset()
        {
            isClassForNameInvocation         = false;
            isDotClassInvocation             = false;
            isClassForNameInstanceInvocation = false;
        }


        public void visitIntegerCpInfo(ClassFile classFile, IntegerCpInfo integerCpInfo) {}
        public void visitLongCpInfo(ClassFile classFile, LongCpInfo longCpInfo) {}
        public void visitFloatCpInfo(ClassFile classFile, FloatCpInfo floatCpInfo) {}
        public void visitDoubleCpInfo(ClassFile classFile, DoubleCpInfo doubleCpInfo) {}
        public void visitUtf8CpInfo(ClassFile classFile, Utf8CpInfo utf8CpInfo) {}
        public void visitFieldrefCpInfo(ClassFile classFile, FieldrefCpInfo fieldrefCpInfo) {}
        public void visitInterfaceMethodrefCpInfo(ClassFile classFile, InterfaceMethodrefCpInfo interfaceMethodrefCpInfo) {}
        public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo) {}


        public void visitMethodrefCpInfo(ClassFile classFile, MethodrefCpInfo methodrefCpInfo)
        {
            String className  = methodrefCpInfo.getClassName(classFile);
            String methodName = methodrefCpInfo.getName(classFile);
            String methodType = methodrefCpInfo.getType(classFile);

            // Is it a reference to Class.newInstance, following a reference to
            // Class.forName?
            isClassForNameInstanceInvocation =
                isClassForNameInvocation                                              &&
                className .equals(ClassConstants.INTERNAL_CLASS_NAME_JAVA_LANG_CLASS) &&
                methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_NEW_INSTANCE)   &&
                methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_NEW_INSTANCE);

            // Is it a reference to Class.forName?
            isClassForNameInvocation =
                className .equals(ClassConstants.INTERNAL_CLASS_NAME_JAVA_LANG_CLASS) &&
                methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_CLASS_FOR_NAME) &&
                methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_CLASS_FOR_NAME);

            // Is it a reference to .class?
            // Note that .class is implemented as "static Class class$(String)".
            isDotClassInvocation =
                className .equals(classFile.getName())                           &&
                methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_DOT_CLASS) &&
                methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_DOT_CLASS);
        }


        public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
        {
            if (isClassForNameInvocation ||
                isDotClassInvocation)
            {
                // Find and initialize the corresponding class file, and save a
                // reference to it.
                String externalClassName = stringCpInfo.getString(classFile);
                String internalClassName = ClassUtil.internalClassName(externalClassName);

                stringCpInfo.referencedClassFile =
                    findAndInitializeClassFile(classFile,
                                               internalClassName,
                                               programClassPool,
                                               null);
            }
        }


        public void visitClassCpInfo(ClassFile classFile, ClassCpInfo classCpInfo)
        {
            if (isClassForNameInstanceInvocation)
            {
                if (note)
                {
                    noteCount++;
                    System.err.println("Note: " +
                                       ClassUtil.externalClassName(classFile.getName()) +
                                       " calls '(" +
                                       ClassUtil.externalClassName(classCpInfo.getName(classFile)) +
                                       ")Class.forName(variable).newInstance()'");
                }
            }
        }
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
