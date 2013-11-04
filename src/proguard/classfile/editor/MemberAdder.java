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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This ConstantVisitor adds all class members that it visits to the given
 * target class.
 *
 * @author Eric Lafortune
 */
public class MemberAdder
extends      SimplifiedVisitor
implements   MemberVisitor
{
    private static final Attribute[] EMPTY_ATTRIBUTES = new Attribute[0];


    private final ProgramClass targetClass;
    private final boolean      copyAttributes;

    private final ConstantAdder constantAdder = new ConstantAdder();
    private final MembersEditor membersEditor = new MembersEditor();


    /**
     * Creates a new MemberAdder that will copy methods into the given target
     * class.
     */
    public MemberAdder(ProgramClass targetClass, boolean copyAttributes)
    {
        this.targetClass    = targetClass;
        this.copyAttributes = copyAttributes;

        constantAdder.setTargetClass(targetClass);
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        String name       = programField.getName(programClass);
        String descriptor = programField.getDescriptor(programClass);

        if (targetClass.findMethod(name, descriptor) == null)
        {
            ProgramField newProgramField = new ProgramField();

            // Copy the access flags.
            newProgramField.u2accessFlags = programField.u2accessFlags;

            // Make sure the name is set in the constant pool.
            programClass.constantPoolEntryAccept(programField.u2nameIndex,
                                                 constantAdder);

            newProgramField.u2nameIndex = constantAdder.getConstantIndex();

            // Make sure the descriptor is set in the constant pool.
            programClass.constantPoolEntryAccept(programField.u2descriptorIndex,
                                                 constantAdder);

            newProgramField.u2descriptorIndex = constantAdder.getConstantIndex();

            // Copy the attributes if requested.
            if (copyAttributes)
            {
                programField.attributesAccept(programClass,
                                              new AttributeAdder(targetClass,
                                                                 newProgramField));
            }

            // Actually add the completed field.
            membersEditor.addField(targetClass, newProgramField);
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        String name       = programMethod.getName(programClass);
        String descriptor = programMethod.getDescriptor(programClass);

        if (targetClass.findMethod(name, descriptor) == null)
        {
            ProgramMethod newProgramMethod = new ProgramMethod();

            // Copy the access flags.
            newProgramMethod.u2accessFlags = programMethod.u2accessFlags;

            // Make sure the name is set in the constant pool.
            programClass.constantPoolEntryAccept(programMethod.u2nameIndex,
                                                 constantAdder);

            newProgramMethod.u2nameIndex = constantAdder.getConstantIndex();

            // Make sure the descriptor is set in the constant pool.
            programClass.constantPoolEntryAccept(programMethod.u2descriptorIndex,
                                                 constantAdder);

            newProgramMethod.u2descriptorIndex = constantAdder.getConstantIndex();

            // Start with an empty list of attributes.
            newProgramMethod.u2attributesCount = 0;
            newProgramMethod.attributes        = EMPTY_ATTRIBUTES;

            // Copy the attributes if requested.
            if (copyAttributes)
            {
                programMethod.attributesAccept(programClass,
                                               new AttributeAdder(targetClass,
                                                                  newProgramMethod));
            }

            // Actually add the completed method.
            membersEditor.addMethod(targetClass, newProgramMethod);
        }
    }
}
