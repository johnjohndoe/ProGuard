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
package proguard.optimize;

import proguard.*;
import proguard.classfile.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.visitor.AllConstantVisitor;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.visitor.*;
import proguard.classfile.util.MethodLinker;
import proguard.classfile.visitor.*;
import proguard.evaluation.value.SpecificValueFactory;
import proguard.optimize.evaluation.*;
import proguard.optimize.info.*;
import proguard.optimize.peephole.*;

import java.io.IOException;

/**
 * This class optimizes class pools according to a given configuration.
 *
 * @author Eric Lafortune
 */
public class Optimizer
{
    private final Configuration configuration;


    /**
     * Creates a new Optimizer.
     */
    public Optimizer(Configuration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * Performs optimization of the given program class pool.
     */
    public boolean execute(ClassPool programClassPool,
                           ClassPool libraryClassPool) throws IOException
    {
        // Check if we have at least some keep commands.
        if (configuration.keep         == null &&
            configuration.applyMapping == null &&
            configuration.printMapping == null)
        {
            throw new IOException("You have to specify '-keep' options for the optimization step.");
        }

        // Create counters to count the numbers of optimizations.
        ClassCounter       singleImplementationCounter = new ClassCounter();
        ClassCounter       finalClassCounter           = new ClassCounter();
        MemberCounter      finalMethodCounter          = new MemberCounter();
        MemberCounter      privateFieldCounter         = new MemberCounter();
        MemberCounter      privateMethodCounter        = new MemberCounter();
        MemberCounter      staticMethodCounter         = new MemberCounter();
        MemberCounter      writeOnlyFieldCounter       = new MemberCounter();
        MemberCounter      constantFieldCounter        = new MemberCounter();
        MemberCounter      constantMethodCounter       = new MemberCounter();
        MemberCounter      descriptorShrinkCounter     = new MemberCounter();
        MemberCounter      parameterShrinkCounter      = new MemberCounter();
        MemberCounter      variableShrinkCounter       = new MemberCounter();
        ExceptionCounter   exceptionCounter            = new ExceptionCounter();
        InstructionCounter inliningCounter             = new InstructionCounter();
        InstructionCounter commonCodeCounter           = new InstructionCounter();
        InstructionCounter pushCounter                 = new InstructionCounter();
        InstructionCounter branchCounter               = new InstructionCounter();
        InstructionCounter deletedCounter              = new InstructionCounter();
        InstructionCounter addedCounter                = new InstructionCounter();
        InstructionCounter peepholeCounter             = new InstructionCounter();

        // Clean up any old visitor info.
        programClassPool.classesAccept(new ClassCleaner());
        libraryClassPool.classesAccept(new ClassCleaner());

        // Link all methods that should get the same optimization info.
        programClassPool.classesAccept(new BottomClassFilter(
                                       new MethodLinker()));
        libraryClassPool.classesAccept(new BottomClassFilter(
                                       new MethodLinker()));

        // Create a visitor for marking the seeds.
        KeepMarker keepMarker = new KeepMarker();
        ClassPoolVisitor classPoolvisitor =
            ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep,
                                                                    keepMarker,
                                                                    keepMarker,
                                                                    false,
                                                                    true,
                                                                    false);
        // Mark the seeds.
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);

        // All library classes and library class members remain unchanged.
        libraryClassPool.classesAccept(keepMarker);
        libraryClassPool.classesAccept(new AllMemberVisitor(keepMarker));

        // We also keep all classes that are involved in .class constructs.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new AllInstructionVisitor(
                                       new DotClassClassVisitor(keepMarker)))));

        // We also keep all classes that are involved in Class.forName constructs.
        programClassPool.classesAccept(new AllConstantVisitor(
                                       new ClassForNameClassVisitor(keepMarker)));

        // Attach some optimization info to all class members, so it can be
        // filled out later.
        programClassPool.classesAccept(new AllMemberVisitor(
                                       new MemberOptimizationInfoSetter()));

        if (configuration.assumeNoSideEffects != null)
        {
            // Create a visitor for marking methods that don't have any side effects.
            NoSideEffectMethodMarker noSideEffectMethodMarker = new NoSideEffectMethodMarker();
            ClassPoolVisitor noClassPoolvisitor =
                ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.assumeNoSideEffects,
                                                                        null,
                                                                        noSideEffectMethodMarker);

            // Mark the seeds.
            programClassPool.accept(noClassPoolvisitor);
            libraryClassPool.accept(noClassPoolvisitor);
        }

        // Mark all interfaces that have single implementations.
        programClassPool.classesAccept(new SingleImplementationMarker(configuration.allowAccessModification, singleImplementationCounter));

        // Make classes and methods final, as far as possible.
        programClassPool.classesAccept(new ClassFinalizer(finalClassCounter, finalMethodCounter));

        // Mark all fields that are write-only.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new AllInstructionVisitor(
                                       new MultiInstructionVisitor(
                                       new InstructionVisitor[]
                                       {
                                           new ReadWriteFieldMarker(),
                                       })))));

        // Mark all used parameters, including the 'this' parameters.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new OptimizationInfoMemberFilter(
                                       new ParameterUsageMarker())));

        // Shrink the parameters in the method descriptors.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new OptimizationInfoMemberFilter(
                                       new MethodDescriptorShrinker(descriptorShrinkCounter))));

        // Make all non-static methods that don't require the 'this' parameter static.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new OptimizationInfoMemberFilter(
                                       new MemberAccessFilter(0, ClassConstants.INTERNAL_ACC_STATIC,
                                       new MethodStaticizer(staticMethodCounter)))));

        // Remove all unused parameters and variables from the code, for
        // MethodDescriptorShrinker, MethodStaticizer.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new ParameterShrinker(parameterShrinkCounter))));

        // Fix invocations of methods that have become static, for
        // MethodStaticizer.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new MethodInvocationFixer())));

        // Fix all references to class members, for MethodDescriptorShrinker.
        programClassPool.classesAccept(new MemberReferenceFixer());

        // Mark all methods that have side effects.
        programClassPool.accept(new SideEffectMethodMarker());

//        System.out.println("Optimizer.execute: before evaluation simplification");
//        programClassPool.classAccept("abc/Def", new NamedMethodVisitor("abc", null, new ClassPrinter()));

        // Perform partial evaluation for filling out fields, method parameters,
        // and method return values.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new PartialEvaluator(new SpecificValueFactory(), new UnusedParameterInvocationUnit(new StoringInvocationUnit()), false))));

        // Count the write-only fields, and the constant fields and methods.
        programClassPool.classesAccept(new MultiClassVisitor(
                                       new ClassVisitor[]
                                       {
                                           new AllFieldVisitor(
                                           new WriteOnlyFieldFilter(writeOnlyFieldCounter)),
                                           new AllFieldVisitor(
                                           new ConstantMemberFilter(constantFieldCounter)),
                                           new AllMethodVisitor(
                                           new ConstantMemberFilter(constantMethodCounter)),
                                       }));

        // Simplify based on partial evaluation.
        // Also remove unused parameters from the stack before method invocations,
        // for MethodDescriptorShrinker, MethodStaticizer.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new EvaluationSimplifier(
                                       new PartialEvaluator(new SpecificValueFactory(), new UnusedParameterInvocationUnit(new LoadingInvocationUnit()), false),
                                       pushCounter, branchCounter, deletedCounter, addedCounter))));

//        // Specializing the class member descriptors seems to increase the
//        // class file size, on average.
//        // Specialize all class member descriptors.
//        programClassPool.classesAccept(new AllMemberVisitor(
//                                       new OptimizationInfoMemberFilter(
//                                       new MemberDescriptorSpecializer())));
//
//        // Fix all references to classes, for MemberDescriptorSpecializer.
//        programClassPool.classesAccept(new AllMemberVisitor(
//                                       new OptimizationInfoMemberFilter(
//                                       new ClassReferenceFixer(true))));

        // Inline interfaces with single implementations.
        programClassPool.classesAccept(new SingleImplementationInliner());

        // Restore the interface references from these single implementations.
        programClassPool.classesAccept(new SingleImplementationFixer());

        if (configuration.allowAccessModification)
        {
            // Fix the access flags of referenced classes and class members,
            // for SingleImplementationInliner.
            programClassPool.classesAccept(new AllConstantVisitor(
                                           new AccessFixer()));
        }

        // Fix all references to classes, for SingleImplementationInliner.
        programClassPool.classesAccept(new ClassReferenceFixer(true));

        // Fix all references to class members, for SingleImplementationInliner.
        programClassPool.classesAccept(new MemberReferenceFixer());

        // Count all method invocations.
        // Mark super invocations and other access of methods.
        // Mark all exception catches of methods.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new MultiAttributeVisitor(
                                       new AttributeVisitor[]
                                       {
                                           new AllInstructionVisitor(
                                           new MultiInstructionVisitor(
                                           new InstructionVisitor[]
                                           {
                                               new MethodInvocationMarker(),
                                               new SuperInvocationMarker(),
                                               new BackwardBranchMarker(),
                                               new AccessMethodMarker(),
                                           })),
                                           new CatchExceptionMarker(),
                                       }))));

        // Inline methods that are only invoked once.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new MethodInliner(configuration.allowAccessModification, true, inliningCounter))));

        // Inline short methods.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new MethodInliner(configuration.allowAccessModification, false, inliningCounter))));

        // Mark all class members that can not be made private.
        programClassPool.classesAccept(new NonPrivateMemberMarker());

        // Make all non-private and unmarked class members in non-interface
        // classes private.
        programClassPool.classesAccept(new ClassAccessFilter(0, ClassConstants.INTERNAL_ACC_INTERFACE,
                                       new AllMemberVisitor(
                                       new MemberAccessFilter(0, ClassConstants.INTERNAL_ACC_PRIVATE,
                                       new MemberPrivatizer(privateFieldCounter, privateMethodCounter)))));

        if (configuration.allowAccessModification)
        {
            // Fix the access flags of referenced classes and class members,
            // for MethodInliner.
            programClassPool.classesAccept(new AllConstantVisitor(
                                           new AccessFixer()));
        }

        // Fix invocations of methods that have become non-abstract or private,
        // for SingleImplementationInliner, MemberPrivatizer, AccessFixer.
        programClassPool.classesAccept(new AllMemberVisitor(
                                       new AllAttributeVisitor(
                                       new MethodInvocationFixer())));

        // Create a branch target marker and a code attribute editor that can
        // be reused for all code attributes.
        BranchTargetFinder  branchTargetFinder  = new BranchTargetFinder();
        CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();

        // Share common blocks of code at branches.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new GotoCommonCodeReplacer(commonCodeCounter))));

        // Perform various peephole optimisations.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new PeepholeOptimizer(branchTargetFinder, codeAttributeEditor,
                                       new MultiInstructionVisitor(
                                       new InstructionVisitor[]
                                       {
                                           new InstructionSequencesReplacer(InstructionSequenceConstants.PATTERN_CONSTANTS,
                                                                            InstructionSequenceConstants.INSTRUCTION_SEQUENCES,
                                                                            branchTargetFinder, codeAttributeEditor, peepholeCounter),
                                           new GotoGotoReplacer(                                codeAttributeEditor, peepholeCounter),
                                           new GotoReturnReplacer(                              codeAttributeEditor, peepholeCounter),
                                       })))));

        // Remove unnecessary exception handlers.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new UnreachableExceptionRemover(exceptionCounter))));

        // Remove unreachable code.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new UnreachableCodeRemover(deletedCounter))));

        // Remove all unused local variables.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new VariableShrinker(variableShrinkCounter))));

        // Optimize the variables.
        programClassPool.classesAccept(new AllMethodVisitor(
                                       new AllAttributeVisitor(
                                       new VariableOptimizer(!configuration.microEdition))));

        int singleImplementationCount = singleImplementationCounter.getCount();
        int finalClassCount           = finalClassCounter          .getCount();
        int privateFieldCount         = privateFieldCounter        .getCount();
        int privateMethodCount        = privateMethodCounter       .getCount();
        int staticMethodCount         = staticMethodCounter        .getCount();
        int finalMethodCount          = finalMethodCounter         .getCount();
        int writeOnlyFieldCount       = writeOnlyFieldCounter      .getCount();
        int constantFieldCount        = constantFieldCounter       .getCount();
        int constantMethodCount       = constantMethodCounter      .getCount();
        int descriptorShrinkCount     = descriptorShrinkCounter    .getCount();
        int parameterShrinkCount      = parameterShrinkCounter     .getCount();
        int variableShrinkCount       = variableShrinkCounter      .getCount();
        int exceptionCount            = exceptionCounter           .getCount();
        int inliningCount             = inliningCounter            .getCount();
        int commonCodeCount           = commonCodeCounter          .getCount();
        int pushCount                 = pushCounter                .getCount();
        int branchCount               = branchCounter              .getCount();
        int removedCount              = deletedCounter             .getCount() - addedCounter.getCount();
        int peepholeCount             = peepholeCounter            .getCount();

        if (configuration.verbose)
        {
            System.out.println("  Number of inlined interfaces:             "+singleImplementationCount);
            System.out.println("  Number of finalized classes:              "+finalClassCount);
            System.out.println("  Number of privatized fields:              "+privateFieldCount);
            System.out.println("  Number of privatized methods:             "+privateMethodCount);
            System.out.println("  Number of staticized methods:             "+staticMethodCount);
            System.out.println("  Number of finalized methods:              "+finalMethodCount);
            System.out.println("  Number of removed write-only fields:      "+writeOnlyFieldCount);
            System.out.println("  Number of inlined constant fields:        "+constantFieldCount);
            System.out.println("  Number of inlined constant methods:       "+constantMethodCount);
            System.out.println("  Number of simplified method declarations: "+descriptorShrinkCount);
            System.out.println("  Number of removed parameters:             "+parameterShrinkCount);
            System.out.println("  Number of removed local variables:        "+variableShrinkCount);
            System.out.println("  Number of inlined method calls:           "+inliningCount);
            System.out.println("  Number of removed exception blocks:       "+exceptionCount);
            System.out.println("  Number of merged code blocks:             "+commonCodeCount);
            System.out.println("  Number of simplified push instructions:   "+pushCount);
            System.out.println("  Number of simplified branches:            "+branchCount);
            System.out.println("  Number of removed instructions:           "+removedCount);
            System.out.println("  Number of peephole optimizations:         "+peepholeCount);
        }

        return singleImplementationCount > 0 ||
               finalClassCount           > 0 ||
               privateFieldCount         > 0 ||
               privateMethodCount        > 0 ||
               staticMethodCount         > 0 ||
               finalMethodCount          > 0 ||
               writeOnlyFieldCount       > 0 ||
               constantFieldCount        > 0 ||
               constantMethodCount       > 0 ||
               descriptorShrinkCount     > 0 ||
               parameterShrinkCount      > 0 ||
               variableShrinkCount       > 0 ||
               inliningCount             > 0 ||
               exceptionCount            > 0 ||
               commonCodeCount           > 0 ||
               pushCount                 > 0 ||
               branchCount               > 0 ||
               removedCount              > 0 ||
               peepholeCount             > 0;
    }
}
