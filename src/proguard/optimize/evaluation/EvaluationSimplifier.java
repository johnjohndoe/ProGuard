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
import proguard.classfile.visitor.ClassPrinter;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;
import proguard.evaluation.value.*;
import proguard.optimize.info.SideEffectInstructionChecker;

/**
 * This AttributeVisitor simplifies the code attributes that it visits, based
 * on partial evaluation.
 *
 * @author Eric Lafortune
 */
public class EvaluationSimplifier
extends      SimplifiedVisitor
implements   AttributeVisitor,
             InstructionVisitor
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
    private final SideEffectInstructionChecker sideEffectInstructionChecker = new SideEffectInstructionChecker(true);
    private final CodeAttributeEditor          codeAttributeEditor          = new CodeAttributeEditor();

    private boolean[] isNecessary  = new boolean[ClassConstants.TYPICAL_CODE_LENGTH];
    private boolean[] isSimplified = new boolean[ClassConstants.TYPICAL_CODE_LENGTH];


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

        // TODO: Remove this when the partial evaluator has stabilized.
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

        codeAttribute.instructionsAccept(clazz, method, this);

        // Mark all essential instructions that have been encountered as used.
        if (DEBUG_ANALYSIS) System.out.println("Usage initialization: ");

        // The invocation of the "super" or "this" <init> method inside a
        // constructor is always necessary.
        int superInitializationOffset = partialEvaluator.superInitializationOffset();
        if (superInitializationOffset != PartialEvaluator.NONE)
        {
            if (DEBUG_ANALYSIS) System.out.print(superInitializationOffset+"(super.<init>),");

            isNecessary[superInitializationOffset] = true;
        }

        // Also mark infinite loops and instructions that cause side effects.
        int offset = 0;
        do
        {
            if (partialEvaluator.isTraced(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                // Mark that the instruction is necessary if it is an infinite loop.
                if (instruction.opcode == InstructionConstants.OP_GOTO &&
                    partialEvaluator.branchTargets(offset).instructionOffsetValue().instructionOffset(0) == offset)
                {
                    if (DEBUG_ANALYSIS) System.out.print(offset+"(infinite loop),");
                    isNecessary[offset] = true;
                }

                // Mark that the instruction is necessary if it has side effects.
                else if (sideEffectInstructionChecker.hasSideEffects(clazz,
                                                                     method,
                                                                     codeAttribute,
                                                                     offset,
                                                                     instruction))
                {
                    if (DEBUG_ANALYSIS) System.out.print(offset+",");
                    isNecessary[offset] = true;
                }
            }

            offset++;
        }
        while (offset < codeLength);
        if (DEBUG_ANALYSIS) System.out.println();


        // Mark all other instructions on which the essential instructions
        // depend. Instead of doing this recursively, we loop across all
        // instructions, starting at the last one, and restarting at any
        // higher, previously unmarked instruction that is being marked.
        if (DEBUG_ANALYSIS) System.out.println("Usage marking:");

        int lowestNecessaryOffset = codeLength;
        offset = codeLength - 1;
        do
        {
            int nextOffset = offset - 1;

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Check if this instruction is a branch origin from a branch that
            // straddles some marked code.
            nextOffset = markStraddlingBranches(offset,
                                                partialEvaluator.branchTargets(offset),
                                                true,
                                                lowestNecessaryOffset,
                                                nextOffset);

            // Mark the producers on which this instruction depends.
            if (isNecessary[offset] &&
                !isSimplified[offset])
            {
                nextOffset = markProducers(offset, nextOffset);
            }

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Check if this instruction is a branch target from a branch that
            // straddles some marked code.
            nextOffset = markStraddlingBranches(offset,
                                                partialEvaluator.branchOrigins(offset),
                                                false,
                                                lowestNecessaryOffset,
                                                nextOffset);

            if (DEBUG_ANALYSIS)
            {
                if (nextOffset >= offset)
                {
                    System.out.println();
                }
            }

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Update the index of the instruction to be investigated next.
            offset = nextOffset;
        }
        while (offset >= 0);
        if (DEBUG_ANALYSIS) System.out.println();


        // Insert pop instructions before unmarked popping instructions,
        // if required to keep the stack consistent.
        if (DEBUG_ANALYSIS) System.out.println("Unmarked pop fixing:");

        // Also figure out the offset of the last dup/swap instruction.
        int highestDupOffset = -1;

        offset = codeLength - 1;
        do
        {
            if (partialEvaluator.isTraced(offset) &&
                !isNecessary[offset] &&
                !isSimplified[offset])
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                // Make sure any non-dup/swap instructions are always consistent
                // at this offset.
                if (!isDupOrSwap(instruction))
                {
                    // Make sure any popping instructions are always
                    // consistent after this offset.
                    fixPopInstruction(clazz,
                                      codeAttribute,
                                      offset,
                                      instruction);
                }
                else if (highestDupOffset < 0)
                {
                    // Remember the offset of the last dup/swap instruction.
                    highestDupOffset = offset;
                }
            }

            offset--;
        }
        while (offset >= 0);
        if (DEBUG_ANALYSIS) System.out.println();


        // Insert dup instructions where necessary, to keep the stack consistent.
        boolean updated;
        do
        {
            if (DEBUG_ANALYSIS) System.out.println("Dup marking:");

            // Repeat going over all instructions, as long as dup/swap
            // instructions are updated.
            updated = false;

            offset = highestDupOffset;
            while (offset >= 0)
            {
                if (partialEvaluator.isTraced(offset))
                {
                    Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                        offset);

                    // Make sure any dup/swap instructions are always consistent
                    // at this offset.
                    if (isDupOrSwap(instruction))
                    {
                        updated |= fixDupInstruction(clazz,
                                                     codeAttribute,
                                                     offset,
                                                     instruction);
                    }
                }

                offset--;
            }
        }
        while (updated);
        if (DEBUG_ANALYSIS) System.out.println();


        // Insert pop instructions after marked pushing instructions,
        // if required to keep the stack consistent.
        if (DEBUG_ANALYSIS) System.out.println("Marked push fixing:");

        offset = codeLength - 1;
        do
        {
            if (//partialEvaluator.isTraced(offset) &&
                isNecessary[offset] &&
                !isSimplified[offset])
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);

                // Make sure any non-dup/swap instructions are always consistent
                // at this offset.
                if (!isDupOrSwap(instruction))
                {
                    // Make sure any pushing instructions are always
                    // consistent after this offset.
                    fixPushInstruction(clazz,
                                       codeAttribute,
                                       offset,
                                       instruction);
                }
            }

            offset--;
        }
        while (offset >= 0);
        if (DEBUG_ANALYSIS) System.out.println();


        // Mark unmarked pop instructions after dup instructions,
        // if required to keep the stack consistent.
        // This is mainly required to fix "synchronized(C.class)" constructs
        // as compiled by jikes and by the Eclipse compiler:
        //    ...
        //    dup
        //    ifnonnull ...
        //    pop
        //    ...
        if (DEBUG_ANALYSIS) System.out.println("Pop marking:");

        offset = codeLength - 1;
        do
        {
            if (//partialEvaluator.isTraced(offset) &&
                isNecessary[offset]   &&
                !isSimplified[offset] &&
                !codeAttributeEditor.isModified(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);
                if (isDupOrSwap(instruction))
                {
                    markConsumingPopInstructions(clazz,
                                                 codeAttribute,
                                                 offset,
                                                 instruction);
                }
            }

            offset--;
        }
        while (offset >= 0);
        if (DEBUG_ANALYSIS) System.out.println();


        // Mark branches straddling just inserted push/pop instructions.
        if (DEBUG_ANALYSIS) System.out.println("Final straddling branch marking:");

        lowestNecessaryOffset = codeLength;
        offset = codeLength - 1;
        do
        {
            int nextOffset = offset - 1;

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Check if this instruction is a branch origin from a branch that
            // straddles some marked code.
            nextOffset = markAndSimplifyStraddlingBranches(offset,
                                                           partialEvaluator.branchTargets(offset),
                                                           lowestNecessaryOffset,
                                                           nextOffset);

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Check if this instruction is a branch target from a branch that
            // straddles some marked code.
            nextOffset = markAndSimplifyStraddlingBranches(partialEvaluator.branchOrigins(offset),
                                                           offset,
                                                           lowestNecessaryOffset,
                                                           nextOffset);

            if (DEBUG_ANALYSIS)
            {
                if (nextOffset >= offset)
                {
                    System.out.println();
                }
            }

            // Update the lowest index of all marked instructions higher up.
            if (isNecessary[offset])
            {
                lowestNecessaryOffset = offset;
            }

            // Update the index of the instruction to be investigated next.
            offset = nextOffset;
        }
        while (offset >= 0);
        if (DEBUG_ANALYSIS) System.out.println();


        // Mark variable initializations, even if they aren't strictly necessary.
        // The virtual machine's verification step is not smart enough to see
        // this, and may complain otherwise.
        if (DEBUG_ANALYSIS) System.out.println("Initialization marking: ");

        offset = 0;
        do
        {
            // Is it an initialization that hasn't been marked yet, and whose
            // corresponding variable is used for storage?
            int variableIndex = partialEvaluator.initializedVariable(offset);
            if (variableIndex >= 0 &&
                !isNecessary[offset] &&
                isVariableReferenced(codeAttribute, offset+1, variableIndex))
            {
                if (DEBUG_ANALYSIS) System.out.println(offset +",");

                // Figure out what kind of initialization value has to be stored.
                int pushComputationalType = partialEvaluator.getVariablesAfter(offset).getValue(variableIndex).computationalType();
                increaseStackSize(offset, pushComputationalType, false);
            }

            offset++;
        }
        while (offset < codeLength);
        if (DEBUG_ANALYSIS) System.out.println();

        if (DEBUG_RESULTS)
        {
            System.out.println("Simplification results:");
            offset = 0;
            do
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                    offset);
                System.out.println((isNecessary[offset] ? " + " : " - ")+instruction.toString(offset));

                if (partialEvaluator.isTraced(offset))
                {
                    InstructionOffsetValue varProducerOffsets = partialEvaluator.varProducerOffsets(offset);
                    if (varProducerOffsets.instructionOffsetCount() > 0)
                    {
                        System.out.println("     has overall been using information from instructions setting vars: "+varProducerOffsets);
                    }

                    InstructionOffsetValue stackProducerOffsets = partialEvaluator.stackProducerOffsets(offset);
                    if (stackProducerOffsets.instructionOffsetCount() > 0)
                    {
                        System.out.println("     has overall been using information from instructions setting stack: "+stackProducerOffsets);
                    }

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

                    //System.out.println("     Vars:  "+vars[offset]);
                    //System.out.println("     Stack: "+stacks[offset]);
                }

                offset += instruction.length(offset);
            }
            while (offset < codeLength);
        }

        // Delete all instructions that are not used.
        offset = 0;
        do
        {
            Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                offset);
            if (!isNecessary[offset])
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

        // Apply all accumulated changes to the code.
        codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);
    }


    /**
     * Marks the producers at the given offsets.
     * @param consumerOffset the offset of the consumer.
     * @param nextOffset     the offset of the instruction to be investigated next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markProducers(int consumerOffset,
                              int nextOffset)
    {
        if (DEBUG_ANALYSIS) System.out.print(consumerOffset);

        // Mark all instructions whose variable values are used.
        nextOffset = markProducers(partialEvaluator.varProducerOffsets(consumerOffset), nextOffset);

        // Mark all instructions whose stack values are used.
        nextOffset = markProducers(partialEvaluator.stackProducerOffsets(consumerOffset), nextOffset);

        // Mark the initializer of the variables and stack entries used by the
        // instruction.
        nextOffset = markProducer(partialEvaluator.initializationOffset(consumerOffset), nextOffset);

        if (DEBUG_ANALYSIS) System.out.print(",");

        return nextOffset;
    }


    /**
     * Marks the instructions at the given offsets.
     * @param producerOffsets the offsets of the producers to be marked.
     * @param nextOffset      the offset of the instruction to be investigated
     *                        next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markProducers(InstructionOffsetValue producerOffsets,
                              int                    nextOffset)
    {
        if (producerOffsets != null)
        {
            int offsetCount = producerOffsets.instructionOffsetCount();
            for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++)
            {
                // Has the other instruction been marked yet?
                int offset = producerOffsets.instructionOffset(offsetIndex);
                nextOffset = markProducer(offset, nextOffset);
            }
        }

        return nextOffset;
    }


    /**
     * Marks the instruction at the given offset.
     * @param producerOffset the offsets of the producer to be marked.
     * @param nextOffset     the offset of the instruction to be investigated
     *                       next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markProducer(int producerOffset, int nextOffset)
    {
        if (producerOffset > PartialEvaluator.AT_METHOD_ENTRY &&
            !isNecessary[producerOffset])
        {
            if (DEBUG_ANALYSIS) System.out.print("["+producerOffset +"]");

            // Mark it.
            isNecessary[producerOffset] = true;

            // Restart at this instruction if it has a higher offset.
            if (nextOffset < producerOffset)
            {
                if (DEBUG_ANALYSIS) System.out.print("!");

                nextOffset = producerOffset;
            }
        }

        return nextOffset;
    }


    /**
     * Marks the branch instructions of straddling branches, if they straddle
     * some code that has been marked.
     * @param index                 the offset of the branch origin or branch target.
     * @param branchOffsets         the offsets of the straddling branch targets
     *                              or branch origins.
     * @param isPointingToTargets   <code>true</code> if the above offsets are
     *                              branch targets, <code>false</code> if they
     *                              are branch origins.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     * @param nextOffset            the offset of the instruction to be investigated
     *                              next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markStraddlingBranches(int                    index,
                                       InstructionOffsetValue branchOffsets,
                                       boolean                isPointingToTargets,
                                       int                    lowestNecessaryOffset,
                                       int                    nextOffset)
    {
        if (branchOffsets != null)
        {
            // Loop over all branch origins.
            int branchCount = branchOffsets.instructionOffsetCount();
            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
            {
                // Is the branch straddling any necessary instructions?
                int branch = branchOffsets.instructionOffset(branchIndex);

                // Is the offset pointing to a branch origin or to a branch target?
                nextOffset = isPointingToTargets ?
                    markStraddlingBranch(index, branch, lowestNecessaryOffset, nextOffset) :
                    markStraddlingBranch(branch, index, lowestNecessaryOffset, nextOffset);
            }
        }

        return nextOffset;
    }


    /**
     * Marks the given branch instruction, if it straddles some code that has
     * been marked.
     * @param branchOrigin          the branch origin.
     * @param branchTarget          the branch target.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     * @param nextOffset            the offset of the instruction to be investigated
     *                              next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markStraddlingBranch(int branchOrigin,
                                     int branchTarget,
                                     int lowestNecessaryOffset,
                                     int nextOffset)
    {
        // Has the branch origin been marked yet, and is it straddling the
        // lowest necessary instruction?
        if (!isNecessary[branchOrigin] &&
            isStraddlingBranch(branchOrigin, branchTarget, lowestNecessaryOffset))
        {
            if (DEBUG_ANALYSIS) System.out.print("["+branchOrigin+"->"+branchTarget+"]");

            // Mark the branch origin.
            isNecessary[branchOrigin] = true;

            // Restart at the branch origin if it has a higher offset.
            if (nextOffset < branchOrigin)
            {
                if (DEBUG_ANALYSIS) System.out.print("!");

                nextOffset = branchOrigin;
            }
        }

        return nextOffset;
    }


    /**
     * Marks and simplifies the branch instructions of straddling branches,
     * if they straddle some code that has been marked.
     * @param branchOrigin          the branch origin.
     * @param branchTargets         the branch targets.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     * @param nextOffset            the offset of the instruction to be investigated
     *                              next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markAndSimplifyStraddlingBranches(int                    branchOrigin,
                                                  InstructionOffsetValue branchTargets,
                                                  int                    lowestNecessaryOffset,
                                                  int                    nextOffset)
    {
        if (branchTargets != null &&
            !isNecessary[branchOrigin])
        {
            // Loop over all branch targets.
            int branchCount = branchTargets.instructionOffsetCount();
            if (branchCount > 0)
            {
                for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
                {
                    // Is the branch straddling any necessary instructions?
                    int branchTarget = branchTargets.instructionOffset(branchIndex);

                    if (!isStraddlingBranch(branchOrigin,
                                            branchTarget,
                                            lowestNecessaryOffset))
                    {
                        return nextOffset;
                    }
                }

                nextOffset = markAndSimplifyStraddlingBranch(branchOrigin,
                                                             branchTargets.instructionOffset(0),
                                                             lowestNecessaryOffset,
                                                             nextOffset);
            }
        }

        return nextOffset;
    }


    /**
     * Marks and simplifies the branch instructions of straddling branches,
     * if they straddle some code that has been marked.
     * @param branchOrigins         the branch origins.
     * @param branchTarget          the branch target.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     * @param nextOffset            the offset of the instruction to be investigated
     *                              next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markAndSimplifyStraddlingBranches(InstructionOffsetValue branchOrigins,
                                                  int                    branchTarget,
                                                  int                    lowestNecessaryOffset,
                                                  int                    nextOffset)
    {
        if (branchOrigins != null)
        {
            // Loop over all branch origins.
            int branchCount = branchOrigins.instructionOffsetCount();
            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++)
            {
                // Is the branch straddling any necessary instructions?
                int branchOrigin = branchOrigins.instructionOffset(branchIndex);

                nextOffset = markAndSimplifyStraddlingBranch(branchOrigin,
                                                             branchTarget,
                                                             lowestNecessaryOffset,
                                                             nextOffset);
            }
        }

        return nextOffset;
    }


    /**
     * Marks and simplifies the given branch instruction, if it straddles some
     * code that has been marked.
     * @param branchOrigin          the branch origin.
     * @param branchTarget          the branch target.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     * @param nextOffset            the offset of the instruction to be investigated
     *                              next.
     * @return the updated offset of the instruction to be investigated next.
     *         It is always greater than or equal the original offset, because
     *         instructions are investigated starting at the highest index.
     */
    private int markAndSimplifyStraddlingBranch(int branchOrigin,
                                                int branchTarget,
                                                int lowestNecessaryOffset,
                                                int nextOffset)
    {
        // Has the branch origin been marked yet, and is it straddling the
        // lowest necessary instruction?
        if (!isNecessary[branchOrigin] &&
            isStraddlingBranch(branchOrigin, branchTarget, lowestNecessaryOffset))
        {
            if (DEBUG_ANALYSIS) System.out.print("["+branchOrigin+"->"+branchTarget+"]");

            // Mark the branch origin.
            isNecessary[branchOrigin] = true;

            // Replace the branch instruction by a simple branch instrucion.
            Instruction replacementInstruction =
                new BranchInstruction(InstructionConstants.OP_GOTO_W,
                                      branchTarget - branchOrigin).shrink();

            codeAttributeEditor.replaceInstruction(branchOrigin,
                                                   replacementInstruction);

            // Restart at the branch origin if it has a higher offset.
            if (nextOffset < branchOrigin)
            {
                if (DEBUG_ANALYSIS) System.out.print("!");

                nextOffset = branchOrigin;
            }
        }

        return nextOffset;
    }


    /**
     * Returns whether the given branch straddling some code that has been marked.
     * @param branchOrigin          the branch origin.
     * @param branchTarget          the branch target.
     * @param lowestNecessaryOffset the lowest offset of all instructions marked
     *                              so far.
     */
    private boolean isStraddlingBranch(int branchOrigin,
                                       int branchTarget,
                                       int lowestNecessaryOffset)
    {
        return branchOrigin <= lowestNecessaryOffset ^
               branchTarget <= lowestNecessaryOffset;
    }


    /**
     * Marks the specified instruction if it is a required dup/swap instruction,
     * replacing it by an appropriate variant if necessary.
     * @param clazz         the class that is being checked.
     * @param codeAttribute the code that is being checked.
     * @param dupOffset     the offset of the dup/swap instruction.
     * @param instruction   the dup/swap instruction.
     * @return whether the instruction is updated.
     */
    private boolean fixDupInstruction(Clazz         clazz,
                                      CodeAttribute codeAttribute,
                                      int           dupOffset,
                                      Instruction   instruction)
    {
        byte    oldOpcode = instruction.opcode;
        byte    newOpcode = 0;
        boolean present   = false;

        // Simplify the popping instruction if possible.
        switch (oldOpcode)
        {
            case InstructionConstants.OP_DUP:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, 0, false);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, 1, false);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent1)
                {
                    present = true;

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
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, 0, false);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, 1, false);
                boolean stackEntryPresent2 = isStackEntryNecessaryAfter(dupOffset, 2, false);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent2)
                {
                    present = true;

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
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, 0, false);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, 1, false);
                boolean stackEntryPresent2 = isStackEntryNecessaryAfter(dupOffset, 2, false);
                boolean stackEntryPresent3 = isStackEntryNecessaryAfter(dupOffset, 3, false);

                // Should either the original element or the copy be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent3)
                {
                    present = true;

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
                            throw new IllegalArgumentException("Can't handle dup_x2 instruction moving original element across two elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP2:
            {
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, 0, 1, false);
                boolean stackEntriesPresent23 = isStackEntriesNecessaryAfter(dupOffset, 2, 3, false);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent23)
                {
                    present = true;

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
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, 0, 1, false);
                boolean stackEntryPresent2    = isStackEntryNecessaryAfter(dupOffset, 2, false);
                boolean stackEntriesPresent34 = isStackEntriesNecessaryAfter(dupOffset, 3, 4, false);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent34)
                {
                    present = true;

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
                            throw new IllegalArgumentException("Can't handle dup2_x1 instruction moving original element across "+skipCount+" elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_DUP2_X2:
            {
                boolean stackEntriesPresent01 = isStackEntriesNecessaryAfter(dupOffset, 0, 1, false);
                boolean stackEntryPresent2    = isStackEntryNecessaryAfter(dupOffset, 2, false);
                boolean stackEntryPresent3    = isStackEntryNecessaryAfter(dupOffset, 3, false);
                boolean stackEntriesPresent45 = isStackEntriesNecessaryAfter(dupOffset, 4, 5, false);

                // Should either the original element or the copy be present?
                if (stackEntriesPresent01 ||
                    stackEntriesPresent45)
                {
                    present = true;

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
                            throw new IllegalArgumentException("Can't handle dup2_x2 instruction moving original element across "+skipCount+" elements at ["+dupOffset +"]");
                        }
                    }
                }
                break;
            }
            case InstructionConstants.OP_SWAP:
            {
                boolean stackEntryPresent0 = isStackEntryNecessaryAfter(dupOffset, 0, false);
                boolean stackEntryPresent1 = isStackEntryNecessaryAfter(dupOffset, 1, false);

                // Will either element be present?
                if (stackEntryPresent0 ||
                    stackEntryPresent1)
                {
                    present = true;

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

        boolean updated = false;

        // Actually replace the instruction with the new opcode, if any.
        if (present)
        {
            // If this is the first pass, note that the instruction is updated.
            if (!isNecessary[dupOffset])
            {
                updated = true;

                // Mark that the instruction is necessary.
                isNecessary[dupOffset]  = true;
            }

            if      (newOpcode == 0)
            {
                // Delete the instruction.
                codeAttributeEditor.deleteInstruction(dupOffset);

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

        return updated;
    }


    /**
     * Pops the stack after the given necessary instruction, if it pushes an
     * entry that is not used at all.
     * @param clazz          the class that is being checked.
     * @param codeAttribute  the code that is being checked.
     * @param producerOffset the offset of the producer instruction.
     * @param instruction    the producer instruction.
     */
    private void fixPushInstruction(Clazz         clazz,
                                    CodeAttribute codeAttribute,
                                    int           producerOffset,
                                    Instruction   instruction)
    {
        int pushCount = instruction.stackPushCount(clazz);
        if (pushCount > 0)
        {
            boolean stackEntryPresent0 = isStackEntryNecessaryAfter(producerOffset, 0, false);

            if (!stackEntryPresent0)
            {
                if (instruction.opcode != InstructionConstants.OP_JSR &&
                    instruction.opcode != InstructionConstants.OP_JSR_W)
                {
                    // Make sure the pushed value is popped again,
                    // right after this instruction.
                    decreaseStackSize(producerOffset, pushCount, false, false);

                    if (DEBUG_ANALYSIS) System.out.println("  Popping unused value right after "+instruction.toString(producerOffset));
                }
            }
        }
    }


    /**
     * Pops the stack before the given unnecessary instruction, if the stack
     * contains an entry that it would have popped.
     * @param clazz          the class that is being checked.
     * @param codeAttribute  the code that is being checked.
     * @param consumerOffset the offset of the consumer instruction.
     * @param instruction    the consumer instruction.
     */
    private void fixPopInstruction(Clazz         clazz,
                                   CodeAttribute codeAttribute,
                                   int           consumerOffset,
                                   Instruction   instruction)
    {
        int popCount = instruction.stackPopCount(clazz);
        if (popCount > 0)
        {
            if (partialEvaluator.stackProducerOffsets(consumerOffset).contains(PartialEvaluator.AT_CATCH_ENTRY) ||
                (isStackEntryNecessaryBefore(consumerOffset, 0, false) &&
                 !isStackEntryNecessaryBefore(consumerOffset, 0, true)))
            {
                // Is the consumer a simple pop or pop2 instruction?
                if (isPop(instruction))
                {
                    if (DEBUG_ANALYSIS) System.out.println("  Popping value again at "+instruction.toString(consumerOffset));

                    // Simply mark the pop or pop2 instruction.
                    isNecessary[consumerOffset] = true;
                }
                else
                {
                    if (DEBUG_ANALYSIS) System.out.println("  Popping value instead of "+instruction.toString(consumerOffset));

                    // Make sure the pushed value is popped again,
                    // right before this instruction.
                    decreaseStackSize(consumerOffset, popCount, true, true);
                }
            }
        }
    }


    /**
     * Marks pop and pop2 instructions that pop stack entries produced by the
     * given instruction.
     * @param clazz               the class that is being checked.
     * @param codeAttribute       the code that is being checked.
     * @param producerOffset      the offset of the producer instruction.
     * @param producerInstruction the producer instruction.
     */
    private void markConsumingPopInstructions(Clazz         clazz,
                                              CodeAttribute codeAttribute,
                                              int           producerOffset,
                                              Instruction   producerInstruction)
    {
        // Loop over all pushed stack entries.
        int pushCount = producerInstruction.stackPushCount(clazz);
        for (int stackIndex = 0; stackIndex < pushCount; stackIndex++)
        {
            // Loop over all consumers of this entry.
            InstructionOffsetValue consumerOffsets =
                partialEvaluator.getStackAfter(producerOffset).getTopConsumerValue(stackIndex).instructionOffsetValue();

            int consumerCount = consumerOffsets.instructionOffsetCount();
            for (int consumerIndex = 0; consumerIndex < consumerCount; consumerIndex++)
            {
                int consumerOffset = consumerOffsets.instructionOffset(consumerIndex);

                // Is the consumer not necessary yet?
                if (!isNecessary[consumerOffset])
                {
                    Instruction consumerInstruction =
                        InstructionFactory.create(codeAttribute.code, consumerOffset);

                    // Is the consumer a simple pop or pop2 instruction?
                    if (isPop(consumerInstruction))
                    {
                        // Mark it.
                        isNecessary[consumerOffset] = true;

                        if (DEBUG_ANALYSIS) System.out.println("  Marking "+consumerInstruction.toString(consumerOffset)+" due to "+producerInstruction.toString(producerOffset));
                    }
                }
            }
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
        isNecessary[offset] = true;

        // Create a simple push instrucion.
        byte replacementOpcode =
            computationalType == Value.TYPE_INTEGER   ? InstructionConstants.OP_ICONST_0    :
            computationalType == Value.TYPE_LONG      ? InstructionConstants.OP_LCONST_0    :
            computationalType == Value.TYPE_FLOAT     ? InstructionConstants.OP_FCONST_0    :
            computationalType == Value.TYPE_DOUBLE    ? InstructionConstants.OP_DCONST_0    :
            computationalType == Value.TYPE_REFERENCE ? InstructionConstants.OP_ACONST_NULL :
            InstructionConstants.OP_NOP;

        Instruction replacementInstruction = new SimpleInstruction(replacementOpcode);

        // Insert the pop or push instruction.
        codeAttributeEditor.insertBeforeInstruction(offset,
                                                    replacementInstruction);

        if (extraAddedInstructionVisitor != null)
        {
            replacementInstruction.accept(null, null, null, offset, extraAddedInstructionVisitor);
        }

        // Delete the original instruction if necessary.
        if (delete)
        {
            codeAttributeEditor.deleteInstruction(offset);

            if (extraDeletedInstructionVisitor != null)
            {
                extraDeletedInstructionVisitor.visitSimpleInstruction(null, null, null, offset, null);
            }
        }
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
        isNecessary[offset] = true;

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
            throw new IllegalArgumentException("Unsupported stack size reduction ["+popCount+"]");
        }
    }


    // Implementations for InstructionVisitor.

    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        if (partialEvaluator.isTraced(offset))
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
                    replaceIntegerPushInstruction(offset);
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
                    replaceLongPushInstruction(offset);
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
                    replaceFloatPushInstruction(offset);
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
                    replaceDoublePushInstruction(offset);
                    break;

                case InstructionConstants.OP_AALOAD:
                    replaceReferencePushInstruction(offset);
                    break;
            }
        }
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        if (partialEvaluator.isTraced(offset))
        {
            switch (variableInstruction.opcode)
            {
                case InstructionConstants.OP_ILOAD:
                case InstructionConstants.OP_ILOAD_0:
                case InstructionConstants.OP_ILOAD_1:
                case InstructionConstants.OP_ILOAD_2:
                case InstructionConstants.OP_ILOAD_3:
                    replaceIntegerPushInstruction(offset);
                    break;

                case InstructionConstants.OP_LLOAD:
                case InstructionConstants.OP_LLOAD_0:
                case InstructionConstants.OP_LLOAD_1:
                case InstructionConstants.OP_LLOAD_2:
                case InstructionConstants.OP_LLOAD_3:
                    replaceLongPushInstruction(offset);
                    break;

                case InstructionConstants.OP_FLOAD:
                case InstructionConstants.OP_FLOAD_0:
                case InstructionConstants.OP_FLOAD_1:
                case InstructionConstants.OP_FLOAD_2:
                case InstructionConstants.OP_FLOAD_3:
                    replaceFloatPushInstruction(offset);
                    break;

                case InstructionConstants.OP_DLOAD:
                case InstructionConstants.OP_DLOAD_0:
                case InstructionConstants.OP_DLOAD_1:
                case InstructionConstants.OP_DLOAD_2:
                case InstructionConstants.OP_DLOAD_3:
                    replaceDoublePushInstruction(offset);
                    break;

                case InstructionConstants.OP_ALOAD:
                case InstructionConstants.OP_ALOAD_0:
                case InstructionConstants.OP_ALOAD_1:
                case InstructionConstants.OP_ALOAD_2:
                case InstructionConstants.OP_ALOAD_3:
                    replaceReferencePushInstruction(offset);
                    break;

            }
        }
    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        if (partialEvaluator.isTraced(offset))
        {
            switch (constantInstruction.opcode)
            {
                case InstructionConstants.OP_GETSTATIC:
                case InstructionConstants.OP_GETFIELD:
                    replaceAnyPushInstruction(offset);
                    break;

                case InstructionConstants.OP_INVOKEVIRTUAL:
                case InstructionConstants.OP_INVOKESPECIAL:
                case InstructionConstants.OP_INVOKESTATIC:
                case InstructionConstants.OP_INVOKEINTERFACE:
                    if (constantInstruction.stackPushCount(clazz) > 0 &&
                        !sideEffectInstructionChecker.hasSideEffects(clazz,
                                                                     method,
                                                                     codeAttribute,
                                                                     offset,
                                                                     constantInstruction))
                    {
                        replaceAnyPushInstruction(offset);
                    }
                    break;

                case InstructionConstants.OP_CHECKCAST:
                    replaceReferencePushInstruction(offset);
                    break;
            }
        }
    }


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        if (partialEvaluator.isTraced(offset))
        {
            switch (branchInstruction.opcode)
            {
                case InstructionConstants.OP_GOTO:
                case InstructionConstants.OP_GOTO_W:
                    // Don't replace unconditional branches.
                    break;

                case InstructionConstants.OP_JSR:
                case InstructionConstants.OP_JSR_W:
                    replaceJsrInstruction(offset, branchInstruction);
                    break;

                default:
                    replaceBranchInstruction(offset, branchInstruction);
                    break;
            }
        }
    }


    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        if (partialEvaluator.isTraced(offset))
        {
            // First try to simplify it to a simple branch.
            replaceBranchInstruction(offset, switchInstruction);

            // Otherwise make sure all branch targets are valid.
            if (!isSimplified[offset])
            {
                replaceSwitchInstruction(offset, switchInstruction);
            }
        }
    }


    // Small utility methods.

    /**
     * Replaces the push instruction at the given offset by a simpler push
     * instruction, if possible.
     */
    private void replaceAnyPushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            switch (pushedValue.computationalType())
            {
                case Value.TYPE_INTEGER:
                    replaceIntegerPushInstruction(offset);
                    break;
                case Value.TYPE_LONG:
                    replaceLongPushInstruction(offset);
                    break;
                case Value.TYPE_FLOAT:
                    replaceFloatPushInstruction(offset);
                    break;
                case Value.TYPE_DOUBLE:
                    replaceDoublePushInstruction(offset);
                    break;
                case Value.TYPE_REFERENCE:
                    replaceReferencePushInstruction(offset);
                    break;
            }
        }
    }


    /**
     * Replaces the integer pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceIntegerPushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            int value = pushedValue.integerValue().value();
            if (value << 16 >> 16 == value)
            {
                replacePushInstruction(offset,
                                       InstructionConstants.OP_SIPUSH,
                                       value);
            }
        }
    }


    /**
     * Replaces the long pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceLongPushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            long value = pushedValue.longValue().value();
            if (value == 0L ||
                value == 1L)
            {
                replacePushInstruction(offset,
                                       InstructionConstants.OP_LCONST_0,
                                       (int)value);
            }
        }
    }


    /**
     * Replaces the float pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceFloatPushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            float value = pushedValue.floatValue().value();
            if (value == 0f ||
                value == 1f ||
                value == 2f)
            {
                replacePushInstruction(offset,
                                       InstructionConstants.OP_FCONST_0,
                                       (int)value);
            }
        }
    }


    /**
     * Replaces the double pushing instruction at the given offset by a simpler
     * push instruction, if possible.
     */
    private void replaceDoublePushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            double value = pushedValue.doubleValue().value();
            if (value == 0.0 ||
                value == 1.0)
            {
                replacePushInstruction(offset,
                                       InstructionConstants.OP_DCONST_0,
                                       (int)value);
            }
        }
    }


    /**
     * Replaces the reference pushing instruction at the given offset by a
     * simpler push instruction, if possible.
     */
    private void replaceReferencePushInstruction(int offset)
    {
        Value pushedValue = partialEvaluator.getStackAfter(offset).getTop(0);
        if (pushedValue.isSpecific())
        {
            // A reference value can only be specific if it is null.
            replacePushInstruction(offset,
                                   InstructionConstants.OP_ACONST_NULL,
                                   0);
        }
    }


    /**
     * Replaces the instruction at a given offset by a given push instruction.
     */
    private void replacePushInstruction(int offset, byte opcode, int value)
    {
        // Remember the replacement instruction.
        Instruction replacementInstruction =
             new SimpleInstruction(opcode, value).shrink();

        if (DEBUG_ANALYSIS) System.out.println("  Replacing instruction at ["+offset+"] by "+replacementInstruction.toString());

        codeAttributeEditor.replaceInstruction(offset, replacementInstruction);

        // Mark that the instruction has been simplified.
        isSimplified[offset] = true;

        // Visit the instruction, if required.
        if (extraPushInstructionVisitor != null)
        {
            // Note: we're not passing the right arguments for now, knowing that
            // they aren't used anyway.
            extraPushInstructionVisitor.visitSimpleInstruction(null, null, null, offset, null);
        }
    }


    /**
     * Replaces the given 'jsr' instruction by a simpler branch instruction,
     * if possible.
     */
    private void replaceJsrInstruction(int offset, BranchInstruction branchInstruction)
    {
        // Is the subroutine ever returning?
        if (!partialEvaluator.isSubroutineReturning(offset + branchInstruction.branchOffset))
        {
            // All 'jsr' instructions to this subroutine can be replaced
            // by unconditional branch instructions.
            replaceBranchInstruction(offset, branchInstruction);
        }
        else if (!partialEvaluator.isTraced(offset + branchInstruction.length(offset)))
        {
            // We have to make sure the instruction after this 'jsr'
            // instruction is valid, even if it is never reached.
            insertInfiniteLoop(offset + branchInstruction.length(offset));
        }
    }


    /**
     * Deletes the given branch instruction, or replaces it by a simpler branch
     * instruction, if possible.
     */
    private void replaceBranchInstruction(int offset, Instruction instruction)
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
                if (DEBUG_ANALYSIS) System.out.println("  Deleting zero branch instruction at ["+offset+"]");

                // Delete the branch instruction.
                codeAttributeEditor.deleteInstruction(offset);
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
                isSimplified[offset] = true;

                // Visit the instruction, if required.
                if (extraBranchInstructionVisitor != null)
                {
                    // Note: we're not passing the right arguments for now,
                    // knowing that they aren't used anyway.
                    extraBranchInstructionVisitor.visitBranchInstruction(null, null, null, offset, null);
                }
            }
        }
    }


    /**
     * Makes sure all branch targets of the given switch instruction are valid.
     */
    private void replaceSwitchInstruction(int offset, SwitchInstruction switchInstruction)
    {
        // Get the actual branch targets.
        InstructionOffsetValue branchTargets = partialEvaluator.branchTargets(offset);
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
     * Puts an infinite loop at the given offset.
     */
    private void insertInfiniteLoop(int offset)
    {
        // Replace the branch instruction by a simple branch instruction.
        Instruction replacementInstruction =
            new BranchInstruction(InstructionConstants.OP_GOTO, 0);

        if (DEBUG_ANALYSIS) System.out.println("  Inserting infinite loop at unreachable instruction at ["+offset+"]");

        codeAttributeEditor.replaceInstruction(offset,
                                               replacementInstruction);

        // Mark that the instruction has been simplified.
        isNecessary[offset]  = true;
        isSimplified[offset] = true;
    }


    // Small utility methods.

    /**
     * Returns whether the given instruction is a dup or swap instruction
     * (dup, dup_x1, dup_x2, dup2, dup2_x1, dup2_x1, swap).
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
     * Initializes the necessary data structure.
     */
    private void initializeNecessary(CodeAttribute codeAttribute)
    {
        int codeLength = codeAttribute.u4codeLength;

        // Create new arrays for storing information at each instruction offset.
        if (isNecessary.length < codeLength)
        {
            isNecessary  = new boolean[codeLength];
            isSimplified = new boolean[codeLength];
        }
        else
        {
            for (int index = 0; index < codeLength; index++)
            {
                isNecessary[index]  = false;
                isSimplified[index] = false;
            }
        }
    }


    /**
     * Returns whether the given variable is ever referenced (stored) by an
     * instruction that is marked as necessary.
     */
    private boolean isVariableReferenced(CodeAttribute codeAttribute,
                                         int           startOffset,
                                         int           variableIndex)
    {
        int codeLength = codeAttribute.u4codeLength;

        for (int offset = startOffset; offset < codeLength; offset++)
        {
            if (isNecessary[offset]   &&
                !isSimplified[offset] &&
                isNecessary(partialEvaluator.getVariablesBefore(offset).getProducerValue(variableIndex),
                            true,
                            false))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether the given stack entry is present after execution of the
     * instruction at the given offset.
     */
    private boolean isStackEntriesNecessaryAfter(int instructionOffset, int stackIndex1, int stackIndex2, boolean all)
    {
        boolean present1 = isStackEntryNecessaryAfter(instructionOffset, stackIndex1, all);
        boolean present2 = isStackEntryNecessaryAfter(instructionOffset, stackIndex2, all);

//        if (present1 ^ present2)
//        {
//            throw new IllegalArgumentException("Can't handle partial use of dup2 instructions");
//        }

        return present1 || present2;
    }


    /**
     * Returns whether the given stack entry is present before execution of the
     * instruction at the given offset.
     */
    private boolean isStackEntryNecessaryBefore(int     instructionOffset,
                                                int     stackIndex,
                                                boolean all)
    {
        return isNecessary(partialEvaluator.getStackBefore(instructionOffset).getTopConsumerValue(stackIndex),
                           false,
                           all);
    }


    /**
     * Returns whether the given stack entry is present after execution of the
     * instruction at the given offset.
     */
    private boolean isStackEntryNecessaryAfter(int     instructionOffset,
                                               int     stackIndex,
                                               boolean all)
    {
        return isNecessary(partialEvaluator.getStackAfter(instructionOffset).getTopConsumerValue(stackIndex),
                           false,
                           all) ||
               partialEvaluator.getStackAfter(instructionOffset).getTopProducerValue(stackIndex).instructionOffsetValue().contains(PartialEvaluator.AT_METHOD_ENTRY);
    }


    /**
     * Returns whether any or all of the instructions at the given offsets are
     * marked as necessary.
     */
    private boolean isNecessary(Value   offsets,
                                boolean producer,
                                boolean all)
    {
        return offsets != null &&
               isNecessary(offsets.instructionOffsetValue(), producer, all);
    }


    /**
     * Returns whether any or all of the instructions at the given offsets are
     * marked as necessary.
     */
    private boolean isNecessary(InstructionOffsetValue offsets,
                                boolean                producer,
                                boolean                all)
    {
        int offsetCount = offsets.instructionOffsetCount();

        for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++)
        {
            int offset = offsets.instructionOffset(offsetIndex);

            if (offset != PartialEvaluator.AT_METHOD_ENTRY &&
                (all ^ (isNecessary[offset] && (producer || !isSimplified[offset]))))
            {
                return !all;
            }
        }

        return all;
    }
}
