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
package proguard.obfuscate;

import proguard.classfile.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.*;

/**
 * This <code>ConstantVisitor</code> makes package visible classes and class
 * members public, if they are referenced by visited references from different
 * packages.
 *
 * @author Eric Lafortune
 */
public class ClassOpener
extends      SimplifiedVisitor
implements   ConstantVisitor
{
    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        fixPackageVisibility(clazz,
                             stringConstant.referencedClass,
                             stringConstant.referencedMember);
    }


    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        fixPackageVisibility(clazz,
                             refConstant.referencedClass,
                             refConstant.referencedMember);
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Do we know the referenced class?
        Clazz referencedClass = classConstant.referencedClass;
        if (referencedClass != null)
        {
            int accessFlags = referencedClass.getAccessFlags();

            // Make it public if necessary.
            if (isNotPublic(accessFlags)                 &&
                referencedClass instanceof ProgramClass  &&
                inDifferentPackages(clazz, referencedClass))
            {
                ((ProgramClass)referencedClass).u2accessFlags =
                    AccessUtil.replaceAccessFlags(accessFlags,
                                                  ClassConstants.INTERNAL_ACC_PUBLIC);
            }
        }
    }


    // Small utility methods.

    /**
     * Fixes the package visibility of the given referenced class member,
     * if necessary.
     */
    private void fixPackageVisibility(Clazz clazz, Clazz referencedClass, Member referencedMember)
    {
        // Do we know the referenced class member?
        if (referencedMember != null)
        {
            int accessFlags = referencedMember.getAccessFlags();

            // Make it public if necessary.
            if (isNotPublic(accessFlags)                  &&
                referencedMember instanceof ProgramMember &&
                inDifferentPackages(clazz, referencedClass))
            {
                ((ProgramMember)referencedMember).u2accessFlags =
                    AccessUtil.replaceAccessFlags(accessFlags,
                                                  clazz.extends_(referencedClass) ?
                                                  ClassConstants.INTERNAL_ACC_PROTECTED :
                                                  ClassConstants.INTERNAL_ACC_PUBLIC);
            }
        }
    }


    /**
     * Returns whether the given classes are in different packages..
     */
    private boolean inDifferentPackages(Clazz class1, Clazz class2)
    {
        return !ClassUtil.internalPackageName(class1.getName()).equals(
                ClassUtil.internalPackageName(class2.getName()));
    }


    /**
     * Returns whether the given access flags specify a non-public class
     * or class member.
     */
    private boolean isNotPublic(int accessFlags)
    {
        return (accessFlags & ClassConstants.INTERNAL_ACC_PUBLIC) == 0;
    }
}
