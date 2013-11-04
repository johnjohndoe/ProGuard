/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This ConstantVisitor adds all constants that it visits to the constant pool
 * of a given target class.
 *
 * @author Eric Lafortune
 */
public class ConstantAdder
implements   ConstantVisitor
{
    private ProgramClass targetClass;
    private int          constantIndex;

    private final ConstantPoolEditor constantPoolEditor = new ConstantPoolEditor();


    /**
     * Sets the class to which visited constants will be copied.
     */
    public void setTargetClass(ProgramClass targetClass)
    {
        this.targetClass = targetClass;
    }


    /**
     * Returns the class to which visited constants will be copied.
     */
    public ProgramClass getTargetClass()
    {
        return targetClass;
    }


    /**
     * Returns the index of the most recently created constant in the constant
     * pool of the target class.
     */
    public int getConstantIndex()
    {
        return constantIndex;
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        constantIndex =
            constantPoolEditor.addIntegerConstant(targetClass,
                                                  integerConstant.getValue());
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        constantIndex =
            constantPoolEditor.addLongConstant(targetClass,
                                               longConstant.getValue());
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        constantIndex =
            constantPoolEditor.addFloatConstant(targetClass,
                                                floatConstant.getValue());
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        constantIndex =
            constantPoolEditor.addDoubleConstant(targetClass,
                                                 doubleConstant.getValue());
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        constantIndex =
            constantPoolEditor.addStringConstant(targetClass,
                                                 stringConstant.getString(clazz),
                                                 stringConstant.referencedClass,
                                                 stringConstant.referencedMember);
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        constantIndex =
            constantPoolEditor.addUtf8Constant(targetClass,
                                               utf8Constant.getString());
    }


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        // First add the referenced class constant, with its own referenced class.
        clazz.constantPoolEntryAccept(fieldrefConstant.u2classIndex, this);

        // Then add the actual field reference constant, with its referenced
        // class and class member.
        constantIndex =
            constantPoolEditor.addFieldrefConstant(targetClass,
                                                   constantIndex,
                                                   fieldrefConstant.getName(clazz),
                                                   fieldrefConstant.getType(clazz),
                                                   fieldrefConstant.referencedClass,
                                                   fieldrefConstant.referencedMember);
    }


    public void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        // First add the referenced class constant, with its own referenced class.
        clazz.constantPoolEntryAccept(interfaceMethodrefConstant.u2classIndex, this);

        // Then add the actual interface method reference constant, with its
        // referenced class and class member.
        constantIndex =
            constantPoolEditor.addInterfaceMethodrefConstant(targetClass,
                                                             constantIndex,
                                                             interfaceMethodrefConstant.getName(clazz),
                                                             interfaceMethodrefConstant.getType(clazz),
                                                             interfaceMethodrefConstant.referencedClass,
                                                             interfaceMethodrefConstant.referencedMember);
    }


    public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant)
    {
        // First add the referenced class constant, with its own referenced class.
        clazz.constantPoolEntryAccept(methodrefConstant.u2classIndex, this);

        // Then add the actual method reference constant, with its referenced
        // class and class member.
        constantIndex =
            constantPoolEditor.addMethodrefConstant(targetClass,
                                                    constantIndex,
                                                    methodrefConstant.getName(clazz),
                                                    methodrefConstant.getType(clazz),
                                                    methodrefConstant.referencedClass,
                                                    methodrefConstant.referencedMember);
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Add the class constant, with its referenced class..
        constantIndex =
            constantPoolEditor.addClassConstant(targetClass,
                                                classConstant.getName(clazz),
                                                classConstant.referencedClass);
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        constantIndex =
            constantPoolEditor.addNameAndTypeConstant(targetClass,
                                                      nameAndTypeConstant.getName(clazz),
                                                      nameAndTypeConstant.getType(clazz));
    }
}
