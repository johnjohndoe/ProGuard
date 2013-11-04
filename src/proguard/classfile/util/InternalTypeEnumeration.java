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
package proguard.classfile.util;

import proguard.classfile.ClassConstants;


/**
 * An <code>InternalTypeEnumeration</code> provides an enumeration of all
 * parameter types listed in a given internal method descriptor or signature.
 * The signature can also be a class signature. The return type of a method
 * descriptor can retrieved separately.
 *
 * @author Eric Lafortune
 */
public class InternalTypeEnumeration
{
    private String descriptor;
    private int    index;


    /**
     * Creates a new InternalTypeEnumeration for the given method descriptor.
     */
    public InternalTypeEnumeration(String descriptor)
    {
        this.descriptor = descriptor;
        this.index      = descriptor.indexOf(ClassConstants.INTERNAL_METHOD_ARGUMENTS_OPEN) + 1;
    }


    /**
     * Returns whether the enumeration can provide more types from the method
     * descriptor.
     */
    public boolean hasMoreTypes()
    {
        return index < descriptor.length() &&
               descriptor.charAt(index) != ClassConstants.INTERNAL_METHOD_ARGUMENTS_CLOSE;
    }


    /**
     * Returns the next type from the method descriptor.
     */
    public String nextType()
    {
        int startIndex = index;

        int     nestingLevel   = 0;
        boolean parsingRawType = true;
        boolean parsingArrayPrefix;
        do
        {
            parsingArrayPrefix = false;

            char c = descriptor.charAt(index++);

            if (parsingRawType)
            {
                // Parse an array character, primitive type, or a token
                // marking the beginning of an identifier (for a class or
                // a variable type).
                switch (c)
                {
                    case ClassConstants.INTERNAL_TYPE_GENERIC_START:
                    {
                        parsingRawType = false;
                        nestingLevel++;
                        break;
                    }
                    case ClassConstants.INTERNAL_TYPE_GENERIC_END:
                    {
                        parsingRawType = false;
                        nestingLevel--;
                        break;
                    }
                    case ClassConstants.INTERNAL_TYPE_ARRAY:
                    {
                        parsingArrayPrefix = true;
                        break;
                    }
                    case ClassConstants.INTERNAL_TYPE_CLASS_START:
                    case ClassConstants.INTERNAL_TYPE_GENERIC_VARIABLE_START:
                    {
                        parsingRawType = false;
                        nestingLevel += 2;
                        break;
                    }
                }
            }
            else
            {
                // Parse the identifier, or a token marking its end.
                switch (c)
                {
                    case ClassConstants.INTERNAL_TYPE_CLASS_END:
                        parsingRawType = true;
                        nestingLevel -= 2;

                        // Are we at the start of a type parameter?
                        if (nestingLevel == 1 &&
                            descriptor.charAt(index) != ClassConstants.INTERNAL_TYPE_GENERIC_END)
                        {
                            parsingRawType = false;
                        }
                        break;
                    case ClassConstants.INTERNAL_TYPE_GENERIC_START:
                        parsingRawType = true;
                        nestingLevel++;
                        break;
                    case ClassConstants.INTERNAL_TYPE_GENERIC_BOUND:
                        parsingRawType = true;
                        break;
                }
            }
        }
        while (nestingLevel > 0 || parsingArrayPrefix);

        return descriptor.substring(startIndex, index);
    }


    /**
     * Returns the return type from the descriptor, assuming it's a method
     * descriptor.
     */
    public String returnType()
    {
        return descriptor.substring(descriptor.indexOf(ClassConstants.INTERNAL_METHOD_ARGUMENTS_CLOSE) + 1);
    }


    /**
     * A main method for testing the type enumeration.
     */
    public static void main(String[] args)
    {
        try
        {
            System.out.println("Descriptor ["+args[0]+"]");
            InternalTypeEnumeration enumeration = new InternalTypeEnumeration(args[0]);

            while (enumeration.hasMoreTypes())
            {
                System.out.println("  Type ["+enumeration.nextType()+"]");
            }

            System.out.println("  Return type ["+enumeration.returnType()+"]");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
