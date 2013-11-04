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
package proguard.classfile.attribute;

import proguard.classfile.VisitorAccepter;

/**
 * Representation of an Inner Classes table entry.
 *
 * @author Eric Lafortune
 */
public class InnerClassesInfo implements VisitorAccepter
{
    public int u2innerClassIndex;
    public int u2outerClassIndex;
    public int u2innerNameIndex;
    public int u2innerClassAccessFlags;

    /**
     * An extra field in which visitors can store information.
     */
    public Object visitorInfo;


    /**
     * Returns the inner class index.
     */
    protected int getInnerClassIndex()
    {
        return u2innerClassIndex;
    }

    /**
     * Returns the name index.
     */
    protected int getInnerNameIndex()
    {
        return u2innerNameIndex;
    }

    /**
     * Sets the name index.
     */
    protected void setInnerNameIndex(int index)
    {
        u2innerNameIndex = index;
    }


    // Implementations for VisitorAccepter.

    public Object getVisitorInfo()
    {
        return visitorInfo;
    }

    public void setVisitorInfo(Object visitorInfo)
    {
        this.visitorInfo = visitorInfo;
    }
}
