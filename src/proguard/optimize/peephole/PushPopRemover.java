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
package proguard.optimize.peephole;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This InstructionVisitor deletes all push/pop instruction pairs. In this
 * context, push instructions are instructions that push values onto the stack,
 * like dup and load instructions.
 *
 * @author Eric Lafortune
 */
public class PushPopRemover
extends      SimplifiedVisitor
implements   InstructionVisitor
{
    private final BranchTargetFinder  branchTargetFinder;
    private final CodeAttributeEditor codeAttributeEditor;
    private final InstructionVisitor  extraInstructionVisitor;


    /**
     * Creates a new PushPopRemover.
     * @param branchTargetFinder    a branch target finder that has been
     *                              initialized to indicate branch targets
     *                              in the visited code.
     * @param codeAttributeEditor   a code editor that can be used for
     *                              accumulating changes to the code.
     */
    public PushPopRemover(BranchTargetFinder  branchTargetFinder,
                          CodeAttributeEditor codeAttributeEditor)
    {
        this(branchTargetFinder, codeAttributeEditor, null);
    }


    /**
     * Creates a new PushPopRemover.
     * @param branchTargetFinder      a branch target finder that has been
     *                                initialized to indicate branch targets
     *                                in the visited code.
     * @param codeAttributeEditor     a code editor that can be used for
     *                                accumulating changes to the code.
     * @param extraInstructionVisitor an optional extra visitor for all deleted
     *                                push instructions.
     */
    public PushPopRemover(BranchTargetFinder  branchTargetFinder,
                          CodeAttributeEditor codeAttributeEditor,
                          InstructionVisitor  extraInstructionVisitor)
    {
        this.branchTargetFinder      = branchTargetFinder;
        this.codeAttributeEditor     = codeAttributeEditor;
        this.extraInstructionVisitor = extraInstructionVisitor;
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        switch (simpleInstruction.opcode)
        {
            case InstructionConstants.OP_ICONST_M1:
            case InstructionConstants.OP_ICONST_0:
            case InstructionConstants.OP_ICONST_1:
            case InstructionConstants.OP_ICONST_2:
            case InstructionConstants.OP_ICONST_3:
            case InstructionConstants.OP_ICONST_4:
            case InstructionConstants.OP_ICONST_5:
            case InstructionConstants.OP_LCONST_0:
            case InstructionConstants.OP_LCONST_1:
            case InstructionConstants.OP_FCONST_0:
            case InstructionConstants.OP_FCONST_1:
            case InstructionConstants.OP_FCONST_2:
            case InstructionConstants.OP_DCONST_0:
            case InstructionConstants.OP_DCONST_1:

            case InstructionConstants.OP_DUP:
            case InstructionConstants.OP_DUP2:
            case InstructionConstants.OP_BIPUSH:
            case InstructionConstants.OP_SIPUSH:
            case InstructionConstants.OP_LDC:
            case InstructionConstants.OP_LDC_W:
            case InstructionConstants.OP_LDC2_W:
                // All these simple instructions are pushing instructions.
                deleteWithSubsequentPop(clazz, method, codeAttribute, offset, simpleInstruction);
                break;
        }
    }

    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        if (variableInstruction.isLoad() &&
            variableInstruction.opcode != InstructionConstants.OP_RET)
        {
            // All load instructions are pushing instructions.
            deleteWithSubsequentPop(clazz, method, codeAttribute, offset, variableInstruction);
        }
    }


    // Small utility methods.

    /**
     * Deletes the given instruction and its subsequent compatible pop instruction,
     * if any, and if the latter is not a branch target.
     */
    private void deleteWithSubsequentPop(Clazz         clazz,
                                         Method        method,
                                         CodeAttribute codeAttribute,
                                         int           offset,
                                         Instruction   instruction)
    {
        boolean isCategory2 = instruction.isCategory2();

        int nextOffset = offset + instruction.length(offset);

        if (!codeAttributeEditor.isModified(offset)     &&
            !codeAttributeEditor.isModified(nextOffset) &&
            !branchTargetFinder.isTarget(nextOffset))
        {
            Instruction nextInstruction = InstructionFactory.create(codeAttribute.code,
                                                                    nextOffset);
            int nextOpcode = nextInstruction.opcode;
            if ((nextOpcode == InstructionConstants.OP_POP ||
                 nextOpcode == InstructionConstants.OP_POP2) &&
                                                             nextInstruction.isCategory2() == isCategory2)
            {
                // Delete the pushing instruction and the pop instruction.
                codeAttributeEditor.deleteInstruction(offset);
                codeAttributeEditor.deleteInstruction(nextOffset);

                // Visit the instruction, if required.
                if (extraInstructionVisitor != null)
                {
                    instruction.accept(clazz, method, codeAttribute, offset, extraInstructionVisitor);
                }
            }
        }
    }
}
