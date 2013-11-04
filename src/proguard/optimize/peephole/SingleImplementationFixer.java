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
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.*;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.optimize.info.SingleImplementationMarker;

/**
 * This ClassVisitor cleans up after the SingleImplementationInliner.
 * It fixes the names of interfaces that have single implementations, lets
 * the implementations and fields references point to them again. This is
 * necessary after the SingleImplementationInliner has overzealously renamed
 * the interfaces to the single implementations, let the single implementations
 * point to themselves as interfaces, and let the field references point to the
 * single implementations.
 *
 * @see SingleImplementationInliner
 * @see ClassReferenceFixer
 * @author Eric Lafortune
 */
public class SingleImplementationFixer
extends      SimplifiedVisitor
implements   ClassVisitor,
             ConstantVisitor
{
    private final ConstantPoolEditor constantPoolEditor = new ConstantPoolEditor();


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Is this an interface with a single implementation?
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(programClass);

        if (singleImplementationClass != null)
        {
            // Fix the reference to its own name.
            fixThisClassReference(programClass);

            // Fix the reference from its single interface or implementation.
            fixInterfaceReference((ProgramClass)programClass.subClasses[0],
                                  programClass);
        }

        // Fix the field references in the constant pool.
        programClass.constantPoolEntriesAccept(this);
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        // Update the referenced class if it is an interface with a single
        // implementation.
        Clazz singleImplementationClass =
            SingleImplementationMarker.singleImplementation(fieldrefConstant.referencedClass);

        if (singleImplementationClass != null)
        {
            // Fix the reference to the interface.
            fixFieldrefClassReference((ProgramClass)clazz,
                                      fieldrefConstant);
        }
    }


    // Small utility methods.

    /**
     * Fixes the given class, so its name points to itself again.
     */
    private void fixThisClassReference(ProgramClass programClass)
    {
        // We have to add a new class entry to avoid an existing entry with the
        // same name being reused. The names have to be fixed later, based on
        // their referenced classes.
        int nameIndex =
            constantPoolEditor.addUtf8Constant(programClass,
                                               programClass.getName());
        programClass.u2thisClass =
            constantPoolEditor.addConstant(programClass,
                                           new ClassConstant(nameIndex,
                                                             programClass));
    }


    /**
     * Fixes the given class, so it points to the given interface again.
     */
    private void fixInterfaceReference(ProgramClass programClass,
                                       ProgramClass interfaceClass)
    {
        // Make sure the class refers to the given interface again.
        String interfaceName = interfaceClass.getName();

        int interfacesCount = programClass.u2interfacesCount;
        for (int index = 0; index < interfacesCount; index++)
        {
            if (interfaceName.equals(programClass.getInterfaceName(index)))
            {
                // Update the class index.
                // We have to add a new class entry to avoid an existing entry
                // with the same name being reused. The names have to be fixed
                // later, based on their referenced classes.
                int nameIndex =
                    constantPoolEditor.addUtf8Constant(programClass,
                                                       interfaceName);
                programClass.u2interfaces[index]       =
                    constantPoolEditor.addConstant(programClass,
                                                   new ClassConstant(nameIndex,
                                                                     interfaceClass));
                break;

            }
        }
    }


    /**
     * Fixes the given field reference, so its class index points to its
     * class again. Note that this could be a different class than the one
     * in the original class.
     */
    private void fixFieldrefClassReference(ProgramClass     programClass,
                                           FieldrefConstant fieldrefConstant)
    {
        Clazz referencedClass = fieldrefConstant.referencedClass;

        // We have to add a new class entry to avoid an existing entry with the
        // same name being reused. The names have to be fixed later, based on
        // their referenced classes.
        int nameIndex =
            constantPoolEditor.addUtf8Constant(programClass,
                                               fieldrefConstant.getClassName(programClass));
        fieldrefConstant.u2classIndex =
            constantPoolEditor.addConstant(programClass,
                                           new ClassConstant(nameIndex,
                                                             referencedClass));
    }
}
