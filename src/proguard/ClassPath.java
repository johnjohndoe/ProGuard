/* $Id: ClassPath.java,v 1.3 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard;

import java.util.*;


/**
 * This class represents a class path, as a list of ClassPathEntry objects.
 *
 * @author Eric Lafortune
 */
public class ClassPath
{
    private List classPathEntries = new ArrayList();


    // Delegates to List

    public void clear()
    {
        classPathEntries.clear();
    }

    public void add(int index, ClassPathEntry classPathEntry)
    {
        classPathEntries.add(index, classPathEntry);
    }

    public boolean add(ClassPathEntry classPathEntry)
    {
        return classPathEntries.add(classPathEntry);
    }

    public boolean addAll(ClassPath classPath)
    {
        return classPathEntries.addAll(classPath.classPathEntries);
    }

    public ClassPathEntry get(int index)
    {
        return (ClassPathEntry)classPathEntries.get(index);
    }

    public ClassPathEntry remove(int index)
    {
        return (ClassPathEntry)classPathEntries.remove(index);
    }

    public boolean isEmpty()
    {
        return classPathEntries.isEmpty();
    }

    public int size()
    {
        return classPathEntries.size();
    }
}
