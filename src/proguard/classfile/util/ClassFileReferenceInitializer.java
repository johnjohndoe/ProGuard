/* $Id: ClassFileReferenceInitializer.java,v 1.4 2002/11/03 13:30:14 eric Exp $
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


/**
 * This ClassFileVisitor initializes the elements of all class files it visits.
 * The class file hierarchy must be initialized before using this visitor.
 * <p>
 * The visitor fills out the references of fields, methods, and constant pool
 * entries that refer to a class file or to a class member in the program class
 * pool.
 *
 * @see proguard.classfile.util.ClassFileHierarchyVisitor
 * @author Eric Lafortune
 */
public class ClassFileReferenceInitializer
  implements ClassFileVisitor,
             MemberInfoVisitor,
             CpInfoVisitor,
             AttrInfoVisitor,
             InstructionVisitor
{
    private ClassPool programClassPool;
    private boolean   warn;
    private boolean   note;

    // Counters for warnings and notes.
    private int       warningCount;
    private int       noteCount;

    // Helper class for checking whether referenced methods are Class.forName,...
    private MyClassForNameFinder classForNameFinder = new MyClassForNameFinder();

    // Fields to remember the previous StringCpInfo and MethodRefCpInfo objects
    // while visiting all instructions (to find Class.forName, class$, and
    // Class.newInstance invocations, and possible class casts afterwards).
    private int ldcStringCpIndex              = -1;
    private int invokestaticMethodRefCpIndex  = -1;
    private int invokevirtualMethodRefCpIndex = -1;


    /**
     * Creates a new ClassFileReferenceInitializer that initializes the elements
     * of all visited class files, printing warnings if some items can't be found.
     * It also prints notes if on usage of
     * <code>(SomeClass)Class.forName(variable).newInstance()</code>.
     */
    public ClassFileReferenceInitializer(ClassPool programClassPool)
    {
        this(programClassPool, true, true);
    }


    /**
     * Creates a new ClassFileReferenceInitializer that initializes the elements
     * of all visited class files, optionally printing warnings if some items
     * can't be found. It also optionally prints notes if on usage of
     * <code>(SomeClass)Class.forName(variable).newInstance()</code>.
     */
    public ClassFileReferenceInitializer(ClassPool programClassPool,
                                         boolean   warn,
                                         boolean   note)
    {
        this.programClassPool = programClassPool;
        this.warn             = warn;
        this.note             = note;
    }


    /**
     * Returns the number of warnings printed about unresolved references to
     * class members in program class files.
     */
    public int getWarningCount()
    {
        return warningCount;
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
        // Initialize the constant pool entries.
        programClassFile.constantPoolEntriesAccept(this);

        // Initialize all fields and methods.
        programClassFile.fieldsAccept(this);
        programClassFile.methodsAccept(this);
    }


    public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
    {
        // Library class files have no elements to be initialized.
    }


    // Implementations for MemberInfoVisitor

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
            // of superclasses and interfaces. This can happen with classes
            // compiled with "-target 1.2" (the default in JDK 1.4).
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
                warningCount++;
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
        // Didn't the reference get filled out already by the hierarchy initializer?
        if (classCpInfo.referencedClassFile == null)
        {
            classCpInfo.referencedClassFile =
                programClassPool.getClass(classCpInfo.getName(classFile));
        }
    }


    public void visitNameAndTypeCpInfo(ClassFile classFile, NameAndTypeCpInfo nameAndTypeCpInfo)
    {
        nameAndTypeCpInfo.referencedClassFiles =
            findReferencedClassFiles(nameAndTypeCpInfo.getType(classFile));
    }


    // Implementations for AttrInfoVisitor

    public void visitUnknownAttrInfo(ClassFile classFile, UnknownAttrInfo unknownAttrInfo) {}
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
        switch (instruction.getOpcode())
        {
            case Instruction.OP_ICONST_0:
            case Instruction.OP_ICONST_1:
                // Still remember any loaded string; it might be the argument of
                // class$(String, boolean).
                break;

            default:
                ldcStringCpIndex = -1;
                break;
        }

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
                    // Remember it; it might be the argument of
                    // Class.forName(String), class$(String), or
                    // class$(String, boolean).
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
     * a note of it is printed.
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
            // Note that .class is implemented as "static Class class$(String)"
            // or as "static Class class$(String, boolean)".
            isDotClassInvocation =
                className .equals(classFile.getName())                           &&
                methodName.equals(ClassConstants.INTERNAL_METHOD_NAME_DOT_CLASS) &&
                (methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_DOT_CLASS_JAVAC) ||
                 methodType.equals(ClassConstants.INTERNAL_METHOD_TYPE_DOT_CLASS_JIKES));
        }


        public void visitStringCpInfo(ClassFile classFile, StringCpInfo stringCpInfo)
        {
            if (isClassForNameInvocation ||
                isDotClassInvocation)
            {
                // Save a reference to the corresponding class file.
                String externalClassName = stringCpInfo.getString(classFile);
                String internalClassName = ClassUtil.internalClassName(externalClassName);

                stringCpInfo.referencedClassFile =
                    programClassPool.getClass(internalClassName);
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
