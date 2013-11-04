/* $Id: DataEntryResourceFileFilter.java,v 1.4 2003/12/06 22:15:38 eric Exp $
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
 * This DataEntryReader applies another given DataEntryReader to the ZIP entries
 * and files that it reads. Only resource files are considered; class files are
 * ignored.
 *
 * @author Eric Lafortune
 */
public class DataEntryResourceFileFilter implements DataEntryReader
{
    private DataEntryReader dataEntryReader;


    public DataEntryResourceFileFilter(DataEntryReader dataEntryReader)
    {
        this.dataEntryReader = dataEntryReader;
    }


    // Implementations for DataEntryReader.

    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException
    {
        // Is it a resource file?
        String name = zipEntry.getName();
        if (!zipEntry.isDirectory() &&
            !name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
        {
            dataEntryReader.readZipEntry(zipEntry, inputStream);
        }
    }


    public void readFile(File file,
                         File directory)
    throws IOException
    {
        // Is it a class file?
        String name = file.getName();
        if (!file.isDirectory() &&
            !name.endsWith(ClassConstants.CLASS_FILE_EXTENSION))
        {
            dataEntryReader.readFile(file, directory);
        }
    }
}
