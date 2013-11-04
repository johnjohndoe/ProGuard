/* $Id: KeepCommand.java,v 1.11 2002/08/02 16:40:28 eric Exp $
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
package proguard;

import proguard.classfile.*;
import proguard.classfile.visitor.*;
import proguard.shrink.*;
import proguard.obfuscate.*;


/**
 * This <code>Command</code> marks specified classes and/or class members to be
 * kept from shrinking and/or obfuscation.
 *
 * @author Eric Lafortune
 */
public class KeepCommand implements Command
{
    // The visitors that delegate to the usage marker and name marker,
    // depending on the phase of the program execution.
    private VariableClassFileVisitor     variableClassFileVisitor =
        new VariableClassFileVisitor();
    private VariableMemberInfoVisitor variableMemberInfoVisitor =
        new VariableMemberInfoVisitor();

    // The visitors that delegate to lists of class file visitors like the
    // ones above.
    // The first one is for keeping class files and possibly the one below.
    private MultiClassFileVisitor        multiClassFileVisitor =
        new MultiClassFileVisitor();
    // The second one is for keeping their class members.
    private MultiClassFileVisitor        multiClassFileVisitorForMembers;

    // The main class pool visitor.
    private ClassPoolVisitor             classPoolMarker;

    // Properties that define the behavior of this Command.
    private String                       extendsClassName;
    private boolean                      markConditionally;
    private boolean                      onlyKeepNames;

    // Flags and counters to check how many class members are being visited.
    private boolean                      countingMembers;
    private int                          memberCounter;
    private int                          memberVisitCounter;

    /**
     * Creates a new command that instructs to keep the specified class(es).
     *
     * @param requiredSetAccessFlags   the class access flags that must be set
     *                                 in order for the class to apply.
     * @param requiredUnsetAccessFlags the class access flags that must be unset
     *                                 in order for the class to apply.
     * @param className                the class name. The name may be null to
     *                                 specify any class.
     * @param extendsClassName         the name of the class that the class must
     *                                 extend or implement in order to apply.
     *                                 The name may be null to specify any class.
     * @param asClassName              the new name that the class should get.
     *                                 A value of null specifies the original
     *                                 name.
     * @param markClassFiles           specifies whether to mark the class files.
     *                                 If false, only class members are marked.
     *                                 If true, the class files are marked as
     *                                 well.
     * @param markConditionally        specifies whether to mark the class files
     *                                 and class members conditionally.
     *                                 If true, class files and class members
     *                                 are marked, on the condition that all
     *                                 specified class members are present.
     * @param onlyKeepNames            specifies whether the class files and class
     *                                 members need to be kept from obfuscation
     *                                 only.
     *                                 If true, the specified class class files
     *                                 and class members will be kept from
     *                                 obfuscation, but the may be removed in
     *                                 the shrinking phase.
     *                                 If false, they will be kept from
     *                                 shrinking and obfuscation alike.
     */
    public KeepCommand(int     requiredSetAccessFlags,
                       int     requiredUnsetAccessFlags,
                       String  className,
                       String  extendsClassName,
                       String  asClassName,
                       boolean markClassFiles,
                       boolean markConditionally,
                       boolean onlyKeepNames)
    {
        this.extendsClassName  = extendsClassName;
        this.markConditionally = markConditionally;
        this.onlyKeepNames     = onlyKeepNames;

        // The visitor is a multi-class file visitor that is empty initially.
        // Visitors to mark class files and class members will be added in a
        // moment.
        ClassFileVisitor classFileVisitor = multiClassFileVisitor;

        // If specified, let the marker visit the class file itself.
        if (markClassFiles || markConditionally)
        {
            multiClassFileVisitor.addClassFileVisitor(variableClassFileVisitor);
        }

        // If specified, let the marker visit the class file and its class
        // members conditionally.
        if (markConditionally)
        {
            ensureMultiClassFileVisitorForMembers();

            MultiClassFileVisitor newMultiClassFileVisitor =
                new MultiClassFileVisitor();

            // We'll visit class files, subsequently doing the following:
            // (1) go into counting mode and reset the class member visit counter,
            // (2) count class members,
            // (3) go into conditional mode,
            // (4) conditionally mark the class file and its class members.
            newMultiClassFileVisitor.addClassFileVisitor(
                new MemberVisitCounterResetter(true));
            newMultiClassFileVisitor.addClassFileVisitor(
                multiClassFileVisitorForMembers);
            newMultiClassFileVisitor.addClassFileVisitor(
                new MemberVisitCounterResetter(false));
            newMultiClassFileVisitor.addClassFileVisitor(
                new ConditionalClassFileVisitor(multiClassFileVisitor));

            classFileVisitor = newMultiClassFileVisitor;
        }

        // If specified, only visit class files with the right access flags.
        if (requiredSetAccessFlags   != 0 ||
            requiredUnsetAccessFlags != 0)
        {
            classFileVisitor =
                new FilteredClassFileVisitor(classFileVisitor,
                                             requiredSetAccessFlags,
                                             requiredUnsetAccessFlags);
        }

        // Depending on what's specified, visit a single named class,
        // all classes that extend a particular named class, or all classes.
        classPoolMarker =
            className        != null ? (ClassPoolVisitor)
                new NamedClassFileVisitor(classFileVisitor, className) :

            extendsClassName != null ? (ClassPoolVisitor)
                new NamedClassFileVisitor(new ClassFileUpDownTraveler(false, false, false, true,
                                                                      classFileVisitor),
                                          extendsClassName)
                                     : (ClassPoolVisitor)

                new AllClassFileVisitor(classFileVisitor);
    }


    /**
     * Instructs to keep the specified field(s) of this command's class(es).
     *
     * @param requiredSetAccessFlags   the field access flags that must be set
     *                                 in order for the field to apply.
     * @param requiredUnsetAccessFlags the field access flags that must be unset
     *                                 in order for the field to apply.
     * @param name                     the field name. The name may be null to
     *                                 specify any field.
     * @param descriptor               the field descriptor. The descriptor may
     *                                 be null to specify any field.
     * @param asName                   the new name that the field should get.
     *                                 A value of null specifies the original
     *                                 name. (currently ignored)
     */
    public void keepField(int    requiredSetAccessFlags,
                          int    requiredUnsetAccessFlags,
                          String name,
                          String descriptor,
                          String asName)
    {
        MemberInfoVisitor memberInfoVisitor = variableMemberInfoVisitor;

        // If required, count the number of specified class members and class
        // member visits.
        if (markConditionally)
        {
            memberCounter++;

            memberInfoVisitor =
                new ConditionalMemberInfoVisitor(memberInfoVisitor);
        }

        // If specified, only visit fields with the right access flags.
        if (requiredSetAccessFlags   != 0 ||
            requiredUnsetAccessFlags != 0)
        {
            memberInfoVisitor =
                new FilteredMemberInfoVisitor(memberInfoVisitor,
                                                 requiredSetAccessFlags,
                                                 requiredUnsetAccessFlags);
        }

        // Depending on what's specified, visit a single named field or all fields.
        ClassFileVisitor marker =
            name != null ? (ClassFileVisitor)
                new NamedFieldVisitor(memberInfoVisitor, name, descriptor)
                         : (ClassFileVisitor)
                new AllFieldVisitor(memberInfoVisitor);

        ensureMultiClassFileVisitorForMembers();
        multiClassFileVisitorForMembers.addClassFileVisitor(marker);
    }


    /**
     * Instructs to keep the specified method(s) of this command's class(es).
     *
     * @param requiredSetAccessFlags   the method access flags that must be set
     *                                 in order for the method to apply.
     * @param requiredUnsetAccessFlags the method access flags that must be unset
     *                                 in order for the method to apply.
     * @param name                     the method name. The name may be null to
     *                                 specify any method.
     * @param descriptor               the method descriptor. The descriptor may
     *                                 be null to specify any method.
     * @param asName                   the new name that the method should get.
     *                                 A value of null specifies the original
     *                                 name. (currently ignored)
     */
    public void keepMethod(int    requiredSetAccessFlags,
                           int    requiredUnsetAccessFlags,
                           String name,
                           String descriptor,
                           String asName)
    {
        MemberInfoVisitor memberInfoVisitor = variableMemberInfoVisitor;

        // If required, count the number of specified class members and class
        // member visits.
        if (markConditionally)
        {
            memberCounter++;

            memberInfoVisitor =
                new ConditionalMemberInfoVisitor(memberInfoVisitor);
        }

        // If specified, only visit methods with the right access flags.
        if (requiredSetAccessFlags   != 0 ||
            requiredUnsetAccessFlags != 0)
        {
            memberInfoVisitor =
                new FilteredMemberInfoVisitor(memberInfoVisitor,
                                                 requiredSetAccessFlags,
                                                 requiredUnsetAccessFlags);
        }

        // Depending on what's specified, visit a single named method or all methods.
        ClassFileVisitor marker =
            name != null ? (ClassFileVisitor)
                new NamedMethodVisitor(memberInfoVisitor, name, descriptor)

                         : (ClassFileVisitor)
                new AllMethodVisitor(memberInfoVisitor);

        ensureMultiClassFileVisitorForMembers();
        multiClassFileVisitorForMembers.addClassFileVisitor(marker);
    }


    /**
     * Ensures that the multiClassFileVisitorForMembers is filled in,
     * and that it is chained into the main multiClassFileVisitor.
     */
    private void ensureMultiClassFileVisitorForMembers()
    {
        if (multiClassFileVisitorForMembers == null)
        {
            multiClassFileVisitorForMembers =
                new MultiClassFileVisitor();

            // Make sure to mark overriding class members in subclasses.
            multiClassFileVisitor.addClassFileVisitor(
                new ClassFileUpDownTraveler(true, false, false, true,
                                            multiClassFileVisitorForMembers));
        }
    }


    // Implementations for Command

    public void execute(int       phase,
                        ClassPool programClassPool,
                        ClassPool libraryClassPool)
    {
        switch (phase)
        {
            case PHASE_CHECK:
                executeCheckingPhase(programClassPool, libraryClassPool);
                break;
            case PHASE_SHRINK:
                executeShrinkingPhase(programClassPool, libraryClassPool);
                break;
            case PHASE_OBFUSCATE:
                executeObfuscationPhase(programClassPool, libraryClassPool);
                break;
        }
    }


    private void executeCheckingPhase(ClassPool programClassPool,
                                      ClassPool libraryClassPool)
    {
        if (onlyKeepNames)
        {
            return;
        }

        // Print the class(es) and class member(s).
        SimpleClassFilePrinter printer = new SimpleClassFilePrinter();
        variableClassFileVisitor.setClassFileVisitor(printer);
        variableMemberInfoVisitor.setMemberInfoVisitor(printer);

        // Start processing from the program class pool or the library class pool.
        if (extendsClassName == null ||
            programClassPool.getClass(extendsClassName) != null)
        {
            programClassPool.accept(classPoolMarker);
        }
        else
        {
            libraryClassPool.accept(classPoolMarker);
        }
    }


    private void executeShrinkingPhase(ClassPool programClassPool,
                                       ClassPool libraryClassPool)
    {
        if (onlyKeepNames)
        {
            return;
        }

        UsageMarker usageMarker = new UsageMarker();

        // Mark the class(es) and class member(s) as being used.
        variableClassFileVisitor.setClassFileVisitor(usageMarker);
        variableMemberInfoVisitor.setMemberInfoVisitor(usageMarker);

        // Start processing from the program class pool or the library class pool.
        if (extendsClassName == null ||
            programClassPool.getClass(extendsClassName) != null)
        {
            programClassPool.accept(classPoolMarker);
        }
        else
        {
            libraryClassPool.accept(classPoolMarker);
        }
    }


    private void executeObfuscationPhase(ClassPool programClassPool,
                                         ClassPool libraryClassPool)
    {
        NameMarker nameMarker = new NameMarker();

        // Mark the class(es) and class member(s) to keep their names.
        variableClassFileVisitor.setClassFileVisitor(nameMarker);
        variableMemberInfoVisitor.setMemberInfoVisitor(nameMarker);

        // Start processing from the program class pool or the library class pool.
        if (extendsClassName == null ||
            programClassPool.getClass(extendsClassName) != null)
        {
            programClassPool.accept(classPoolMarker);
        }
        else
        {
            libraryClassPool.accept(classPoolMarker);
        }
    }


    /**
     * This visitor resets the counting flag and the class member visit counter
     * upon visiting class files.
     */
    private class MemberVisitCounterResetter
       implements ClassFileVisitor
    {
        private boolean resetCountingMembers;


        public MemberVisitCounterResetter(boolean resetCountingMembers)
        {
            this.resetCountingMembers = resetCountingMembers;
        }


        // Implementations for ClassFileVisitor

        public void visitProgramClassFile(ProgramClassFile programClassFile)
        {
            countingMembers = resetCountingMembers;
            if (countingMembers)
            {
                memberVisitCounter = 0;
            }
        }


        public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
        {
            countingMembers = resetCountingMembers;
            if (countingMembers)
            {
                memberVisitCounter = 0;
            }
        }
    }


    /**
     * This visitor visits class files with its given class file visitor,
     * if the counting flag is false and the class member visit count has reached
     * its set minimum.
     */
    private class ConditionalClassFileVisitor
       implements ClassFileVisitor
    {
        ClassFileVisitor classFileVisitor;


        public ConditionalClassFileVisitor(ClassFileVisitor classFileVisitor)
        {
            this.classFileVisitor = classFileVisitor;
        }

        // Implementations for ClassFileVisitor

        public void visitProgramClassFile(ProgramClassFile programClassFile)
        {
            if (memberVisitCounter >= memberCounter)
            {
                classFileVisitor.visitProgramClassFile(programClassFile);
            }
        }


        public void visitLibraryClassFile(LibraryClassFile libraryClassFile)
        {
            if (memberVisitCounter >= memberCounter)
            {
                classFileVisitor.visitLibraryClassFile(libraryClassFile);
            }
        }
    }


    /**
     * This visitor counts the number of class member visits if the counting flag
     * is true, or it visits class members with its given class member visitor
     * if the counting flag is false.
     */
    private class ConditionalMemberInfoVisitor
       implements MemberInfoVisitor
    {
        MemberInfoVisitor memberInfoVisitor;


        public ConditionalMemberInfoVisitor(MemberInfoVisitor memberInfoVisitor)
        {
            this.memberInfoVisitor = memberInfoVisitor;
        }

        // Implementations for MemberInfoVisitor

        public void visitProgramFieldInfo(ProgramClassFile programClassFile, ProgramFieldInfo programfieldInfo)
        {
            if (countingMembers)
            {
                memberVisitCounter++;
            }
            else
            {
                memberInfoVisitor.visitProgramFieldInfo(programClassFile, programfieldInfo);
            }
        }


        public void visitProgramMethodInfo(ProgramClassFile programClassFile, ProgramMethodInfo programMethodInfo)
        {
            if (countingMembers)
            {
                memberVisitCounter++;
            }
            else
            {
                memberInfoVisitor.visitProgramMethodInfo(programClassFile, programMethodInfo);
            }
        }


        public void visitLibraryFieldInfo(LibraryClassFile libraryClassFile, LibraryFieldInfo libraryFieldInfo)
        {
            if (countingMembers)
            {
                memberVisitCounter++;
            }
            else
            {
                memberInfoVisitor.visitLibraryFieldInfo(libraryClassFile, libraryFieldInfo);
            }
        }


        public void visitLibraryMethodInfo(LibraryClassFile libraryClassFile, LibraryMethodInfo libraryMethodInfo)
        {
            if (countingMembers)
            {
                memberVisitCounter++;
            }
            else
            {
                memberInfoVisitor.visitLibraryMethodInfo(libraryClassFile, libraryMethodInfo);
            }
        }
    }
}
