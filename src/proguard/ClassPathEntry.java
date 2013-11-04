/* $Id: ClassPathEntry.java,v 1.3 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2004 Eric Lafortune (eric@graphics.cornell.edu)
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


/**
 * This class represents an entry from a class path: a jar or directory,
 * and an optional filter to be applied to its contents.
 *
 * @author Eric Lafortune
 */
public class ClassPathEntry
{
    private String name;
    private String filter;


    /**
     * Creates a new ClassPathEntry with the given jar name.
     */
    public ClassPathEntry(String jarName)
    {
        this.name = jarName;
    }


    /**
     * Creates a new ClassPathEntry with the given jar name and filter.
     */
    public ClassPathEntry(String name,
                          String filter)
    {
        this.name   = name;
        this.filter = filter;
    }


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }


    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }


    public String toString()
    {
        String string = name;

        if (filter != null)
        {
            string +=
                ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD +
                filter +
                ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD;
        }

        return string;
    }
}
