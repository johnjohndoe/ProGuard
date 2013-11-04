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

/**
 * This class can add class members to classes. Class members to be added must
 * be filled out beforehand, including their references to the constant pool.
 *
 * @author Eric Lafortune
 */
public class MembersEditor
{
    private static final boolean DEBUG = false;


    /**
     * Adds a given field to the given class.
     */
    public void addField(ProgramClass programClass,
                         Field        field)
    {
        int     fieldsCount = programClass.u2fieldsCount;
        Field[] fields      = programClass.fields;

        // Make sure there is enough space for the new field.
        if (fields.length <= fieldsCount)
        {
            programClass.fields = new ProgramField[fieldsCount+1];
            System.arraycopy(fields, 0,
                             programClass.fields, 0,
                             fieldsCount);
            fields = programClass.fields;
        }

        if (DEBUG)
        {
            System.out.println(programClass.getName()+": adding ["+field.getName(programClass)+field.getDescriptor(programClass)+"]");
        }

        // Add the field.
        fields[programClass.u2fieldsCount++] = field;
    }


    /**
     * Adds a given method to the given class.
     */
    public void addMethod(ProgramClass programClass,
                          Method       method)
    {
        int      methodsCount = programClass.u2methodsCount;
        Method[] methods      = programClass.methods;

        // Make sure there is enough space for the new method.
        if (methods.length <= methodsCount)
        {
            programClass.methods = new ProgramMethod[methodsCount+1];
            System.arraycopy(methods, 0,
                             programClass.methods, 0,
                             methodsCount);
            methods = programClass.methods;
        }

        if (DEBUG)
        {
            System.out.println(programClass.getName()+": adding ["+method.getName(programClass)+method.getDescriptor(programClass)+"]");
        }

        // Add the method.
        methods[programClass.u2methodsCount++] = method;
    }
}
