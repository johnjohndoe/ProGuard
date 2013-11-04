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

import proguard.classfile.attribute.ExceptionsAttribute;

/**
 * This class can add exceptions to exceptions attributes. Exceptions to be
 * added must have been added to the constant pool filled out beforehand.
 *
 * @author Eric Lafortune
 */
public class ExceptionsEditor
{
    /**
     * Adds a given exception to the given exceptions attribute.
     */
  public void addException(ExceptionsAttribute exceptionsAttribute,
                           int                 exceptionIndex)
    {
        int   exceptionIndexTableLength = exceptionsAttribute.u2exceptionIndexTableLength;
        int[] exceptionIndexTable       = exceptionsAttribute.u2exceptionIndexTable;

        // Make sure there is enough space for the new exception.
        if (exceptionIndexTable.length <= exceptionIndexTableLength)
        {
            exceptionsAttribute.u2exceptionIndexTable = new int[exceptionIndexTableLength+1];
            System.arraycopy(exceptionIndexTable, 0,
                             exceptionsAttribute.u2exceptionIndexTable, 0,
                             exceptionIndexTableLength);
            exceptionIndexTable = exceptionsAttribute.u2exceptionIndexTable;
        }

        // Add the exception.
        exceptionIndexTable[exceptionsAttribute.u2exceptionIndexTableLength++] = exceptionIndex;
    }
}
