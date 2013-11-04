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
import proguard.classfile.constant.Constant;
import proguard.classfile.visitor.ClassVisitor;

import java.util.Arrays;

/**
 * This ClassVisitor sorts the constant pool entries of the classes that
 * it visits. The sorting order is based on the types of the constant pool
 * entries in the first place, and on their contents in the second place.
 *
 * @author Eric Lafortune
 */
public class ConstantPoolSorter implements ClassVisitor
{
    private int[]                constantIndexMap       = new int[ClassConstants.TYPICAL_CONSTANT_POOL_SIZE];
    private ComparableConstant[] comparableConstantPool = new ComparableConstant[ClassConstants.TYPICAL_CONSTANT_POOL_SIZE];

    private final ConstantPoolRemapper constantPoolRemapper = new ConstantPoolRemapper();


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Sort the constant pool and set up an index map.
        sortConstantPool(programClass,
                         programClass.constantPool,
                         programClass.u2constantPoolCount);

        // Remap all constant pool references.
        constantPoolRemapper.setConstantIndexMap(constantIndexMap);
        constantPoolRemapper.visitProgramClass(programClass);
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
    }


    // Small utility methods.

    /**
     * Sorts the given constant pool.
     */
    private void sortConstantPool(Clazz clazz, Constant[] constantPool, int length)
    {
        if (constantIndexMap.length < length)
        {
            constantIndexMap       = new int[length];
            comparableConstantPool = new ComparableConstant[length];
        }

        // Initialize an array whose elements can be compared.
        for (int oldIndex = 1; oldIndex < length; oldIndex++)
        {
            Constant constant = constantPool[oldIndex];

            // Long entries take up two entries, the second of which is null.
            if (constant == null)
            {
                constant = constantPool[oldIndex-1];
            }

            comparableConstantPool[oldIndex] = new ComparableConstant(clazz,
                                                                      oldIndex,
                                                                      constant);
        }

        // Sort the array.
        Arrays.sort(comparableConstantPool, 1, length);

        // Save the sorted elements.
        Constant previousConstant = null;
        for (int newIndex = 1; newIndex < length; newIndex++)
        {
            ComparableConstant comparableConstant = comparableConstantPool[newIndex];

            // Fill out the map array.
            int oldIndex = comparableConstant.getIndex();
            constantIndexMap[oldIndex] = newIndex;

            // Copy the sorted constant pool entry over to the constant pool.
            // Long entries take up two entries, the second of which is null.
            Constant constant = comparableConstant.getConstant();
            constantPool[newIndex] = constant != previousConstant ?
                constant :
                null;

            previousConstant = constant;
        }
    }
}
