/* $Id: ClassPathIterator.java,v 1.3 2003/12/19 04:17:03 eric Exp $
 *
 * ProGuard - integration into Ant.
 *
 * Copyright (c) 2003 Dirk Schnelle (dirk.schnelle@web.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHAntABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.ant;

import java.util.*;

import proguard.*;


/**
 * Iterator over a ClassPath.
 *
 * @author Dirk Schnelle
 * @author Eric Lafortune
 *
 * @see proguard.ClassPath
 */
public class ClassPathIterator
        implements Iterator
{
    /** The class path over which we ar iterating. */
    private final ClassPath classPath;

    /** Current class path entry. */
    private int index = 0;

    /**
     * Create a new ClassPathIterator.
     *
     * @param classPath the ClassPath over which we are iterationg.
     */
    public ClassPathIterator(ClassPath classPath)
    {
        this.classPath = classPath;
    }

    /**
     * Returns the next class path entry in the iteration.
     *
     * @return The next class path entry in the iteration.
     *
     * @exception NoSuchElementException iteration has no more elements
     */
    public ClassPathEntry nextClassPathEntry()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        ClassPathEntry entry = classPath.get(index);
        index++;

        return entry;
    }

    /**
     * Check ifthere isa next element.
     *
     * @return <code>true</code> if there is a next element.
     */
    public boolean hasNext()
    {
        return (classPath != null) && (index < classPath.size());
    }

    /**
     * Get the next element.
     *
     * @return Next element
     */
    public Object next()
    {
        return nextClassPathEntry();
    }

    /**
     * Remove the current element.
     */
    public void remove()
    {
        --index;
        classPath.remove(index);
    }
}
