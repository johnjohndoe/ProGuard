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
package proguard.optimize.evaluation;

import proguard.classfile.*;
import proguard.classfile.constant.RefConstant;
import proguard.evaluation.BasicInvocationUnit;
import proguard.evaluation.value.*;

/**
 * This InvocationUbit loads parameter values and return values that were
 * previously stored with the methods that are invoked.
 *
 * @see StoringInvocationUnit
 * @author Eric Lafortune
 */
public class LoadingInvocationUnit
extends      BasicInvocationUnit
{
    // Implementations for BasicInvocationUnit.

    protected Value getFieldClassValue(Clazz       clazz,
                                       RefConstant refConstant,
                                       String      type)
    {
        Member referencedMember = refConstant.referencedMember;
        if (referencedMember != null)
        {
            ReferenceValue value = StoringInvocationUnit.getFieldClassValue((Field)referencedMember);
            if (value != null)
            {
                return value;
            }
        }

        return super.getFieldClassValue(clazz, refConstant, type);
    }


    protected Value getFieldValue(Clazz       clazz,
                                  RefConstant refConstant,
                                  String      type)
    {
        Member referencedMember = refConstant.referencedMember;
        if (referencedMember != null)
        {
            Value value = StoringInvocationUnit.getFieldValue((Field)referencedMember);
            if (value != null)
            {
                return value;
            }
        }

        return super.getFieldValue(clazz, refConstant, type);
    }


    protected Value getMethodParameterValue(Clazz  clazz,
                                            Method method,
                                            int    parameterIndex,
                                            String type,
                                            Clazz  referencedClass)
    {
        Value value = StoringInvocationUnit.getMethodParameterValue(method, parameterIndex);
        if (value != null)
        {
            return value;
        }

        return super.getMethodParameterValue(clazz,
                                             method,
                                             parameterIndex,
                                             type,
                                             referencedClass);
    }


    protected Value getMethodReturnValue(Clazz       clazz,
                                         RefConstant refConstant,
                                         String      type)
    {
        Member referencedMember = refConstant.referencedMember;
        if (referencedMember != null)
        {
            Value value = StoringInvocationUnit.getMethodReturnValue((Method)referencedMember);
            if (value != null)
            {
                return value;
            }
        }

        return super.getMethodReturnValue(clazz,
                                          refConstant,
                                          type);
    }
}
