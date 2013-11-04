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
 * This InstructionVisitor deletes load/store instruction pairs.
 *
 * @author Eric Lafortune
 */
public class LoadStoreRemover
extends      SimplifiedVisitor
implements   InstructionVisitor
{
    private final BranchTargetFinder  branchTargetFinder;
    private final CodeAttributeEditor codeAttributeEditor;
    private final InstructionVisitor  extraInstructionVisitor;


    /**
     * Creates a new LoadStoreRemover.
     * @param branchTargetFinder      a branch target finder that has been
     *                                initialized to indicate branch targets
     *                                in the visited code.
     * @param codeAttributeEditor     a code editor that can be used for
     *                                accumulating changes to the code.
     */
    public LoadStoreRemover(BranchTargetFinder  branchTargetFinder,
                            CodeAttributeEditor codeAttributeEditor)
    {
        this(branchTargetFinder, codeAttributeEditor, null);
    }


    /**
     * Creates a new LoadStoreRemover.
     * @param branchTargetFinder      a branch target finder that has been
     *                                initialized to indicate branch targets
     *                                in the visited code.
     * @param codeAttributeEditor     a code editor that can be used for
     *                                accumulating changes to the code.
     * @param extraInstructionVisitor an optional extra visitor for all deleted
     *                                load instructions.
     */
    public LoadStoreRemover(BranchTargetFinder  branchTargetFinder,
                            CodeAttributeEditor codeAttributeEditor,
                            InstructionVisitor  extraInstructionVisitor)
    {
        this.branchTargetFinder      = branchTargetFinder;
        this.codeAttributeEditor     = codeAttributeEditor;
        this.extraInstructionVisitor = extraInstructionVisitor;
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        // Is this instruction a load instruction?
        if (variableInstruction.isLoad() &&
            variableInstruction.opcode != InstructionConstants.OP_RET)
        {
            int variableIndex = variableInstruction.variableIndex;

            int nextOffset = offset + variableInstruction.length(offset);

            if (!codeAttributeEditor.isModified(offset)     &&
                !codeAttributeEditor.isModified(nextOffset) &&
                !branchTargetFinder.isTarget(nextOffset))
            {
                // Is the next instruction a corresponding store instruction?
                Instruction nextInstruction = InstructionFactory.create(codeAttribute.code,
                                                                        nextOffset);

                if (nextInstruction instanceof VariableInstruction)
                {
                    variableInstruction = (VariableInstruction)nextInstruction;
                    if (!variableInstruction.isLoad()                              &&
                        variableInstruction.opcode != InstructionConstants.OP_IINC &&
                        variableInstruction.variableIndex == variableIndex)
                    {
                        // Delete both instructions.
                        codeAttributeEditor.deleteInstruction(offset);
                        codeAttributeEditor.deleteInstruction(nextOffset);

                        // Visit the instruction, if required.
                        if (extraInstructionVisitor != null)
                        {
                            extraInstructionVisitor.visitVariableInstruction(clazz, method, codeAttribute, offset, variableInstruction);
                        }
                    }
                }
            }
        }
    }
}
