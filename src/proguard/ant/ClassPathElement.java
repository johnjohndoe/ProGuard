/* $Id: ClassPathElement.java,v 1.3 2004/09/04 16:30:12 eric Exp $
 *
 * ProGuard -- shrinking, optimization, and obfuscation of Java class files.
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
package proguard.ant;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import proguard.*;

import java.io.*;

/**
 * This FileSet represents a class path entry (or a set of class path entries)
 * in Ant.
 *
 * @author Eric Lafortune
 */
public class ClassPathElement extends FileSet
{
    private File   file;

    private String filter;
    private String jarFilter;
    private String warFilter;
    private String earFilter;
    private String zipFilter;


    /**
     * Adds the contents of this class path element to the given class path.
     * @param classPath the class path to be extended.
     * @param output    specifies whether this is an output entry or not.
     */
    public void appendClassPathEntriesTo(ClassPath classPath, boolean output)
    {
        // Get the referenced file set, or else this one.
        AbstractFileSet fileSet = isReference() ? getRef(getProject()) : this;

        if (output)
        {
            // Create a new class path entry, with the proper file name and any
            // filters, bypassing the file check.
            String name = file != null ?
                file.getPath() :
                fileSet.getDir(getProject()).getPath();

            ClassPathEntry entry = new ClassPathEntry(name, output);
            entry.setFilter(filter);
            entry.setJarFilter(jarFilter);
            entry.setWarFilter(warFilter);
            entry.setEarFilter(earFilter);
            entry.setZipFilter(zipFilter);

            // Add it to the class path.
            classPath.add(entry);
        }
        else
        {
            // Get the names of the existing files in the file set.
            DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
            String[]         files   = scanner.getIncludedFiles();

            for (int index = 0; index < files.length; index++)
            {
                // Create a new class path entry, with the proper file name and
                // any filters.
                String name = scanner.getBasedir().getPath() +
                    File.separator +
                    files[index];

                ClassPathEntry entry = new ClassPathEntry(name, output);
                entry.setFilter(filter);
                entry.setJarFilter(jarFilter);
                entry.setWarFilter(warFilter);
                entry.setEarFilter(earFilter);
                entry.setZipFilter(zipFilter);

                // Add it to the class path.
                classPath.add(entry);
            }
        }
    }


    // Ant task attributes.

    public void setFile(File file)
    {
        super.setFile(file);

        this.file = file;
    }


    /**
     * @deprecated Use {@link #setFile(File)} instead.
     */
    public void setName(String name)
    {
        setFile(new File(name));
    }


    public void setFilter(String filter)
    {
        this.filter = filter;
    }


    public void setJarFilter(String jarFilter)
    {
        this.jarFilter = jarFilter;
    }


    public void setWarFilter(String warFilter)
    {
        this.warFilter = warFilter;
    }


    public void setEarFilter(String earFilter)
    {
        this.earFilter = earFilter;
    }


    public void setZipFilter(String zipFilter)
    {
        this.zipFilter = zipFilter;
    }
}
