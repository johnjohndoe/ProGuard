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
package proguard.shrink;

import proguard.classfile.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.ClassVisitor;


/**
 * This ClassVisitor recursively marks all interface
 * classes that are being used in the visited class.
 *
 * @see UsageMarker
 *
 * @author Eric Lafortune
 */
public class InterfaceUsageMarker
extends      SimplifiedVisitor
implements   ClassVisitor,
             ConstantVisitor
{
    private final UsageMarker usageMarker;

    // A field acting as a return parameter for several methods.
    private boolean used;


    /**
     * Creates a new InterfaceUsageMarker.
     * @param usageMarker the usage marker that is used to mark the classes
     *                    and class members.
     */
    public InterfaceUsageMarker(UsageMarker usageMarker)
    {
        this.usageMarker = usageMarker;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        boolean classUsed         = usageMarker.isUsed(programClass);
        boolean classPossiblyUsed = usageMarker.isPossiblyUsed(programClass);

        if (classUsed || classPossiblyUsed)
        {
            // Mark the references to interfaces that are being used.
            for (int index = 0; index < programClass.u2interfacesCount; index++)
            {
                // Check if the interface is used. Mark the constant pool entry
                // if so.
                markConstant(programClass, programClass.u2interfaces[index]);
                classUsed |= used;
            }

            // Is this an interface with a preliminary mark?
            if (classPossiblyUsed)
            {
                // Should it be included now?
                if (classUsed)
                {
                    // At least one if this interface's interfaces is being used.
                    // Mark this interface as well.
                    usageMarker.markAsUsed(programClass);

                    // Mark this interface's name.
                    markConstant(programClass, programClass.u2thisClass);

                    // Mark the superclass (java/lang/Object).
                    if (programClass.u2superClass != 0)
                    {
                        markConstant(programClass, programClass.u2superClass);
                    }
                }
                else
                {
                    // Unmark this interface, so we don't bother looking at it again.
                    usageMarker.markAsUnused(programClass);
                }
            }
        }

        // The return value.
        used = classUsed;
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // The return value.
        used = true;
    }


    // Implementations for ConstantVisitor.

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        boolean classUsed = usageMarker.isUsed(classConstant);

        if (!classUsed)
        {
            // The ClassConstant isn't marked as being used yet. But maybe it should
            // be included as an interface, so check the actual class.
            classConstant.referencedClassAccept(this);
            classUsed =   used;

            if (classUsed)
            {
                // The class is being used. Mark the ClassConstant as being used
                // as well.
                usageMarker.markAsUsed(classConstant);

                markConstant(clazz, classConstant.u2nameIndex);
            }
        }

        // The return value.
        used = classUsed;
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        if (!usageMarker.isUsed(utf8Constant))
        {
            usageMarker.markAsUsed(utf8Constant);
        }
    }


    // Small utility methods.

    /**
     * Marks the given constant pool entry of the given class. This includes
     * visiting any referenced objects.
     */
    private void markConstant(Clazz clazz, int index)
    {
         clazz.constantPoolEntryAccept(index, this);
    }
}
