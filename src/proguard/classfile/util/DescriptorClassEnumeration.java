/* $Id: DescriptorClassEnumeration.java,v 1.3 2002/05/12 13:33:41 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
 * A <code>DescriptorClassEnumeration</code> provides an enumeration of all
 * classes mentioned in a given descriptor string.
 * <p>
 * A <code>DescriptorClassEnumeration</code> object can be reused for processing
 * different subsequent descriptors, by means of the <code>setDescriptor</code>
 * method.
 *
 * @author Eric Lafortune
 */
public class DescriptorClassEnumeration
{
    private String descriptor;
    private int    index;


    public DescriptorClassEnumeration()
    {
    }


    public DescriptorClassEnumeration(String descriptor)
    {
        setDescriptor(descriptor);
    }


    public void setDescriptor(String descriptor)
    {
        this.descriptor = descriptor;
        this.index      = 0;
    }


    /**
     * Returns the number of classes contained in the descriptor. This
     * is the number of class names that the enumeration will return.
     */
    public int classCount()
    {
        int count = 0;

        for (int i = 0; i < descriptor.length(); i++)
        {
            if (descriptor.charAt(i) == ClassConstants.INTERNAL_TYPE_CLASS_END)
            {
                count++;
            }
        }

        return count;
    }


    public boolean hasMoreClassNames()
    {
        return descriptor.indexOf(ClassConstants.INTERNAL_TYPE_CLASS_START, index) >= 0;
    }


    public String nextFluff()
    {
        int startIndex = index;
        index = descriptor.indexOf(ClassConstants.INTERNAL_TYPE_CLASS_START, index);
        int endIndex   = index + 1;
        if (index < 0)
        {
            index    = descriptor.length();
            endIndex = index;
        }

        return descriptor.substring(startIndex, endIndex);
    }


    public String nextClassName()
    {
        int startIndex = descriptor.indexOf(ClassConstants.INTERNAL_TYPE_CLASS_START, index) + 1;
        index = descriptor.indexOf(ClassConstants.INTERNAL_TYPE_CLASS_END, startIndex);
        return descriptor.substring(startIndex, index);
    }
}
