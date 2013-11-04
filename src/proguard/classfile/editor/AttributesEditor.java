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
import proguard.classfile.attribute.*;

/**
 * This class can add and delete attributes to and from classes, fields,
 * methods, and code attributes. Attributes to be added must be filled out
 * beforehand, including their references to the constant pool. Existing
 * attributes of the same type are always replaced.
 *
 * @author Eric Lafortune
 */
public class AttributesEditor
{
    /**
     * Adds the given attribute to the given class.
     */
    public void addAttribute(ProgramClass programClass,
                             Attribute    attribute)
    {
        // Try to replace an existing attribute.
        if (!replaceAttribute(programClass,
                              programClass.u2attributesCount,
                              programClass.attributes,
                              attribute))
        {
            // Otherwise append the attribute.
            programClass.attributes =
                appendAttribute(programClass.u2attributesCount,
                                programClass.attributes,
                                attribute);

            programClass.u2attributesCount++;
        }
    }


    /**
     * Adds the given attribute to the given field.
     */
    public void addAttribute(ProgramClass programClass,
                             ProgramField programField,
                             Attribute    attribute)
    {
        // Try to replace an existing attribute.
        if (!replaceAttribute(programClass,
                              programField.u2attributesCount,
                              programField.attributes,
                              attribute))
        {
            // Otherwise append the attribute.
            programField.attributes =
                appendAttribute(programField.u2attributesCount,
                                programField.attributes,
                                attribute);

            programField.u2attributesCount++;
        }
    }


    /**
     * Adds the given attribute to the given method.
     */
    public void addAttribute(ProgramClass  programClass,
                             ProgramMethod programMethod,
                             Attribute     attribute)
    {
        // Try to replace an existing attribute.
        if (!replaceAttribute(programClass,
                              programMethod.u2attributesCount,
                              programMethod.attributes,
                              attribute))
        {
            // Otherwise append the attribute.
            programMethod.attributes =
                appendAttribute(programMethod.u2attributesCount,
                                programMethod.attributes,
                                attribute);

            programMethod.u2attributesCount++;
        }
    }


    /**
     * Adds the given attribute to the given code attribute.
     */
    public void addAttribute(ProgramClass  programClass,
                             ProgramMethod programMethod,
                             CodeAttribute codeAttribute,
                             Attribute     attribute)
    {
        // Try to replace an existing attribute.
        if (!replaceAttribute(programClass,
                              codeAttribute.u2attributesCount,
                              codeAttribute.attributes,
                              attribute))
        {
            // Otherwise append the attribute.
            codeAttribute.attributes =
                appendAttribute(codeAttribute.u2attributesCount,
                                codeAttribute.attributes,
                                attribute);

            codeAttribute.u2attributesCount++;
        }
    }


    /**
     * Deletes the given attribute from the given class.
     */
    public void deleteAttribute(ProgramClass programClass,
                                String       attributeName)
    {
        programClass.u2attributesCount =
            deleteAttribute(programClass,
                            programClass.u2attributesCount,
                            programClass.attributes,
                            attributeName);
    }


    /**
     * Deletes the given attribute from the given field.
     */
    public void deleteAttribute(ProgramClass programClass,
                                ProgramField programField,
                                String       attributeName)
    {
        programField.u2attributesCount =
            deleteAttribute(programClass,
                            programField.u2attributesCount,
                            programField.attributes,
                            attributeName);
    }


    /**
     * Deletes the given attribute from the given method.
     */
    public void deleteAttribute(ProgramClass  programClass,
                                ProgramMethod programMethod,
                                String        attributeName)
    {
        programMethod.u2attributesCount =
            deleteAttribute(programClass,
                            programMethod.u2attributesCount,
                            programMethod.attributes,
                            attributeName);
    }


    /**
     * Deletes the given attribute from the given code attribute.
     */
    public void deleteAttribute(ProgramClass  programClass,
                                ProgramMethod programMethod,
                                CodeAttribute codeAttribute,
                                String        attributeName)
    {
        codeAttribute.u2attributesCount =
            deleteAttribute(programClass,
                            codeAttribute.u2attributesCount,
                            codeAttribute.attributes,
                            attributeName);
    }


    // Small utility methods.

    /**
     * Tries put the given attribute in place of an existing attribute of
     * the same type.
     */
    private boolean replaceAttribute(Clazz       clazz,
                                     int         attributesCount,
                                     Attribute[] attributes,
                                     Attribute   attribute)
    {
        String attributeName = attribute.getAttributeName(clazz);

        for (int index = 0; index < attributesCount; index++)
        {
            if (attributes[index].getAttributeName(clazz).equals(attributeName))
            {
                attributes[index] = attribute;
                return true;
            }
        }

        return false;
    }


    /**
     * Appends the given attribute to the given array of attributes, creating
     * a new array if necessary.
     */
    private Attribute[] appendAttribute(int         attributesCount,
                                        Attribute[] attributes,
                                        Attribute   attribute)
    {
        // Is the array too small to contain the additional attribute?
        if (attributes.length <= attributesCount)
        {
            // Create a new array and copy the attributes into it.
            Attribute[] newAttributes = new Attribute[attributesCount + 1];
            System.arraycopy(attributes, 0, newAttributes, 0, attributesCount);
            attributes = newAttributes;
        }

        // Append the attribute.
        attributes[attributesCount] = attribute;

        return attributes;
    }


    /**
     * Deletes attributes with the given name, and returns the new number of
     * attributes.
     */
    private int deleteAttribute(Clazz       clazz,
                                int         attributesCount,
                                Attribute[] attributes,
                                String      attributeName)
    {
        int newIndex = 0;

        // Shift the other attributes in the array.
        for (int index = 0; index < attributesCount; index++)
        {
            if (!attributes[index].getAttributeName(clazz).equals(attributeName))
            {
                attributes[newIndex++] = attributes[index];
            }
        }

        // Clear the remaining entries in the array.
        for (int index = newIndex; index < attributesCount; index++)
        {
            attributes[index] = null;
        }

        return newIndex;
    }
}
