/* $Id: GenericInstruction.java,v 1.1 2002/07/04 16:16:58 eric Exp $
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
package proguard.classfile.instruction;

import proguard.classfile.*;

import java.io.*;
import java.util.*;


/**
 * Base class for representing instructions.
 *
 * @author Eric Lafortune
 */
public class GenericInstruction
  implements Instruction
{
    // Shared copies of instruction objects, to avoid creating a lot of objects.
    private static final GenericInstruction genericInstruction = new GenericInstruction();
    private static final ByteCpInstruction  byteCpInstruction  = new ByteCpInstruction();
    private static final ShortCpInstruction shortCpInstruction = new ShortCpInstruction();

    protected byte[]  code;
    protected int     offset;
    protected int     opcode;
    protected boolean wide;
    protected int     length;


    /**
     * Creates a new Instruction from the data in the byte array, starting
     * at the given index.
     */
    public static GenericInstruction create(byte[] code, int offset)
    {
        // We'll re-use one single instruction.
        //Instruction instruction = new Instruction();

        int value            = 0;
        int variableIndex    = -1;
        int byteCpIndex      = -1;
        int shortCpIndex     = -1;
        int branchOffset     = 0;
        int switchDefault    = 0;
        int switchLow        = 0;
        int switchHigh       = 0;
        int switchCaseCount  = 0;

        int index  = offset;
        int opcode = code[index++];

        boolean wide = false;
        if (opcode == OP_WIDE)
        {
            opcode = code[index++];
            wide   = true;
        }

        switch (opcode)
        {
        // Instructions without additional operands.
        case OP_NOP:
        case OP_ACONST_NULL:
        case OP_ICONST_M1:
        case OP_ICONST_0:
        case OP_ICONST_1:
        case OP_ICONST_2:
        case OP_ICONST_3:
        case OP_ICONST_4:
        case OP_ICONST_5:
        case OP_LCONST_0:
        case OP_LCONST_1:
        case OP_FCONST_0:
        case OP_FCONST_1:
        case OP_FCONST_2:
        case OP_DCONST_0:
        case OP_DCONST_1:

        case OP_ILOAD_0:
        case OP_ILOAD_1:
        case OP_ILOAD_2:
        case OP_ILOAD_3:
        case OP_LLOAD_0:
        case OP_LLOAD_1:
        case OP_LLOAD_2:
        case OP_LLOAD_3:
        case OP_FLOAD_0:
        case OP_FLOAD_1:
        case OP_FLOAD_2:
        case OP_FLOAD_3:
        case OP_DLOAD_0:
        case OP_DLOAD_1:
        case OP_DLOAD_2:
        case OP_DLOAD_3:
        case OP_ALOAD_0:
        case OP_ALOAD_1:
        case OP_ALOAD_2:
        case OP_ALOAD_3:
        case OP_IALOAD:
        case OP_LALOAD:
        case OP_FALOAD:
        case OP_DALOAD:
        case OP_AALOAD:
        case OP_BALOAD:
        case OP_CALOAD:
        case OP_SALOAD:

        case OP_ISTORE_0:
        case OP_ISTORE_1:
        case OP_ISTORE_2:
        case OP_ISTORE_3:
        case OP_LSTORE_0:
        case OP_LSTORE_1:
        case OP_LSTORE_2:
        case OP_LSTORE_3:
        case OP_FSTORE_0:
        case OP_FSTORE_1:
        case OP_FSTORE_2:
        case OP_FSTORE_3:
        case OP_DSTORE_0:
        case OP_DSTORE_1:
        case OP_DSTORE_2:
        case OP_DSTORE_3:
        case OP_ASTORE_0:
        case OP_ASTORE_1:
        case OP_ASTORE_2:
        case OP_ASTORE_3:
        case OP_IASTORE:
        case OP_LASTORE:
        case OP_FASTORE:
        case OP_DASTORE:
        case OP_AASTORE:
        case OP_BASTORE:
        case OP_CASTORE:
        case OP_SASTORE:
        case OP_POP:
        case OP_POP2:
        case OP_DUP:
        case OP_DUP_X1:
        case OP_DUP_X2:
        case OP_DUP2:
        case OP_DUP2_X1:
        case OP_DUP2_X2:
        case OP_SWAP:
        case OP_IADD:
        case OP_LADD:
        case OP_FADD:
        case OP_DADD:
        case OP_ISUB:
        case OP_LSUB:
        case OP_FSUB:
        case OP_DSUB:
        case OP_IMUL:
        case OP_LMUL:
        case OP_FMUL:
        case OP_DMUL:
        case OP_IDIV:
        case OP_FDIV:
        case OP_LDIV:
        case OP_DDIV:
        case OP_IREM:
        case OP_LREM:
        case OP_FREM:
        case OP_DREM:
        case OP_INEG:
        case OP_LNEG:
        case OP_FNEG:
        case OP_DNEG:
        case OP_ISHL:
        case OP_LSHL:
        case OP_ISHR:
        case OP_LSHR:
        case OP_IUSHR:
        case OP_LUSHR:
        case OP_IAND:
        case OP_LAND:
        case OP_IOR:
        case OP_LOR:
        case OP_IXOR:
        case OP_LXOR:

        case OP_I2L:
        case OP_I2F:
        case OP_I2D:
        case OP_L2I:
        case OP_L2F:
        case OP_L2D:
        case OP_F2I:
        case OP_F2L:
        case OP_F2D:
        case OP_D2I:
        case OP_D2L:
        case OP_D2F:
        case OP_I2B:
        case OP_I2C:
        case OP_I2S:
        case OP_LCMP:
        case OP_FCMPL:
        case OP_FCMPG:
        case OP_DCMPL:
        case OP_DCMPG:

        case OP_IRETURN:
        case OP_LRETURN:
        case OP_FRETURN:
        case OP_DRETURN:
        case OP_ARETURN:
        case OP_RETURN:

        case OP_ARRAYLENGTH:
        case OP_ATHROW:

        case OP_MONITORENTER:
        case OP_MONITOREXIT:
            break;

        // Instructions with one signed constant byte operand.
        case OP_BIPUSH:
        case OP_NEWARRAY:
            value = code[index++];
            break;

        // Instructions with one signed constant double-byte operand.
        case OP_SIPUSH:
            value = (code[index++] << 8) | (code[index++] & 0xff);
            break;

        // Instructions with a contant pool index.
        case OP_LDC:
            byteCpIndex = code[index++] & 0xff;
            break;

        // Instructions with a wide contant pool index.
        case OP_LDC_WIDE:
        case OP_LDC2_WIDE:

        case OP_GETSTATIC:
        case OP_PUTSTATIC:
        case OP_GETFIELD:
        case OP_PUTFIELD:

        case OP_INVOKEVIRTUAL:
        case OP_INVOKESPECIAL:
        case OP_INVOKESTATIC:

        case OP_NEW:
        case OP_ANEWARRAY:
        case OP_CHECKCAST:
        case OP_INSTANCEOF:
            shortCpIndex = ((code[index++] & 0xff) << 8) | (code[index++] & 0xff);
            break;

        // The multinewarray instruction.
        case OP_MULTINEWARRAY:
            shortCpIndex = ((code[index++] & 0xff) << 8) | (code[index++] & 0xff);
            value        = code[index++] & 0xff;
            break;

        // The invokeinterface instruction.
        case OP_INVOKEINTERFACE:
            shortCpIndex = ((code[index++] & 0xff) << 8) | (code[index++] & 0xff);
            value        = code[index++] & 0xff;
            index++;
            break;

        // Instructions with one local variable operand.
        case OP_ILOAD:
        case OP_LLOAD:
        case OP_FLOAD:
        case OP_DLOAD:
        case OP_ALOAD:

        case OP_ISTORE:
        case OP_LSTORE:
        case OP_FSTORE:
        case OP_DSTORE:
        case OP_ASTORE:

        case OP_RET:
            variableIndex = wide ?
                (((code[index++] & 0xff) << 8) | (code[index++] & 0xff)) :
                (                                 code[index++] & 0xff );
            break;

        // Instructions with one local variable operand and one constant byte operand.
        case OP_IINC:
            variableIndex = wide ?
                (((code[index++] & 0xff) << 8) | (code[index++] & 0xff)) :
                (                                 code[index++] & 0xff );
            value = wide ?
                ((code[index++] << 8) | (code[index++] & 0xff)) :
                (                        code[index++]        );
            break;

        // Instructions with one branch offset operand.
        case OP_IFEQ:
        case OP_IFNE:
        case OP_IFLT:
        case OP_IFGE:
        case OP_IFGT:
        case OP_IFLE:
        case OP_IFICMPEQ:
        case OP_IFICMPNE:
        case OP_IFICMPLT:
        case OP_IFICMPGE:
        case OP_IFICMPGT:
        case OP_IFICMPLE:
        case OP_IFACMPEQ:
        case OP_IFACMPNE:
        case OP_GOTO:
        case OP_JSR:

        case OP_IFNULL:
        case OP_IFNONNULL:
            branchOffset = (code[index++] << 8) | (code[index++] & 0xff);
            break;

        // Instructions with one wide branch offset operand.
        case OP_GOTO_W:
        case OP_JSR_WIDE:
            branchOffset = ((code[index++] & 0xff) << 24) |
                           ((code[index++] & 0xff) << 16) |
                           ((code[index++] & 0xff) <<  8) |
                           ( code[index++] & 0xff       );
            break;

        //  The tableswitch instruction.
        case OP_TABLESWITCH:
            // Up to three padding bytes.
            index = (index+3) & ~3;

            // Read three 32-bit arguments.
            switchDefault = ((code[index++] & 0xff) << 24) |
                            ((code[index++] & 0xff) << 16) |
                            ((code[index++] & 0xff) <<  8) |
                            ( code[index++] & 0xff       );
            switchLow     = ((code[index++] & 0xff) << 24) |
                            ((code[index++] & 0xff) << 16) |
                            ((code[index++] & 0xff) <<  8) |
                            ( code[index++] & 0xff       );
            switchHigh    = ((code[index++] & 0xff) << 24) |
                            ((code[index++] & 0xff) << 16) |
                            ((code[index++] & 0xff) <<  8) |
                            ( code[index++] & 0xff       );

            // We'll skip the other 32-but arguments for now.
            index += (switchHigh - switchLow + 1) * 4;
            break;

        //  The lookupswitch instruction.
        case OP_LOOKUPSWITCH:
            // Up to three padding bytes.
            index = (index+3) & ~3;

            // Read two 32-bit arguments.
            switchDefault    = ((code[index++] & 0xff) << 24) |
                               ((code[index++] & 0xff) << 16) |
                               ((code[index++] & 0xff) <<  8) |
                               ( code[index++] & 0xff       );
            switchCaseCount  = ((code[index++] & 0xff) << 24) |
                               ((code[index++] & 0xff) << 16) |
                               ((code[index++] & 0xff) <<  8) |
                               ( code[index++] & 0xff       );

            // We'll skip the other 2*32-bit arguments for now.
            index += switchCaseCount * 8;
            break;

        default:
            throw new IllegalArgumentException("Unknown instruction ["+opcode+"] at offset "+offset);
        }


        GenericInstruction instruction =
            byteCpIndex  != -1 ? byteCpInstruction  :
            shortCpIndex != -1 ? shortCpInstruction :
                                 genericInstruction;

        //instruction.readInfo(code, offset);
        instruction.code   = code;
        instruction.offset = offset;
        instruction.opcode = opcode;
        instruction.wide   = wide;
        instruction.length = index - offset;

        return instruction;
    }

    public int getOffset()
    {
        return offset;
    }

    public int getOpcode()
    {
        return opcode;
    }

    public boolean isWide()
    {
        return wide;
    }

    public int getLength()
    {
        return length;
    }


    public void accept(ClassFile classFile, InstructionVisitor instructionVisitor)
    {
        instructionVisitor.visitInstruction(classFile, this);
    }
}
