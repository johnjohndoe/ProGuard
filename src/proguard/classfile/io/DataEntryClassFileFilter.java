/* $Id: DataEntryClassFileFilter.java,v 1.2 2003/02/09 15:22:28 eric Exp $
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
import java.util.zip.*;


/**
 * This DataEntryReader applies a given ClassFileReader to the class files
 * that it reads. Other entries are ignored.
 *
 * @author Eric Lafortune
 */
public class DataEntryClassFileFilter implements DataEntryReader
{
    private ClassFileReader classFileReader;


    /**
     * Creates a new DataEntryClassFileFilter for reading ClassFile objects.
     */
    public DataEntryClassFileFilter(ClassFileReader classFileReader)
    {
        this.classFileReader = classFileReader;
    }


    // Implementations for DataEntryReader

    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException
    {
        // Is it a class file?
        String name = zipEntry.getName();
        if (!zipEntry.isDirectory() &&
            name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
        {
            classFileReader.readData(name, inputStream);
        }
    }


    public void readFile(File file,
                         File directory)
    throws IOException
    {
        // Is it a class file?
        String name = file.getPath();
        if (!file.isDirectory() &&
            name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
        {
            // Chop the directory name from the file name and get the right
            // separators.
            name = name
                .substring(directory.getPath().length() + File.separator.length())
                .replace(File.separatorChar, ClassConstants.INTERNAL_PACKAGE_SEPARATOR);

            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            try
            {
                classFileReader.readData(name, inputStream);
            }
            finally
            {
                inputStream.close();
            }
        }
    }
}
