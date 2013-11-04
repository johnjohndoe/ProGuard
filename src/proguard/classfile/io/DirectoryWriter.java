/* $Id: DirectoryWriter.java,v 1.1 2003/03/25 20:08:53 eric Exp $
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
package proguard.classfile.io;

import proguard.classfile.*;

import java.io.*;


/**
 * This DataEntryWriter writes sends data entries to individual files in a
 * given directory.
 *
 * @author Eric Lafortune
 */
public class DirectoryWriter implements DataEntryWriter
{
    private File         baseDirectory;
    private OutputStream currentOutputStream;


    /**
     * Creates a new DirectoryWriter.
     * @param baseDirectory the base directory to which all files will be written.
     */
    public DirectoryWriter(File baseDirectory)
    {
        this.baseDirectory = baseDirectory;
    }


    // Implementations for DataEntryWriter

    public void close() throws IOException
    {
    }


    public OutputStream openDataEntry(String name) throws IOException
    {
        // Make sure the separator is the file system's separator.
        name = name.replace(ClassConstants.INTERNAL_PACKAGE_SEPARATOR,
                            File.separatorChar);

        // Make sure the parent directories exist.
        File file = new File(baseDirectory, name);
        File parentDirectory = file.getParentFile();
        if (!parentDirectory.exists() && !parentDirectory.mkdirs())
        {
            throw new IOException("Can't create directory [" + parentDirectory.getPath() + "]");
        }

        // Open a stream for writing to the file.
        currentOutputStream =
            new BufferedOutputStream(
            new FileOutputStream(file));

        return currentOutputStream;
    }


    public void closeDataEntry() throws IOException
    {
        currentOutputStream.close();

        currentOutputStream = null;
    }
}
