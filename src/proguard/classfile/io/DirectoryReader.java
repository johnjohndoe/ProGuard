/* $Id: DirectoryReader.java,v 1.3 2003/03/25 20:08:53 eric Exp $
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

import java.io.*;


/**
 * This class can read a given directory, recursively, applying a given
 * DataEntryReader to all files it comes across. The reader can for instance
 * collect the class files, or copy the resource files.
 *
 * @author Eric Lafortune
 */
public class DirectoryReader
{
    private File directory;


    public DirectoryReader(File directory)
    {
        this.directory = directory;
    }


    /**
     * Reads the directory recursively, applying the given DataEntryReader to
     * all files that are encountered.
     */
    public void readFiles(DataEntryReader dataEntryReader)
    throws IOException
    {
        readFiles(directory, dataEntryReader);
    }


    /**
     * Reads the given subdirectory recursively, applying the given DataEntryReader
     * to all files that are encountered.
     */
    private void readFiles(File subdirectory, DataEntryReader dataEntryReader)
    throws IOException
    {
        File[] files = subdirectory.listFiles();

        for (int index = 0; index < files.length; index++)
        {
            File file = files[index];
            if (file.isDirectory())
            {
                // Recurse into the subdirectory.
                readFiles(file, dataEntryReader);
            }
            else
            {
                try
                {
                    // Delegate the actual reading to the data reader.
                    dataEntryReader.readFile(file, directory);
                }
                catch (IOException ex)
                {
                }
            }
        }
    }
}
