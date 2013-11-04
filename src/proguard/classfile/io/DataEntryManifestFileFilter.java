/* $Id: DataEntryManifestFileFilter.java,v 1.3 2003/12/06 22:15:38 eric Exp $
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
package proguard.classfile.io;

import java.io.*;
import java.util.jar.*;
import java.util.zip.*;


/**
 * This DataEntryReader applies another given DataEntryReader to the ZIP entries
 * and files that it reads. Only resource files are considered; class files are
 * ignored.
 *
 * @author Eric Lafortune
 */
public class DataEntryManifestFileFilter implements DataEntryReader
{
    private DataEntryReader dataEntryReader;
    private Manifest        manifest;


    public DataEntryManifestFileFilter(DataEntryReader dataEntryReader)
    {
        this.dataEntryReader = dataEntryReader;
    }


    /**
     * Returns the most recently read Manifest.
     */
    public Manifest getManifest()
    {
        return manifest;
    }


    // Implementations for DataEntryReader.

    public void readZipEntry(ZipEntry    zipEntry,
                             InputStream inputStream)
    throws IOException
    {
        // Is it a manifest file?
        String name = zipEntry.getName();
        if (!zipEntry.isDirectory() &&
            name.equalsIgnoreCase(JarFile.MANIFEST_NAME))
        {
            // Read the manifest.
            manifest = new Manifest(inputStream);
        }
        else
        {
            // Delegate to the data entry reader.
            dataEntryReader.readZipEntry(zipEntry, inputStream);
        }
    }


    public void readFile(File file,
                         File directory)
    throws IOException
    {
        // Is it a manifest file? First chop the directory name from the file name.
        String name = file.getPath()
            .substring(directory.getPath().length() + File.separator.length());
        if (!file.isDirectory() &&
            name.equalsIgnoreCase(JarFile.MANIFEST_NAME))
        {
            // Open the file input stream.
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

            try
            {
                // Read the manifest.
                manifest = new Manifest(inputStream);
            }
            finally
            {
                // Close the file input stream.
                inputStream.close();
            }
        }
        else
        {
            // Delegate to the data entry reader.
            dataEntryReader.readFile(file, directory);
        }
    }
}
