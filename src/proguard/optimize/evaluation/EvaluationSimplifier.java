/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2008 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.optimize.evaluation;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.evaluation.*;
import proguard.evaluation.value.*;
import proguard.optimize.info.*;

/**
 * This AttributeVisitor simplifies the code attributes that it visits, based
 * on partial evaluation.
 *
 * @author Eric Lafortune
 */
public class EvaluationSimplifier
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    //*
    private static final boolean DEBUG_RESULTS  = false;
    private static final boolean DEBUG_ANALYSIS = false;
    private static final boolean DEBUG          = false;
    /*/
    private static boolean DEBUG_RESULTS  = true;
    private static boolean DEBUG_ANALYSIS = true;
    private static boolean DEBUG          = true;
    //*/

    private final InstructionVisitor extraPushInstructionVisitor;
    private final InstructionVisitor extraBranchInstructionVisitor;
    private final InstructionVisitor extraDeletedInstructionVisitor;
    private final InstructionVisitor extraAddedInstructionVisitor;

    private final PartialEvaluator             partialEvaluator;
    private final PartialEvaluator             simplePartialEvaluator       = new PartialEvaluator();
    private final SideEffectInstructionChecker sideEffectInstructionChecker = new SideEffectInstructionChecker(true);
    private final MyInstructionSimplifier      instructionSimplifier        = new MyInstructionSimplifier();
    private final MyProducerMarker             producerMarker               = new MyProducerMarker();
    private final MyStackConsistencyFixer      stackConsistencyFixer        = new MyStackConsistencyFixer();
    private final CodeAttributeEditor          codeAttributeEditor          = new CodeAttributeEditor(false);

    private boolean[][] variablesNecessaryAfter = new boolean[ClassConstants.TYPICAL_CODE_LENGTH][ClassConstants.TYPICAL_VARIABLES_SIZE];
    private boolean[][] stacksNecessaryAfter    = new boolean[ClassConstants.TYPICAL_CODE_LENGTH][ClassConstants.TYPICAL_STACK_SIZE];
    private boolean[][] stacksSimplifiedBefore  = new boolean[ClassConstants.TYPICAL_CODE_LENGTH][ClassConstants.TYPICAL_STACK_SIZE];
    private boolean[]   instructionsNecessary   = new boolean[ClassConstants.TYPICAL_CODE_LENGTH];
    private boolean[]   instructionsSimplified  = new boolean[ClassConstants.TYPICAL_CODE_LENGTH];
    private int[]       aliasingVariables       = new int[ClassConstants.TYPICAL_CODE_LENGTH];

    private int maxMarkedOffset;


    /**
     * Creates a new EvaluationSimplifier.
     */
    public EvaluationSimplifier()
    {
        this(new PartialEvaluator(), null, null, null, null);
    }


    /**
     * Creates a new EvaluationSimplifier.
     * @param partialEvaluator               the partial evaluator that will
     *                                       execute the code and provide
     *                                       information about the results.
     * @param extraPushInstructionVisitor    an optional extra visitor for all
     *                                       simplified push instructions.
     * @param extraBranchInstructionVisitor  an optional extra visitor for all
     *                                       simplified branch instructions.
     * @param extraDeletedInstructionVisitor an optional extra visitor for all
     *                                       deleted instructions.
     * @param extraAddedInstructionVisitor   an optional extra visitor for all
     *                                       added instructions.
     */
    public EvaluationSimplifier(PartialEvaluator   partialEvaluator,
                                InstructionVisitor extraPushInstructionVisitor,
                                InstructionVisitor extraBranchInstructionVisitor,
                                InstructionVisitor extraDeletedInstructionVisitor,
                                InstructionVisitor extraAddedInstructionVisitor)
    {
        this.partialEvaluator                      = partialEvaluator;
        this.extraPushInstructionVisitor           = extraPushInstructionVisitor;
        this.extraBranchInstructionVisitor         = extraBranchInstructionVisitor;
        this.extraDeletedInstructionVisitor        = extraDeletedInstructionVisitor;
        this.extraAddedInstructionVisitor          = extraAddedInstructionVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
//        DEBUG = DEBUG_ANALYSIS = DEBUG_RESULTS =
//            clazz.getName().equals("abc/Def") &&
//            method.getName(clazz).equals("abc");

        // TODO: Remove this when the evaluation simplifier has stabilized.
        // Catch any unexpected exceptions from the actual visiting method.
        try
        {
            // Process the code.
            visitCodeAttribute0(clazz, method, codeAttribute);
        }
        catch (RuntimeException ex)
        {
            System.err.println("Unexpected error while optimizing after partial evaluation:");
            System.err.println("  Class       = ["+clazz.getName()+"]");
            System.err.println("  Method      = ["+method.getName(clazz)+method.getDescriptor(clazz)+"]");
            System.err.println("  Exception   = ["+ex.getClass().getName()+"] ("+ex.getMessage()+")");
            System.err.println("Not optimizing this method");

            if (DEBUG)
            {
                method.accept(clazz, new ClassPrinter());

                throw ex;
            }
        }
    }


    public void visitCodeAttribute0(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        if (DEBUG_RESULTS)
        {
            System.out.println();
            System.out.println("Class "+ClassUtil.externalClassName(clazz.getName()));
            System.out.println("Method "+ClassUtil.externalFullMethodDescription(clazz.getName(),
                                                                                 0,
                                                                                 method.getName(clazz),
                                                                                 method.getDescriptor(clazz)));
        }

        // Initialize the necessary array.
        initializeNecessary(codeAttribute);

        // Evaluate the method.
        partialEvaluator.visitCodeAttribute(clazz, method, codeAttribute);

        int codeLength = codeAttribute.u4codeLength;

        // Reset the code changes.
        codeAttributeEditor.reset(codeLength);

        // Replace any instructions that can be simplified.
        if (DEBUG_ANALYSIS) System.out.println("Instruction simplification:");

        for (int offset = 0; offset < codeLength; offset++)
        {
            if (partialEvaluator.isTraced(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                instruction.accept(clazz, method, codeAttribute, offset, instructionSimplifier);
            }
        }

        // Mark all essential instructions that have been encountered as used.
        if (DEBUG_ANALYSIS) System.out.println("Usage initialization: ");

        maxMarkedOffset = -1;

        // The invocation of the "super" or "this" <init> method inside a
        // constructor is always necessary.
        int superInitializationOffset = partialEvaluator.superInitializationOffset();
        if (superInitializationOffset != PartialEvaluator.NONE)
        {
            if (DEBUG_ANALYSIS) System.out.print("(super.<init>)");

            markInstruction(superInitializationOffset);
        }

        // Also mark infinite loops and instructions that cause side effects.
        for (int offset = 0; offset < codeLength; offset++)
        {
            if (partialEvaluator.isTraced(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                // Mark that the instruction is necessary if it is an infinite loop.
                if (instruction.opcode == InstructionConstants.OP_GOTO &&
                    ((BranchInstruction)instruction).branchOffset == 0)
                {
                    if (DEBUG_ANALYSIS) System.out.print("(infinite loop)");
                    markInstruction(offset);
                }

                // Mark that the instruction is necessary if it has side effects.
                else if (sideEffectInstructionChecker.hasSideEffects(clazz,
                                                                     method,
                                                                     codeAttribute,
                                                                     offset,
                                                                     instruction))
                {
                    markInstruction(offset);
                }
            }
        }
        if (DEBUG_ANALYSIS) System.out.println();


        // Globally mark instructions and their produced variables and stack
        // entries on which necessary instructions depend.
        // Instead of doing this recursively, we loop across all instructions,
        // starting at the highest previously unmarked instruction that has
        // been been marked.
        if (DEBUG_ANALYSIS) System.out.println("Usage marking:");

        while (maxMarkedOffset >= 0)
        {
            int offset = maxMarkedOffset;

            maxMarkedOffset = offset - 1;

            if (partialEvaluator.isTraced(offset))
            {
                if (isInstructionNecessary(offset))
                {
                    if (isInstructionSimplified(offset))
                    {
                        int variableIndex = getAliasingVariable(offset);

                        if (variableIndex >= 0)
                        {
                            markVariableProducers(offset, variableIndex);
                        }
                    }
                    else
                    {
                        Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                            offset);

                        instruction.accept(clazz, method, codeAttribute, offset, producerMarker);
                    }
                }

                // Check if this instruction is a branch origin from a branch
                // that straddles some marked code.
                markStraddlingBranches(offset,
                                       partialEvaluator.branchTargets(offset),
                                       true);

                // Check if this instruction is a branch target from a branch
                // that straddles some marked code.
                markStraddlingBranches(offset,
                                       partialEvaluator.branchOrigins(offset),
                                       false);
            }

            if (DEBUG_ANALYSIS)
            {
                if (maxMarkedOffset > offset)
                {
                    System.out.println(" -> "+maxMarkedOffset);
                }
            }
        }
        if (DEBUG_ANALYSIS) System.out.println();


        // Mark variable initializations, even if they aren't strictly necessary.
        // The virtual machine's verification step is not smart enough to see
        // this, and may complain otherwise.
        if (DEBUG_ANALYSIS) System.out.println("Initialization marking: ");

        for (int offset = 0; offset < codeLength; offset++)
        {
            // Is it a variable initialization that hasn't been marked yet?
            if (partialEvaluator.isTraced(offset) &&
                !isInstructionNecessary(offset))
            {
                // Is the corresponding variable necessary anywhere in the code,
                // accoriding to a simple partial evaluation?
                int variableIndex = partialEvaluator.initializedVariable(offset);
                if (variableIndex >= 0 &&
                    isVariableInitializationNecessary(clazz,
                                                      method,
                                                      codeAttribute,
                                                      offset,
                                                      variableIndex))
                {
                    markInstruction(offset);
                }
            }
        }
        if (DEBUG_ANALYSIS) System.out.println();


        // Locally fix instructions, in order to keep the stack consistent.
        if (DEBUG_ANALYSIS) System.out.println("Stack consistency fixing:");

        maxMarkedOffset = codeLength - 1;

        while (maxMarkedOffset >= 0)
        {
            int offset = maxMarkedOffset;

            maxMarkedOffset = offset - 1;

            if (partialEvaluator.isTraced(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                instruction.accept(clazz, method, codeAttribute, offset, stackConsistencyFixer);

                // Check if this instruction is a branch origin from a branch
                // that straddles some marked code.
                markStraddlingBranches(offset,
                                       partialEvaluator.branchTargets(offset),
                                       true);

                // Check if this instruction is a branch target from a branch
                // that straddles some marked code.
                markStraddlingBranches(offset,
                                       partialEvaluator.branchOrigins(offset),
                                       false);
            }
        }
        if (DEBUG_ANALYSIS) System.out.println();


        // Replace traced but unmarked backward branches by infinite loops.
        // The virtual machine's verification step is not smart enough to see
        // the code isn't reachable, and may complain otherwise.
        // Any clearly unreachable code will still be removed elsewhere.
        if (DEBUG_ANALYSIS) System.out.println("Infinite loop fixing:");

        for (int offset = 0; offset < codeLength; offset++)
        {
            // Is a traced but unmarked backward branch, without an unmarked
            // straddling forward branch? Note that this is still a heuristic.
            if (partialEvaluator.isTraced(offset) &&
                !isInstructionNecessary(offset)   &&
                isAllSmallerThanOrEqual(partialEvaluator.branchTargets(offset),
                                        offset)   &&
                !isAnyUnnecessaryInstructionBranchingOver(lastNecessaryInstructionOffset(offset),
                                                          offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                replaceByInfiniteLoop(clazz, offset, instruction);
            }
        }
        if (DEBUG_ANALYSIS) System.out.println();


        // Delete all instructions that are not used.
        int offset = 0;
        do
        {
            Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                offset);
            if (!isInstructionNecessary(offset))
            {
                codeAttributeEditor.deleteInstruction(offset);

                codeAttributeEditor.insertBeforeInstruction(offset, null);
                codeAttributeEditor.replaceInstruction(offset, null);
                codeAttributeEditor.insertAfterInstruction(offset, null);

                // Visit the instruction, if required.
                if (extraDeletedInstructionVisitor != null)
                {
                    instruction.accept(clazz, method, codeAttribute, offset, extraDeletedInstructionVisitor);
                }
            }

            offset += instruction.length(offset);
        }
        while (offset < codeLength);


        if (DEBUG_RESULTS)
        {
            System.out.println("Simplification results:");

            offset = 0;
            do
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);
                System.out.println((isInstructionNecessary(offset) ? " + " : " - ")+instruction.toString(offset));

                if (partialEvaluator.isTraced(offset))
                {
                    int initializationOffset = partialEvaluator.initializationOffset(offset);
                    if (initializationOffset != PartialEvaluator.NONE)
                    {
                        System.out.println("     is to be initialized at ["+initializationOffset+"]");
                    }

                    InstructionOffsetValue branchTargets = partialEvaluator.branchTargets(offset);
                    if (branchTargets != null)
                    {
                        System.out.println("     has overall been branching to "+branchTargets);
                    }

                    boolean deleted = codeAttributeEditor.deleted[offset];
                    if (isInstructionNecessary(offset) && deleted)
                    {
                        System.out.println("     is deleted");
                    }

                    Instruction preInsertion = codeAttributeEditor.preInsertions[offset];
                    if (preInsertion != null)
                    {
                        System.out.println("     is preceded by: "+preInsertion);
                    }

                    Instruction replacement = codeAttributeEditor.replacements[offset];
                    if (replacement != null)
                    {
                        System.out.println("     is replaced by: "+replacement);
                    }

                    Instruction postInsertion = codeAttributeEditor.postInsertions[offset];
                    if (postInsertion != null)
                    {
                        System.out.println("     is followed by: "+postInsertion);
                    }
                }

                offset += instruction.length(offset);
            }
            while (offset < codeLength);
        }

        // Apply all accumulated changes to the code.
        codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);
    }


    /**
     * This InstructionVisitor simplifies the instructions that it visits,
     * whenever possible, and marks them as such.
     */
    private class MyInstructionSimplifier
    extends       SimplifiedVisitor
    implements    InstructionVisitor
    {
        // Implementations for InstructionVisitor.

        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            switch (simpleInstruction.opcode)
            {
                case InstructionConstants.OP_IALOAD:
                case InstructionConstants.OP_BALOAD:
                case InstructionConstants.OP_CALOAD:
                case InstructionConstants.OP_SALOAD:
                case InstructionConstants.OP_IADD:
                case InstructionConstants.OP_ISUB:
                case InstructionConstants.OP_IMUL:
                case InstructionConstants.OP_IDIV:
                case InstructionConstants.OP_IREM:
                case InstructionConstants.OP_INEG:
                case InstructionConstants.OP_ISHL:
                case InstructionConstants.OP_ISHR:
                case InstructionConstants.OP_IUSHR:
                case InstructionConstants.OP_IAND:
                case InstructionConstants.OP_IOR:
                case InstructionConstants.OP_IXOR:
                case InstructionConstants.OP_L2I:
                case InstructionConstants.OP_F2I:
                case InstructionConstants.OP_D2I:
                case InstructionConstants.OP_I2B:
                case InstructionConstants.OP_I2C:
                case InstructionConstants.OP_I2S:
                    replaceIntegerPushInstruction(clazz, offset, simpleInstruction);
                    break;

                case InstructionConstants.OP_LALOAD:
                case InstructionConstants.OP_LADD:
                case InstructionConstants.OP_LSUB:
                case InstructionConstants.OP_LMUL:
                case InstructionConstants.OP_LDIV:
                case InstructionConstants.OP_LREM:
                case InstructionConstants.OP_LNEG:
                case InstructionConstants.OP_LSHL:
                case InstructionConstants.OP_LSHR:
                case InstructionConstants.OP_LUSHR:
                case InstructionConstants.OP_LAND:
                case InstructionConstants.OP_LOR:
                case InstructionConstants.OP_LXOR:
                case InstructionConstants.OP_I2L:
                case InstructionConstants.OP_F2L:
                case InstructionConstants.OP_D2L:
                    replaceLongPushInstruction(clazz, offset, simpleInstruction);
                    break;

                case InstructionConstants.OP_FALOAD:
                case InstructionConstants.OP_FADD:
                case InstructionConstants.OP_FSUB:
                case InstructionConstants.OP_FMUL:
                case InstructionConstants.OP_FDIV:
                case InstructionConstants.OP_FREM:
                case InstructionConstants.OP_FNEG:
                case InstructionConstants.OP_I2F:
                case InstructionConstants.OP_L2F:
                case InstructionConstants.OP_D2F:
                    replaceFloatPushInstruction(clazz, offset, simpleInstruction);
                    break;

                case InstructionConstants.OP_DALOAD:
                case InstructionConstants.OP_DADD:
                case InstructionConstants.OP_DSUB:
                case InstructionConstants.OP_DMUL:
                case InstructionConstants.OP_DDIV:
                case InstructionConstants.OP_DREM:
                case InstructionConstants.OP_DNEG:
                case InstructionConstants.OP_I2D:
                case InstructionConstants.OP_L2D:
                case InstructionConstants.OP_F2D:
                    replaceDoublePushInstruction(clazz, offset, simpleInstruction);
                    break;

                case InstructionConstants.OP_AALOAD:
                    replaceReferencePushInstruction(clazz, offset, simpleInstruction);
                    break;
            }
        }


        public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
        {
            int variableIndex = variableInstruction.variableIndex;

            switch (variableInstruction.opcode)
            {
                case InstructionConstants.OP_ILOAD:
                case InstructionConstants.OP_ILOAD_0:
                case InstructionConstants.OP_ILOAD_1:
                case InstructionConstants.OP_ILOAD_2:
                case InstructionConstants.OP_ILOAD_3:
                    replaceIntegerPushInstruction(clazz, offset, variableInstruction, variableIndex);
                    break;

                case InstructionConstants.OP_LLOAD:
                case InstructionConstants.OP_LLOAD_0:
                case InstructionConstants.OP_LLOAD_1:
                case InstructionConstants.OP_LLOAD_2:
                case InstructionConstants.OP_LLOAD_3:
                    replaceLongPushInstruction(clazz, offset, variableInstruction, variableIndex);
                    break;

                case InstructionConstants.OP_FLOAD:
                case InstructionConstants.OP_FLOAD_0:
                case InstructionConstants.OP_FLOAD_1:
                case InstructionConstants.OP_FLOAD_2:
                case InstructionConstants.OP_FLOAD_3:
                    replaceFloatPushInstruction(clazz, offset, variableInstruction, variableIndex);
                    break;

                case InstructionConstants.OP_DLOAD:
                case InstructionConstants.OP_DLOAD_0:
                case InstructionConstants.OP_DLOAD_1:
                case InstructionConstants.OP_DLOAD_2:
                case InstructionConstants.OP_DLOAD_3:
                    replaceDoublePushInstruction(clazz, offset, variableInstruction, variableIndex);
                    break;

                case InstructionConstants.OP_ALOAD:
                case InstructionConstants.OP_ALOAD_0:
                case InstructionConstants.OP_ALOAD_1:
                case InstructionConstants.OP_ALOAD_2:
                case InstructionConstants.OP_ALOAD_3:
                    replaceReferencePushInstruction(clazz, offset, variableInstruction);
                    break;

            }
        }


        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            switch (constantInstruction.opcode)
            {
                case InstructionConstants.OP_GETSTATIC:
                case InstructionConstants.OP_GETFIELD:
                    replaceAnyPushInstruction(clazz, offset, constantInstruction);
                    break;

                case InstructionConstants.OP_INVOKEVIRTUAL:
                case InstructionConstants.OP_INVOKESPECIAL:
                case InstructionConstants.OP_INVOKESTATIC:
                case InstructionConstants.OP_INVOKEINTERFACE:
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex,
                                                  new ReferencedMemberVisitor(
                                                  new MyUnusedParameterSimplifier(offset,
                                                                                  constantInstruction)));

                    if (constantInstruction.stackPushCount(clazz) > 0 &&
                        !sideEffectInstructionChecker.hasSideEffects(clazz,
                                                                     method,
                                                                     codeAttribute,
                                                                     offset,
                                                                     constantInstruction))
                    {
                        replaceAnyPushInstruction(clazz, offset, constantInstruction);
                    }

                    break;

                case InstructionConstants.OP_CHECKCAST:
                    replaceReferencePushInstruction(clazz, offset, constantInstruction);
                    break;
            }
        }


        public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
        {
            switch (branchInstruction.opcode)
            {
                case InstructionConstants.OP_GOTO:
                case InstructionConstants.OP_GOTO_W:
                    // Don't replace unconditional branches.
                    break;

                case InstructionConstants.OP_JSR:
                case InstructionConstants.OP_JSR_W:
                    replaceJsrInstruction(clazz, offset, branchInstruction);
                    break;

                default:
                    replaceBranchInstruction(clazz, offset, branchInstruction);
                    break;
            }
        }


        public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
        {
            // First try to simplify it to a simple branch.
            replaceBranchInstruction(clazz, offset, switchInstruction);

            // Otherwise make sure all branch targets are valid.
            if (!isInstructionSimplified(offset))
            {
                replaceSwitchInstruction(clazz, offset, switchInstruction);
            }
        }
    }


    /**
     * This MemberVisitor marks stack entries that aren't necessary because
     * parameters aren't used in the methods that are visited.
     */
    private class MyUnusedParameterSimplifier
    extends       SimplifiedVisitor
    implements    MemberVisitor
    {
        private int                 invocationOffset;
        private ConstantInstruction invocationInstruction;


        /**
         * Creates a new MyUnusedParameterSimplifier for the given invocation
         * at the given offset.
         */
        private MyUnusedParameterSimplifier(int                 invocationOffset,
                                            ConstantInstruction invocationInstruction)
        {
            this.invocationOffset      = invocationOffset;
            this.invocationInstruction = invocationInstruction;
        }


        // Implementations for MemberVisitor.

        public void visitAnyMember(Clazz clazz, Member member) {}


        public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
        {
            // Get the total size of the parameters.
            int parameterSize = ParameterUsageMarker.getParameterSize(programMethod);

            // Make the method invocation static, if possible.
            if ((programMethod.getAccessFlags() & ClassConstants.INTERNAL_ACC_STATIC) == 0 &&
                !ParameterUsageMarker.isParameterUsed(programMethod, 0))
            {
                replaceInvocationInstruction(programClass,
                                             invocationOffset,
                                             invocationInstruction);
            }

            // Remove unused parameters.
            for (int index = 0; index < parameterSize; index++)
            {
                if (!ParameterUsageMarker.isParameterUsed(programMethod, index))
                {
                    TracedStack stack =
                        partialEvaluator.getStackBefore(invocationOffset);

                    int stackIndex = stack.size() - parameterSize + index;

                    if (DEBUG)
                    {
                        System.out.println("  ["+invocationOffset+"] Ignoring parameter #"+index+" of "+programClass.getName()+"."+programMethod.getName(programClass)+programMethod.getDescriptor(programClass)+"] (stack entry #"+stackIndex+" ["+stack.getBottom(stackIndex)+"])");
                        System.out.println("    Full stack: "+stack);
                    }

                    markStackSimplificationBefore(invocationOffset, stackIndex);
                }
            }
        }
    }


    /**
     * This InstructionVisitor marks the producing instructions and produced
     * variables and stack entries of the instructions that it visits.
     * Simplified method arguments are ignored.
     */
    private class MyProducerMarker
    extends       SimplifiedVisitor
    implements    InstructionVisitor
    {
        // Implementations for InstructionVisitor.

        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
        {
            markStackProducers(clazz, offset, instruction);
        }


        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            switch (simpleInstruction.opcode)
            {
                case InstructionConstants.OP_DUP:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 0);
                    break;
                case InstructionConstants.OP_DUP_X1:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 1);
                    conditionallyMarkStackEntryProducers(offset, 2, 0);
                    break;
                case InstructionConstants.OP_DUP_X2:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 1);
                    conditionallyMarkStackEntryProducers(offset, 2, 2);
                    conditionallyMarkStackEntryProducers(offset, 3, 0);
                    break;
                case InstructionConstants.OP_DUP2:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 1);
                    conditionallyMarkStackEntryProducers(offset, 2, 0);
                    conditionallyMarkStackEntryProducers(offset, 3, 1);
                    break;
                case InstructionConstants.OP_DUP2_X1:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 1);
                    conditionallyMarkStackEntryProducers(offset, 2, 2);
                    conditionallyMarkStackEntryProducers(offset, 3, 0);
                    conditionallyMarkStackEntryProducers(offset, 4, 1);
                    break;
                case InstructionConstants.OP_DUP2_X2:
                    conditionallyMarkStackEntryProducers(offset, 0, 0);
                    conditionallyMarkStackEntryProducers(offset, 1, 1);
                    conditionallyMarkStackEntryProducers(offset, 2, 2);
                    conditionallyMarkStackEntryProducers(offset, 3, 3);
                    conditionallyMarkStackEntryProducers(offset, 4, 0);
                    conditionallyMarkStackEntryProducers(offset, 5, 1);
                    break;
                case InstructionConstants.OP_SWAP:
                    conditionallyMarkStackEntryProducers(offset, 0, 1);
                    conditionallyMarkStackEntryProducers(offset, 1, 0);
                    break;
                default:
                    markStackProducers(clazz, offset, simpleInstruction);
                    break;
            }
        }


        public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
        {
            // Is the variable being loaded (or incremented)?
            if (variableInstruction.opcode < InstructionConstants.OP_ISTORE)
            {
                markVariableProducers(offset, variableInstruction.variableIndex);
            }
            else
            {
                markStackProducers(clazz, offset, variableInstruction);
            }
        }


        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            // Mark the initializer invocation, if this is a 'new' instruction.
            if (constantInstruction.opcode == InstructionConstants.OP_NEW)
            {
                markInitialization(offset);
            }

            markStackProducers(clazz, offset, constantInstruction);
        }


        public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
        {
            // Explicitly mark the produced stack entry of a 'jsr' instruction,
            // because the consuming 'astore' instruction of the subroutine is
            // cleared every time it is traced.
            if (branchInstruction.opcode == InstructionConstants.OP_JSR ||
                branchInstruction.opcode == InstructionConstants.OP_JSR_W)
            {
                markStackEntryAfter(offset, 0);
            }
            else
            {
                markStackProducers(clazz, offset, branchInstruction);
            }
        }
    }


    /**
     * This InstructionVisitor fixes instructions locally, popping any unused
     * produced stack entries after marked instructions, and popping produced
     * stack entries and pushing missing stack entries instead of unmarked
     * instructions.
     */
    private class MyStackConsistencyFixer
    extends       SimplifiedVisitor
    implements    InstructionVisitor
    {
        // Implementations for InstructionVisitor.

        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
        {
            // Has the instruction been marked?
            if (isInstructionNecessary(offset))
            {
                // Check all stack entries that are popped.
                // Typical case: a freshly marked variable initialization that
                // requires some value on the stack.
                int popCount = instruction.stackPopCount(clazz);
                if (popCount > 0)
                {
                    TracedStack tracedStack =
                        partialEvaluator.getStackBefore(offset);

                    int top = tracedStack.size() - 1;

                    int requiredPushCount = 0;
                    for (int stackIndex = 0; stackIndex < popCount; stackIndex++)
                    {
                        // Is the stack entry required by other consumers?
                        if (!isStackSimplifiedBefore(offset, top - stackIndex) &&
                            !isAnyStackEntryNecessaryAfter(tracedStack.getTopProducerValue(stackIndex).instructionOffsetValue(), top - stackIndex))
                        {
                            // Remember to push it.
                            requiredPushCount++;
                        }
                    }

                    // Push some necessary stack entries.
                    if (requiredPushCount > 0)
                    {
                        if (DEBUG_ANALYSIS) System.out.print("  Inserting before marked consumer "+instruction.toString(offset));

                        if (requiredPushCount > (instruction.isCategory2() ? 2 : 1))
                        {
                            throw new IllegalArgumentException("Unsupported stack size increment ["+requiredPushCount+"]");
                        }

                        increaseStackSize(offset, tracedStack.getTop(0).computationalType(), false);
                    }
                }

                // Check all stack entries that are pushed.
                // Typical case: a return value that wasn't really required and
                // that should be popped.
                int pushCount = instruction.stackPushCount(clazz);
                if (pushCount > 0)
                {
                    TracedStack tracedStack =
                        partialEvaluator.getStackAfter(offset);

                    int top = tracedStack.size() - 1;

                    int requiredPopCount = 0;
                    for (int stackIndex = 0; stackIndex < pushCount; stackIndex++)
                    {
                        // Is the stack entry required by consumers?
                        if (!isStackEntryNecessaryAfter(offset, top - stackIndex))
                        {
                            // Remember to pop it.
                            requiredPopCount++;
                        }
                    }

                    // Pop the unnecessary stack entries.
                    if (requiredPopCount > 0)
                    {
                        if (DEBUG_ANALYSIS) System.out.print("  Inserting after marked producer "+instruction.toString(offset));

                        decreaseStackSize(offset, requiredPopCount, false, false);
                    }
                }
            }
            else
            {
                // Check all stack entries that would be popped.
                // Typical case: a stack value that is required elsewhere and
                // that still has to be popped.
                int popCount = instruction.stackPopCount(clazz);
                if (popCount > 0)
                {
                    TracedStack tracedStack =
                        partialEvaluator.getStackBefore(offset);

                    int top = tracedStack.size() - 1;

                    int expectedPopCount = 0;
                    for (int stackIndex = 0; stackIndex < popCount; stackIndex++)
                    {
                        // Is the stack entry required by other consumers?
                        if (isAnyStackEntryNecessaryAfter(tracedStack.getTopProducerValue(stackIndex).instructionOffsetValue(), top - stackIndex))
                        {
                            // Remember to pop it.
                            expectedPopCount++;
                        }
                    }

                    // Pop the unnecessary stack entries.
                    if (expectedPopCount > 0)
                    {
                        if (DEBUG_ANALYSIS) System.out.print("  Replacing unmarked consumer "+instruction.toString(offset));

                        decreaseStackSize(offset, expectedPopCount, true, true);
                    }
                }

                // Check all stack entries that would be pushed.
                // Typical case: never.
                int pushCount = instruction.stackPushCount(clazz);
                if (pushCount > 0)
                {
                    TracedStack tracedStack =
                        partialEvaluator.getStackAfter(offset);

                    int top = tracedStack.size() - 1;

                    int expectedPushCount = 0;
                    for (int stackIndex = 0; stackIndex < pushCount; stackIndex++)
                    {
                        // Is the stack entry required by consumers?
                        if (isStackEntryNecessaryAfter(offset, top - stackIndex))
                        {
                            // Remember to push it.
                            expectedPushCount++;
                        }
                    }

                    // Push some necessary stack entries.
                    if (expectedPushCount > 0)
                    {
                        if (DEBUG_ANALYSIS) System.out.print("  Replacing unmarked producer "+instruction.toString(offset));

                        increaseStackSize(offset, tracedStack.getTop(0).computationalType(), true);
                    }
                }
            }
        }


        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            if (isInstructionNecessary(offset) &&
                isDupOrSwap(simpleInstruction))
            {
                fixDupInstruction(clazz, codeAttribute, offset, simpleInstruction);
            }
            else
            {
                visitAnyInstruction(clazz, method, codeAttribute, offset, simpleInstruction);
            }
        }
    }


    // Small utility methods.

    /**
     * Marks the variable and the corresponding producing instructions
     * of the consumer at the given offset.
     * @param consumerOffset the offset of the consumer.
     * @param variableIndex  the index of the variable to be marked.
     */
    private void markVariableProducers(int consumerOffset,
                                       int variableIndex)
    {
        TracedVariables tracedVariables =
            partialEvaluator.getVariablesBefore(consumerOffset);

        // Mark the producer of the loaded value.
        markVariableProducers(tracedVariables.getProducerValue(variableIndex).instructionOffsetValue(),
                              variableIndex);
    }


    /**
     * Marks the variable and its producing instructions at the given offsets.
     * @param producerOffsets the offsets of the producers to be marked.
     * @param variableIndex   the index of the variable to be marked.
     */
    private void markVariableProducers(InstructionOffsetValue producerOffsets,
                                       int                    variableIndex)
    {
        if (producerOffsets != null)
        {
            int offsetCount = producerOffsets.instructionOffsetCount();
            for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++)
            {
                // Make sure the variable and the instruction are marked
                // at the producing offset.
                int offset = producerOffsets.instructionOffset(offsetIndex);

                markVariableAfter(offset, variableIndex);
                markInstruction(offset);
            }
        }
    }


    /**
     * Marks the stack entries and their producing instructions of the
     * consumer at the given offset.
     * @param clazz          the containing class.
     * @param consumerOffset the offset of the consumer.
     * @param consumer       the consumer of the stack entries.
     */
    private void markStackProducers(Clazz       clazz,
                                    int         consumerOffset,
                                    Instruction consumer)
    {
        // Mark the producers of the popped values.
        int popCount = consumer.stackPopCount(clazz);
        for (int stackIndex = 0; stackIndex < popCount; stackIndex++)
        {
            markStackEntryProducers(consumerOffset, stackIndex);
        }
    }


    /**
     * Marks the stack entry and the corresponding producing instructions
     * of the consumer at the given offset, if the stack entry of the
     * consumer is marked.
     * @param consumerOffset     the offset of the consumer.
     * @param consumerStackIndex the index of the stack entry to be checked
     *                           (counting from the top).
     * @param producerStackIndex the index of the stack entry to be marked
     *                           (counting from the top).
     */
    private void conditionallyMarkStackEntryProducers(int consumerOffset,
                                                      int consumerStackIndex,
                                                      int producerStackIndex)
    {
        int top = partialEvaluator.getStackAfter(consumerOffset).size() - 1;

        if (isStackEntryNecessaryAfter(consumerOffset, top - consumerStackIndex))
        {
            markStackEntryProducers(consumerOffset, producerStackIndex);
        }
    }


    /**
     * Marks the stack entry and the corresponding producing instructions
     * of the consumer at the given offset.
     * @param consumerOffset the offset of the consumer.
     * @param stackIndex     the index of the stack entry to be marked
     *                        (counting from the top).
     */
    private void markStackEntryProducers(int consumerOffset,
                                         int stackIndex)
    {
        TracedStack tracedStack =
            partialEvaluator.getStackBefore(consumerOffset);

        int stackBottomIndex = tracedStack.size() - 1 - stackIndex;

        if (!isStackSimplifiedBefore(consumerOffset, stackBottomIndex))
        {
            markStackEntryProducers(tracedStack.getTopProducerValue(stackIndex).instructionOffsetValue(),
                                    stackBottomIndex);
        }
    }


    /**
     * Marks the stack entry and its producing instructions at the given
     * offsets.
     * @param producerOffsets the offsets of the producers to be marked.
     * @param stackIndex      the index of the stack entry to be marked
     *                        (counting from the bottom).
     */
    private void markStackEntryProducers(InstructionOffsetValue producerOffsets,
                                         int                    stackIndex)
    {
        if (producerOffsets != null)
        {
            int offsetCount = producerOffsets.instructionOffsetCount();
            for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++)
            {
                // Make sure the stack entry and the instruction are marked
                // at the producing offset.
                int offset = producerOffsets.instructionOffset(offsetIndex);

                markStackEntryAfter(offset, stackIndex);
                markInstruction(offset);
            }
        }
    }


    /**
     * Marks the stack entry and its initializing instruction
     * ('invokespecial *.<init>') for the given 'new' instruction offset.
     * @param newInstructionOffset the offset of the 'new' instruction.
     */
    private void markInitialization(int newInstructionOffset)
    {
        int initializationOffset =
            partialEvaluator.initializationOffset(newInstructionOffset);

        TracedStack tracedStack =
            partialEvaluator.getStackAfter(newInstructionOffset);

        markStackEntryAfter(initializationOffset, tracedStack.size() - 1);
        markInstruction(initializationOffset);
    }


    /**
     * Marks the branch instructions of straddling branches, if they straddle
     * some code that has been marked.
     * @param instructionOffset   the offset of the branch origin or branch target.
     * @param branchOffsets       the offsets of the straddling branch targets
     *                            or branch origins.
     * @param isPointingToTargets <code>true</code> if the above offsets are
     *                            branch targets, <code>false</code> if they
     *                            are branch origins.
     */
    private void markStraddlingBranches(int                    instructionOffset,
                                        InstructionOffsetValue branchOffsets,
                                        boolean                isPointingToTargets)
    {
        if (branchOffsets != null)
        {
            // Loop over all branch offsets.
            int branchCount = branchOffsets.instructionOffsetCount();
            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
            {
                // Is the branch straddling forward any necessary instructions?
                int branchOffset = branchOffsets.instructionOffset(branchIndex);

                // Is the offset pointing to a branch origin or to a branch target?
                if (isPointingToTargets)
                {
                    markStraddlingBranch(instructionOffset,
                                         branchOffset,
                                         instructionOffset,
                                         branchOffset);
                }
                else
                {
                    markStraddlingBranch(instructionOffset,
                                         branchOffset,
                                         branchOffset,
                                         instructionOffset);
                }
            }
        }
    }


    private void markStraddlingBranch(int instructionOffsetStart,
                                      int instructionOffsetEnd,
                                      int branchOrigin,
                                      int branchTarget)
    {
        if (!isInstructionNecessary(branchOrigin) &&
            isAnyInstructionNecessary(instructionOffsetStart, instructionOffsetEnd))
        {
            if (DEBUG_ANALYSIS) System.out.print("["+branchOrigin+"->"+branchTarget+"]");

            // Mark the branch instruction.
            markInstruction(branchOrigin);
        }
    }


    /**
     * Marks the specified instruction if it is a required dup/swap instruction,
     * replacing it by an appropriate variant if necessary.
     * @param clazz         the class that is being checked.
     * @param codeAttribute the code that is being checked.
     * @param dupOffset     the offset of the dup/swap instruction.
     * @param instruction   the dup/swap instruction.
     */
    private void fixDupInstruction(Clazz         clazz,
                                   CodeAttribute codeAttribute,
                                   int           dupOffset,
                                   Instruction   instruction)
    {
        int top = partialEvaluator.getStackAfter(dupOffset).size() - 1;

        byte oldOpcode = instruction.opcode;
        byte newOpcode = 0;

        // Simplify the popping instruction if possible.
        switch (oldOpcode)
        {
            case InstructionConstants.OP_DUP:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, top - 0);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, top - 1);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent1)
                {
                    // Should both the original element and the copy be present?
                    if (stackEntryPresent0 &&
                        stackEntryPresent1)
                    {
                        newOpcode = InstructionConstants.OP_DUP;
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP_X1:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, top - 0);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, top - 1);
                boolean stackEntryPresent2 = isStackEntryNecessaryAfter(dupOffset, top - 2);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent2)
                {
                    // Should the copy be present?
                    if (stackEntryPresent2)
                    {
                        // Compute the number of elements to be skipped.
                        int skipCount = stackEntryPresent1 ? 1 : 0;

                        // Should the original element be present?
                        if (stackEntryPresent0)
                        {
                            // Copy the original element.
                            newOpcode = (byte)(InstructionConstants.OP_DUP + skipCount);
                        }
                        else if (skipCount == 1)
                        {
                            // Move the original element.
                            newOpcode = InstructionConstants.OP_SWAP;
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP_X2:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, top - 0);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, top - 1);
                boolean stackEntryPresent2 = isStackEntryNecessaryAfter(dupOffset, top - 2);
                boolean stackEntryPresent3 = isStackEntryNecessaryAfter(dupOffset, top - 3);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent3)
                {
                    // Should the copy be present?
                    if (stackEntryPresent3)
                    {
                        int skipCount = (stackEntryPresent1 ? 1 : 0) +
                                        (stackEntryPresent2 ? 1 : 0);

                        // Should the original element be present?
                        if (stackEntryPresent0)
                        {
                            // Copy the original element.
                            newOpcode = (byte)(InstructionConstants.OP_DUP + skipCount);
                        }
                        else if (skipCount == 1)
                        {
                            // Move the original element.
                            newOpcode = InstructionConstants.OP_SWAP;
                        }
                        else if (skipCount == 2)
                        {
                            // We can't easily move the original element.
                            throw new UnsupportedOperationException("Can't handle dup_x2 instruction moving original element across two elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP2:
            {
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, top - 0, top - 1);
                boolean stackEntriesPresent23 = isStackEntriesNecessaryAfter(dupOffset, top - 2, top - 3);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent23)
                {
                    // Should both the original element and the copy be present?
                    if (stackEntriesPresent01 &&
                        stackEntriesPresent23)
                    {
                        newOpcode = InstructionConstants.OP_DUP2;
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP2_X1:
            {
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, top - 0, top - 1);
                boolean stackEntryPresent2    = isStackEntryNecessaryAfter(dupOffset, top - 2);
                boolean stackEntriesPresent34 = isStackEntriesNecessaryAfter(dupOffset, top - 3, top - 4);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent34)
                {
                    // Should the copy be present?
                    if (stackEntriesPresent34)
                    {
                        int skipCount = stackEntryPresent2 ? 1 : 0;

                        // Should the original element be present?
                        if (stackEntriesPresent01)
                        {
                            // Copy the original element.
                            newOpcode = (byte)(InstructionConstants.OP_DUP2 + skipCount);
                        }
                        else if (skipCount > 0)
                        {
                            // We can't easily move the original element.
                            throw new UnsupportedOperationException("Can't handle dup2_x1 instruction moving original element across "+skipCount+" elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP2_X2:
            {
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, top - 0, top - 1);
                boolean stackEntryPresent2    = isStackEntryNecessaryAfter(dupOffset, top - 2);
                boolean stackEntryPresent3    = isStackEntryNecessaryAfter(dupOffset, top - 3);
                boolean stackEntriesPresent45 = isStackEntriesNecessaryAfter(dupOffset, top - 4, top - 5);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent45)
                {
                    // Should the copy be present?
                    if (stackEntriesPresent45)
                    {
                        int skipCount = (stackEntryPresent2 ? 1 : 0) +
                                        (stackEntryPresent3 ? 1 : 0);

                        // Should the original element be present?
                        if (stackEntriesPresent01)
                        {
                            // Copy the original element.
                            newOpcode = (byte)(InstructionConstants.OP_DUP2 + skipCount);
                        }
                        else if (skipCount > 0)
                        {
                            // We can't easily move the original element.
                            throw new UnsupportedOperationException("Can't handle dup2_x2 instruction moving original element across "+skipCount+" elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_SWAP:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, top - 0);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, top - 1);

                // Will either element be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent1)
                {
                    // Will both elements be present?
                    if (stackEntryPresent0 &&
                        stackEntryPresent1)
                    {
                        newOpcode = InstructionConstants.OP_SWAP;
                    }
                }
                break;
            }
        }

        if      (newOpcode == 0)
        {
            // Delete the instruction.
            codeAttributeEditor.deleteInstruction(dupOffset);

            if (extraDeletedInstructionVisitor != null)
            {
                extraDeletedInstructionVisitor.visitSimpleInstruction(null, null, null, dupOffset, null);
            }

            if (DEBUG_ANALYSIS) System.out.println("  Marking but deleting instruction "+instruction.toString(dupOffset));
        }
        else if (newOpcode == oldOpcode)
        {
            // Leave the instruction unchanged.
            codeAttributeEditor.undeleteInstruction(dupOffset);

            if (DEBUG_ANALYSIS) System.out.println("  Marking unchanged instruction "+instruction.toString(dupOffset));
        }
        else
        {
            // Replace the instruction.
            Instruction replacementInstruction = new SimpleInstruction(newOpcode);
            codeAttributeEditor.replaceInstruction(dupOffset,
                                                   replacementInstruction);

            if (DEBUG_ANALYSIS) System.out.println("  Replacing instruction "+instruction.toString(dupOffset)+" by "+replacementInstruction.toString());
        }
    }


    /**
     * Puts the required push instruction before the given index. The
     * instruction is marked as necessary.
     * @param offset            the offset of the instruction.
     * @param computationalType the computational type on the stack, for
     *                          push instructions.
     * @param delete            specifies whether the instruction should be
     *                          deleted.
     */
    private void increaseStackSize(int     offset,
                                   int     computationalType,
                                   boolean delete)
    {
        // Mark this instruction.
        markInstruction(offset);

        // Create a simple push instrucion.
        Instruction replacementInstruction =
            new SimpleInstruction(pushOpcode(computationalType));

        if (DEBUG_ANALYSIS) System.out.println(": "+replacementInstruction.toString(offset));

        // Delete the original instruction if necessary.
        if (delete)
        {
            if (DEBUG_ANALYSIS) System.out.println("  Deleting original instruction at "+offset);

            // Replace the push instruction.
            codeAttributeEditor.replaceInstruction(offset,
                                                   replacementInstruction);
        }
        else
        {
            // Insert the push instruction.
            codeAttributeEditor.insertBeforeInstruction(offset,
                                                        replacementInstruction);

            if (extraAddedInstructionVisitor != null)
            {
                replacementInstruction.accept(null, null, null, offset, extraAddedInstructionVisitor);
            }
        }
    }


    /**
     * Returns the opcode of a push instruction corresponding to the given
     * computational type.
     * @param computationalType the computational type to be pushed on the stack.
     */
    private byte pushOpcode(int computationalType)
    {
        switch (computationalType)
        {
            case Value.TYPE_INTEGER:            return InstructionConstants.OP_ICONST_0;
            case Value.TYPE_LONG:               return InstructionConstants.OP_LCONST_0;
            case Value.TYPE_FLOAT:              return InstructionConstants.OP_FCONST_0;
            case Value.TYPE_DOUBLE:             return InstructionConstants.OP_DCONST_0;
            case Value.TYPE_REFERENCE:
            case Value.TYPE_INSTRUCTION_OFFSET: return InstructionConstants.OP_ACONST_NULL;
        }

        throw new IllegalArgumentException("No push opcode for computational type ["+computationalType+"]");
    }


    /**
     * Puts the required pop instruction at the given index. The
     * instruction is marked as necessary.
     * @param offset   the offset of the instruction.
     * @param popCount the required reduction of the stack size.
     * @param before   specifies whether the pop instruction should be inserted
     *                 before or after the present instruction.
     * @param delete   specifies whether the instruction should be deleted.
     */
    private void decreaseStackSize(int     offset,
                                   int     popCount,
                                   boolean before,
                                   boolean delete)
    {
        // Mark this instruction.
        markInstruction(offset);

        boolean after = !before;

        int remainingPopCount = popCount;

        if (delete)
        {
            // Replace the original instruction.
            int count = remainingPopCount == 1 ? 1 : 2;

            // Create a simple pop instrucion.
            byte replacementOpcode = count == 1 ?
                InstructionConstants.OP_POP :
                InstructionConstants.OP_POP2;

            Instruction replacementInstruction = new SimpleInstruction(replacementOpcode);

            if (DEBUG_ANALYSIS) System.out.println(": "+replacementInstruction.toString(offset));

            // Insert the pop instruction.
            codeAttributeEditor.replaceInstruction(offset,
                                                   replacementInstruction);

            remainingPopCount -= count;

            // We may insert other pop instructions before and after this one.
            before = true;
            after  = true;
        }

        if (before && remainingPopCount > 0)
        {
            // Insert before the original instruction.
            int count = remainingPopCount == 1 ? 1 : 2;

            // Create a simple pop instrucion.
            byte replacementOpcode = count == 1 ?
                InstructionConstants.OP_POP :
                InstructionConstants.OP_POP2;

            Instruction replacementInstruction = new SimpleInstruction(replacementOpcode);

            if (DEBUG_ANALYSIS) System.out.println(": "+replacementInstruction.toString(offset));

            // Insert the pop instruction.
            codeAttributeEditor.insertBeforeInstruction(offset,
                                                        replacementInstruction);

            remainingPopCount -= count;

            if (extraAddedInstructionVisitor != null)
            {
                replacementInstruction.accept(null, null, null, offset, extraAddedInstructionVisitor);
            }
        }

        if (after && remainingPopCount > 0)
        {
            // Insert after the original instruction.
            int count = remainingPopCount == 1 ? 1 : 2;

            // Create a simple pop instrucion.
            byte replacementOpcode = count == 1 ?
                InstructionConstants.OP_POP :
                InstructionConstants.OP_POP2;

            Instruction replacementInstruction = new SimpleInstruction(replacementOpcode);

            if (DEBUG_ANALYSIS) System.out.println(": "+replacementInstruction.toString(offset));

            // Insert the pop instruction.
            codeAttributeEditor.insertAfterInstruction(offset,
                                                       replacementInstruction);

            remainingPopCount -= count;

            if (extraAddedInstructionVisitor != null)
            {
                replacementInstruction.accept(null, null, null, offset, extraAddedInstructionVisitor);
            }
        }

        if (remainingPopCount > 0)
        {
            throw new UnsupportedOperationException("Unsupported stack size reduction ["+popCount+"]");
        }
    }


    // Small utility methods.

    /**
     * Replaces the push instruction at the given offset by a simpler push
     * instruction, if possible.
     */
    private void replaceAnyPushInstruction(Clazz       clazz,
                                           int         offset,
                                           Instruction instruction)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            switch (pushedValue.computationalType())
            {
                case Value.TYPE_INTEGER:
                    replaceIntegerPushInstruction(clazz, offset, instruction);
                    break;
                case Value.TYPE_LONG:
                    replaceLongPushInstruction(clazz, offset, instruction);
                    break;
                case Value.TYPE_FLOAT:
                    replaceFloatPushInstruction(clazz, offset, instruction);
                    break;
                case Value.TYPE_DOUBLE:
                    replaceDoublePushInstruction(clazz, offset, instruction);
                    break;
                case Value.TYPE_REFERENCE:
                    replaceReferencePushInstruction(clazz, offset, instruction);
                    break;
            }
        }
    }


    /**
     * Replaces the integer pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceIntegerPushInstruction(Clazz       clazz,
                                               int         offset,
                                               Instruction instruction)
    {
        replaceIntegerPushInstruction(clazz,
                                      offset,
                                      instruction,
                                      partialEvaluator.getVariablesBefore(offset).size());
    }


    /**
     * Replaces the integer pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceIntegerPushInstruction(Clazz       clazz,
                                               int         offset,
                                               Instruction instruction,
                                               int         maxVariableIndex)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            int value = pushedValue.integerValue().value();
            if (value << 16 >> 16 == value)
            {
                replaceConstantPushInstruction(clazz,
                                       offset,
                                       instruction,
                                       InstructionConstants.OP_SIPUSH,
                                       value);
            }
        }
        else if (pushedValue.isSpecific())
        {
            TracedVariables variables = partialEvaluator.getVariablesBefore(offset);
            for (int variableIndex = 0; variableIndex < maxVariableIndex; variableIndex++)
            {
                if (pushedValue.equals(variables.load(variableIndex)))
                {
                    replaceVariablePushInstruction(clazz,
                                                   offset,
                                                   instruction,
                                                   InstructionConstants.OP_ILOAD,
                                                   variableIndex);
                }
            }
        }
    }


    /**
     * Replaces the long pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceLongPushInstruction(Clazz       clazz,
                                            int         offset,
                                            Instruction instruction)
    {
        replaceLongPushInstruction(clazz,
                                   offset,
                                   instruction,
                                   partialEvaluator.getVariablesBefore(offset).size());
    }


    /**
     * Replaces the long pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceLongPushInstruction(Clazz       clazz,
                                            int         offset,
                                            Instruction instruction,
                                            int         maxVariableIndex)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            long value = pushedValue.longValue().value();
            if (value == 0L ||
                value == 1L)
            {
                replaceConstantPushInstruction(clazz,
                                       offset,
                                       instruction,
                                       InstructionConstants.OP_LCONST_0,
                                       (int)value);
            }
        }
        else if (pushedValue.isSpecific())
        {
            TracedVariables variables = partialEvaluator.getVariablesBefore(offset);
            for (int variableIndex = 0; variableIndex < maxVariableIndex; variableIndex++)
            {
                if (pushedValue.equals(variables.load(variableIndex)))
                {
                    replaceVariablePushInstruction(clazz,
                                                   offset,
                                                   instruction,
                                                   InstructionConstants.OP_LLOAD,
                                                   variableIndex);
                }
            }
        }
    }


    /**
     * Replaces the float pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceFloatPushInstruction(Clazz       clazz,
                                             int         offset,
                                             Instruction instruction)
    {
        replaceFloatPushInstruction(clazz,
                                    offset,
                                    instruction,
                                    partialEvaluator.getVariablesBefore(offset).size());
    }


    /**
     * Replaces the float pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceFloatPushInstruction(Clazz       clazz,
                                             int         offset,
                                             Instruction instruction,
                                             int         maxVariableIndex)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            float value = pushedValue.floatValue().value();
            if (value == 0f ||
                value == 1f ||
                value == 2f)
            {
                replaceConstantPushInstruction(clazz,
                                       offset,
                                       instruction,
                                       InstructionConstants.OP_FCONST_0,
                                       (int)value);
            }
        }
        else if (pushedValue.isSpecific())
        {
            TracedVariables variables = partialEvaluator.getVariablesBefore(offset);
            for (int variableIndex = 0; variableIndex < maxVariableIndex; variableIndex++)
            {
                if (pushedValue.equals(variables.load(variableIndex)))
                {
                    replaceVariablePushInstruction(clazz,
                                                   offset,
                                                   instruction,
                                                   InstructionConstants.OP_FLOAD,
                                                   variableIndex);
                }
            }
        }
    }


    /**
     * Replaces the double pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceDoublePushInstruction(Clazz       clazz,
                                              int         offset,
                                              Instruction instruction)
    {
        replaceDoublePushInstruction(clazz,
                                     offset,
                                     instruction,
                                     partialEvaluator.getVariablesBefore(offset).size());
    }


    /**
     * Replaces the double pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceDoublePushInstruction(Clazz       clazz,
                                              int         offset,
                                              Instruction instruction,
                                              int         maxVariableIndex)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            double value = pushedValue.doubleValue().value();
            if (value == 0.0 ||
                value == 1.0)
            {
                replaceConstantPushInstruction(clazz,
                                       offset,
                                       instruction,
                                       InstructionConstants.OP_DCONST_0,
                                       (int)value);
            }
        }
        else if (pushedValue.isSpecific())
        {
            TracedVariables variables = partialEvaluator.getVariablesBefore(offset);
            for (int variableIndex = 0; variableIndex < maxVariableIndex; variableIndex++)
            {
                if (pushedValue.equals(variables.load(variableIndex)))
                {
                    replaceVariablePushInstruction(clazz,
                                                   offset,
                                                   instruction,
                                                   InstructionConstants.OP_DLOAD,
                                                   variableIndex);
                }
            }
        }
    }


    /**
     * Replaces the reference pushing instruction at the given offset by a
     * simpler push instruction, if possible.
     */
    private void replaceReferencePushInstruction(Clazz       clazz,
                                                 int         offset,
                                                 Instruction instruction)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isParticular())
        {
            // A reference value can only be specific if it is null.
            replaceConstantPushInstruction(clazz,
                                   offset,
                                   instruction,
                                   InstructionConstants.OP_ACONST_NULL,
                                   0);
        }
    }


    /**
     * Replaces the instruction at a given offset by a given push instruction
     * of a constant.
     */
    private void replaceConstantPushInstruction(Clazz       clazz,
                                                int         offset,
                                                Instruction instruction,
                                                byte        replacementOpcode,
                                                int         value)
    {
        replacePushInstruction(clazz,
                               offset,
                               instruction,
                               new SimpleInstruction(replacementOpcode, value).shrink());
    }


    /**
     * Replaces the instruction at a given offset by a given push instruction
     * of a variable.
     */
    private void replaceVariablePushInstruction(Clazz       clazz,
                                                int         offset,
                                                Instruction instruction,
                                                byte        replacementOpcode,
                                                int         variableIndex)
    {
        replacePushInstruction(clazz,
                               offset,
                               instruction,
                               new VariableInstruction(replacementOpcode, variableIndex).shrink());

        // Mark that the instruction has been simplified by loading an aliasing
        // variable.
        setAliasingVariable(offset, variableIndex);
    }


    /**
     * Replaces the instruction at a given offset by a given push instruction.
     */
    private void replacePushInstruction(Clazz       clazz,
                                        int         offset,
                                        Instruction instruction,
                                        Instruction replacementInstruction)
    {
        if (DEBUG_ANALYSIS) System.out.println("  Replacing instruction at ["+offset+"] by "+replacementInstruction.toString());

        codeAttributeEditor.replaceInstruction(offset, replacementInstruction);

        // Mark that the instruction has been simplified.
        markSimplification(clazz, offset, instruction);

        // Visit the instruction, if required.
        if (extraPushInstructionVisitor != null)
        {
            // Note: we're not passing the right arguments for now, knowing that
            // they aren't used anyway.
            extraPushInstructionVisitor.visitSimpleInstruction(null, null, null, offset, null);
        }
    }


    /**
     * Replaces the instruction at a given offset by a static invocation.
     */
    private void replaceInvocationInstruction(Clazz               clazz,
                                              int                 offset,
                                              ConstantInstruction constantInstruction)
    {
        // Remember the replacement instruction.
        Instruction replacementInstruction =
             new ConstantInstruction(InstructionConstants.OP_INVOKESTATIC,
                                     constantInstruction.constantIndex).shrink();

        if (DEBUG_ANALYSIS) System.out.println("  Replacing instruction at ["+offset+"] by "+replacementInstruction.toString());

        codeAttributeEditor.replaceInstruction(offset, replacementInstruction);

        // Mark that the instruction has been simplified.
        //markSimplification(clazz, offset, constantInstruction);

        // Visit the instruction, if required.
        //if (extraPushInstructionVisitor != null)
        //{
        //    // Note: we're not passing the right arguments for now, knowing that
        //    // they aren't used anyway.
        //    extraPushInstructionVisitor.visitSimpleInstruction(null, null, null, offset, null);
        //}
    }


    /**
     * Replaces the given 'jsr' instruction by a simpler branch instruction,
     * if possible.
     */
    private void replaceJsrInstruction(Clazz             clazz,
                                       int               offset,
                                       BranchInstruction branchInstruction)
    {
        // Is the subroutine ever returning?
        if (!partialEvaluator.isSubroutineReturning(offset + branchInstruction.branchOffset))
        {
            // All 'jsr' instructions to this subroutine can be replaced
            // by unconditional branch instructions.
            replaceBranchInstruction(clazz, offset, branchInstruction);
        }
        else if (!partialEvaluator.isTraced(offset + branchInstruction.length(offset)))
        {
            // We have to make sure the instruction after this 'jsr'
            // instruction is valid, even if it is never reached.
            replaceByInfiniteLoop(clazz, offset + branchInstruction.length(offset), branchInstruction);
        }
    }


    /**
     * Deletes the given branch instruction, or replaces it by a simpler branch
     * instruction, if possible.
     */
    private void replaceBranchInstruction(Clazz       clazz,
                                          int         offset,
                                          Instruction instruction)
    {
        InstructionOffsetValue branchTargets = partialEvaluator.branchTargets(offset);

        // Is there exactly one branch target (not from a goto or jsr)?
        if (branchTargets != null &&
            branchTargets.instructionOffsetCount() == 1)
        {
            // Is it branching to the next instruction?
            int branchOffset = branchTargets.instructionOffset(0) - offset;
            if (branchOffset == instruction.length(offset))
            {
                if (DEBUG_ANALYSIS) System.out.println("  Ignoring zero branch instruction at ["+offset+"]");
            }
            else
            {
                // Replace the branch instruction by a simple branch instruction.
                Instruction replacementInstruction =
                    new BranchInstruction(InstructionConstants.OP_GOTO_W,
                                          branchOffset).shrink();

                if (DEBUG_ANALYSIS) System.out.println("  Replacing branch instruction at ["+offset+"] by "+replacementInstruction.toString());

                codeAttributeEditor.replaceInstruction(offset,
                                                       replacementInstruction);

                // Mark that the instruction has been simplified.
                markSimplification(clazz, offset, instruction);

                // Visit the instruction, if required.
                if (extraBranchInstructionVisitor != null)
                {
                    extraBranchInstructionVisitor.visitBranchInstruction(null, null, null, offset, null);
                }
            }
        }
    }


    /**
     * Makes sure all branch targets of the given switch instruction are valid.
     */
    private void replaceSwitchInstruction(Clazz             clazz,
                                          int               offset,
                                          SwitchInstruction switchInstruction)
    {
        // Get the actual branch targets.
        InstructionOffsetValue branchTargets = partialEvaluator.branchTargets(offset);

        // Get an offset that can serve as a valid default offset.
        int defaultOffset =
            branchTargets.instructionOffset(branchTargets.instructionOffsetCount()-1) -
            offset;

        Instruction replacementInstruction = null;

        // Check the jump offsets.
        int[] jumpOffsets = switchInstruction.jumpOffsets;
        for (int index = 0; index < jumpOffsets.length; index++)
        {
            if (!branchTargets.contains(offset + jumpOffsets[index]))
            {
                // Replace the unused offset.
                jumpOffsets[index] = defaultOffset;

                // Remember to replace the instruction.
                replacementInstruction = switchInstruction;
            }
        }

        // Check the default offset.
        if (!branchTargets.contains(offset + switchInstruction.defaultOffset))
        {
            // Replace the unused offset.
            switchInstruction.defaultOffset = defaultOffset;

            // Remember to replace the instruction.
            replacementInstruction = switchInstruction;
        }

        if (replacementInstruction != null)
        {
            if (DEBUG_ANALYSIS) System.out.println("  Replacing switch instruction at ["+offset+"] by "+replacementInstruction.toString());

            codeAttributeEditor.replaceInstruction(offset,
                                                   replacementInstruction);

            // Visit the instruction, if required.
            if (extraBranchInstructionVisitor != null)
            {
                // Note: we're not passing the right arguments for now,
                // knowing that they aren't used anyway.
                extraBranchInstructionVisitor.visitBranchInstruction(null, null, null, offset, null);
            }
        }
    }


    /**
     * Replaces the given instruction by an infinite loop.
     */
    private void replaceByInfiniteLoop(Clazz       clazz,
                                       int         offset,
                                       Instruction instruction)
    {
        if (DEBUG_ANALYSIS) System.out.println("  Inserting infinite loop at ["+offset+"]");

        // Replace the instruction by an infinite loop.
        Instruction replacementInstruction =
            new BranchInstruction(InstructionConstants.OP_GOTO, 0);

        codeAttributeEditor.replaceInstruction(offset,
                                               replacementInstruction);

        // Mark the instruction.
        markInstruction(offset);
        markSimplification(clazz, offset, instruction);
    }


    // Small utility methods.

    /**
     * Returns whether the given instruction is a dup or swap instruction
     * (dup, dup_x1, dup_x2, dup2, dup2_x1, dup2_x2, swap).
     */
    private boolean isDupOrSwap(Instruction instruction)
    {
        return instruction.opcode >= InstructionConstants.OP_DUP &&
               instruction.opcode <= InstructionConstants.OP_SWAP;
    }


    /**
     * Returns whether the given instruction is a pop instruction
     * (pop, pop2).
     */
    private boolean isPop(Instruction instruction)
    {
        return instruction.opcode == InstructionConstants.OP_POP ||
               instruction.opcode == InstructionConstants.OP_POP2;
    }


    /**
     * Returns whether any traced but unnecessary instruction between the two
     * given offsets is branching over the second given offset.
     */
    private boolean isAnyUnnecessaryInstructionBranchingOver(int instructionOffset1,
                                                             int instructionOffset2)
    {
        for (int offset = instructionOffset1; offset < instructionOffset2; offset++)
        {
            // Is it a traced but unmarked straddling branch?
            if (partialEvaluator.isTraced(offset) &&
                !isInstructionNecessary(offset)   &&
                isAnyLargerThan(partialEvaluator.branchTargets(offset),
                                instructionOffset2))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether all of the given instruction offsets (at least one)
     * are smaller than or equal to the given offset.
     */
    private boolean isAllSmallerThanOrEqual(InstructionOffsetValue instructionOffsets,
                                            int                    instructionOffset)
    {
        if (instructionOffsets != null)
        {
            // Loop over all instruction offsets.
            int branchCount = instructionOffsets.instructionOffsetCount();
            if (branchCount > 0)
            {
                for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
                {
                    // Is the offset larger than the reference offset?
                    if (instructionOffsets.instructionOffset(branchIndex) > instructionOffset)
                    {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether any of the given instruction offsets (at least one)
     * is larger than the given offset.
     */
    private boolean isAnyLargerThan(InstructionOffsetValue instructionOffsets,
                                    int                    instructionOffset)
    {
        if (instructionOffsets != null)
        {
            // Loop over all instruction offsets.
            int branchCount = instructionOffsets.instructionOffsetCount();
            if (branchCount > 0)
            {
                for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
                {
                    // Is the offset larger than the reference offset?
                    if (instructionOffsets.instructionOffset(branchIndex) > instructionOffset)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * Initializes the necessary data structure.
     */
    private void initializeNecessary(CodeAttribute codeAttribute)
    {
        int codeLength = codeAttribute.u4codeLength;
        int maxLocals  = codeAttribute.u2maxLocals;
        int maxStack   = codeAttribute.u2maxStack;

        // Create new arrays for storing information at each instruction offset.
        if (variablesNecessaryAfter.length    < codeLength ||
            variablesNecessaryAfter[0].length < maxLocals)
        {
            variablesNecessaryAfter = new boolean[codeLength][maxLocals];
        }
        else
        {
            for (int offset = 0; offset < codeLength; offset++)
            {
                for (int index = 0; index < maxLocals; index++)
                {
                    variablesNecessaryAfter[offset][index] = false;
                }
            }
        }

        if (stacksNecessaryAfter.length    < codeLength ||
            stacksNecessaryAfter[0].length < maxStack)
        {
            stacksNecessaryAfter = new boolean[codeLength][maxStack];
        }
        else
        {
            for (int offset = 0; offset < codeLength; offset++)
            {
                for (int index = 0; index < maxStack; index++)
                {
                    stacksNecessaryAfter[offset][index] = false;
                }
            }
        }

        if (stacksSimplifiedBefore.length    < codeLength ||
            stacksSimplifiedBefore[0].length < maxStack)
        {
            stacksSimplifiedBefore = new boolean[codeLength][maxStack];
        }
        else
        {
            for (int offset = 0; offset < codeLength; offset++)
            {
                for (int index = 0; index < maxStack; index++)
                {
                    stacksSimplifiedBefore[offset][index] = false;
                }
            }
        }

        if (instructionsNecessary.length < codeLength)
        {
            instructionsNecessary  = new boolean[codeLength];
            instructionsSimplified = new boolean[codeLength];
            aliasingVariables      = new int[codeLength];
        }
        else
        {
            for (int index = 0; index < codeLength; index++)
            {
                instructionsNecessary[index]  = false;
                instructionsSimplified[index] = false;
            }
        }

        for (int index = 0; index < codeLength; index++)
        {
            aliasingVariables[index] = -1;
        }
    }


    /**
     * Returns whether the given stack entry is present after execution of the
     * instruction at the given offset.
     */
    private boolean isStackEntriesNecessaryAfter(int instructionOffset,
                                                 int stackIndex1,
                                                 int stackIndex2)
    {
        boolean present1 = isStackEntryNecessaryAfter(instructionOffset, stackIndex1);
        boolean present2 = isStackEntryNecessaryAfter(instructionOffset, stackIndex2);

//        if (present1 ^ present2)
//        {
//            throw new UnsupportedOperationException("Can't handle partial use of dup2 instructions");
//        }

        return present1 || present2;
    }


    /**
     * Returns whether the specified variable must be initialized at the
     * specified offset, according to the verifier of the JVM.
     */
    private boolean isVariableInitializationNecessary(Clazz         clazz,
                                                      Method        method,
                                                      CodeAttribute codeAttribute,
                                                      int           initializationOffset,
                                                      int           variableIndex)
    {
        int codeLength = codeAttribute.u4codeLength;

        // Is the variable necessary anywhere at all?
        if (isVariableNecessaryAfterAny(0, codeLength, variableIndex))
        {
            if (DEBUG_ANALYSIS) System.out.println("Simple partial evalutation for initialization of variable v"+variableIndex+" at ["+initializationOffset+"]");

            // Lazily compute perform simple partial evaluation, the way the
            // JVM preverifier would do it.
            simplePartialEvaluator.visitCodeAttribute(clazz, method, codeAttribute);

            if (DEBUG_ANALYSIS) System.out.println("End of simple partial evalutation for initialization of variable v"+variableIndex+" at ["+initializationOffset+"]");

            // Check if the variable is necessary elsewhere.
            for (int offset = 0; offset < codeLength; offset++)
            {
                if (isInstructionNecessary(offset))
                {
                    Value producer = partialEvaluator.getVariablesBefore(offset).getProducerValue(variableIndex);
                    if (producer != null)
                    {
                        Value simpleProducer = simplePartialEvaluator.getVariablesBefore(offset).getProducerValue(variableIndex);
                        if (simpleProducer != null)
                        {
                            InstructionOffsetValue producerOffsets =
                                producer.instructionOffsetValue();
                            InstructionOffsetValue simpleProducerOffsets =
                                simpleProducer.instructionOffsetValue();

                            // Does the sophisticated partial evaluation have fewer
                            // producers than the simple one?
                            // And does the simple partial evaluation point to an
                            // initialization of the variable?
                            if (producerOffsets.instructionOffsetCount() <
                                simpleProducerOffsets.instructionOffsetCount() &&
                                isVariableNecessaryAfterAny(producerOffsets, variableIndex) &&
                                simpleProducerOffsets.contains(initializationOffset))
                            {
                                // Then the initialization is necessary.
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }


    private void markVariableAfter(int instructionOffset,
                                   int variableIndex)
    {
        if (!isVariableNecessaryAfter(instructionOffset, variableIndex))
        {
            if (DEBUG_ANALYSIS) System.out.print("["+instructionOffset+".v"+variableIndex+"],");

            variablesNecessaryAfter[instructionOffset][variableIndex] = true;

            if (maxMarkedOffset < instructionOffset)
            {
                maxMarkedOffset = instructionOffset;
            }
        }
    }


    /**
     * Returns whether the specified variable is ever necessary after any
     * instruction in the specified block.
     */
    private boolean isVariableNecessaryAfterAny(int startOffset,
                                                int endOffset,
                                                int variableIndex)
    {
        for (int offset = startOffset; offset < endOffset; offset++)
        {
            if (isVariableNecessaryAfter(offset, variableIndex))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether the specified variable is ever necessary after any
     * instruction in the specified set of instructions offsets.
     */
    private boolean isVariableNecessaryAfterAny(InstructionOffsetValue instructionOffsetValue,
                                                int                    variableIndex)
    {
        int count = instructionOffsetValue.instructionOffsetCount();

        for (int index = 0; index < count; index++)
        {
            if (isVariableNecessaryAfter(instructionOffsetValue.instructionOffset(index),
                                         variableIndex))
            {
                return true;
            }
        }

        return false;
    }


    private boolean isVariableNecessaryAfter(int instructionOffset,
                                             int variableIndex)
    {
        return instructionOffset == PartialEvaluator.AT_METHOD_ENTRY ||
               variablesNecessaryAfter[instructionOffset][variableIndex];
    }


    private void markStackEntryAfter(int instructionOffset,
                                     int stackIndex)
    {
        if (!isStackEntryNecessaryAfter(instructionOffset, stackIndex))
        {
            if (DEBUG_ANALYSIS) System.out.print("["+instructionOffset+".s"+stackIndex+"],");

            stacksNecessaryAfter[instructionOffset][stackIndex] = true;

            if (maxMarkedOffset < instructionOffset)
            {
                maxMarkedOffset = instructionOffset;
            }
        }
    }


    private boolean isAnyStackEntryNecessaryAfter(InstructionOffsetValue instructionOffsets,
                                                  int                    stackIndex)
    {
        int offsetCount = instructionOffsets.instructionOffsetCount();

        for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++)
        {
            if (isStackEntryNecessaryAfter(instructionOffsets.instructionOffset(offsetIndex), stackIndex))
            {
                return true;
            }
        }

        return false;
    }


    private boolean isStackEntryNecessaryAfter(int instructionOffset,
                                               int stackIndex)
    {
        return instructionOffset == PartialEvaluator.AT_CATCH_ENTRY ||
               stacksNecessaryAfter[instructionOffset][stackIndex];
    }


    private void markStackSimplificationBefore(int instructionOffset,
                                               int stackIndex)
    {
        stacksSimplifiedBefore[instructionOffset][stackIndex] = true;
    }


    private boolean isStackSimplifiedBefore(int instructionOffset,
                                            int stackIndex)
    {
        return stacksSimplifiedBefore[instructionOffset][stackIndex];
    }


    private void markSimplification(Clazz       clazz,
                                    int         instructionOffset,
                                    Instruction instruction)
    {
        // Mark the instruction itself.
        instructionsSimplified[instructionOffset] = true;

        // Also mark all stack entries that would be popped.
        TracedStack tracedStack =
            partialEvaluator.getStackBefore(instructionOffset);

        int top      = tracedStack.size() - 1;
        int popCount = instruction.stackPopCount(clazz);

        for (int stackIndex = 0; stackIndex < popCount; stackIndex++)
        {
            markStackSimplificationBefore(instructionOffset, top - stackIndex);
        }
    }


    private boolean isInstructionSimplified(int instructionOffset)
    {
        return instructionsSimplified[instructionOffset];
    }


    private void setAliasingVariable(int instructionOffset,
                                     int variableIndex)
    {
        aliasingVariables[instructionOffset] = variableIndex;
    }


    private int getAliasingVariable(int instructionOffset)
    {
        return aliasingVariables[instructionOffset];
    }


    private void markInstruction(int instructionOffset)
    {
        if (!isInstructionNecessary(instructionOffset))
        {
            if (DEBUG_ANALYSIS) System.out.print(instructionOffset+",");

            instructionsNecessary[instructionOffset] = true;

            if (maxMarkedOffset < instructionOffset)
            {
                maxMarkedOffset = instructionOffset;
            }
        }
    }


    private boolean isAnyInstructionNecessary(int instructionOffset1,
                                              int instructionOffset2)
    {
        for (int instructionOffset = instructionOffset1;
             instructionOffset < instructionOffset2;
             instructionOffset++)
        {
            if (isInstructionNecessary(instructionOffset))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns the highest offset of an instruction that has been marked as
     * necessary, before the given offset.
     */
    private int lastNecessaryInstructionOffset(int instructionOffset)
    {
        for (int offset = instructionOffset-1; offset >= 0; offset--)
        {
            if (isInstructionNecessary(instructionOffset))
            {
                return offset;
            }
        }

        return 0;
    }


    private boolean isInstructionNecessary(int instructionOffset)
    {
        return instructionOffset == PartialEvaluator.AT_METHOD_ENTRY ||
               instructionsNecessary[instructionOffset];
    }
}
