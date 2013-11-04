/* $Id: JarReader.java,v 1.9 2002/11/03 13:30:14 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (C) 2002 Eric Lafortune (eric@graphics.cornell.edu)
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
package proguard.classfile.visitor;

import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;


/**
 * This class can read a given jar, applying a given ZipEntryReader to all
 * ZIP entries it reads. The reader can for instance collect the class files,
 * or copy the resource files.
 *
 * @author Eric Lafortune
 */
public class JarReader
{
    private String   jarFileName;
    private Manifest manifest;


    public JarReader(String jarFileName)
    {
        this.jarFileName = jarFileName;
    }


    /**
     * Returns the Manifest from the most recently read jar file.
     */
    public Manifest getManifest()
    {
        return manifest;
    }


    /**
     * Reads the given jar, applying the given ZipEntryReader to all ZIP entries
     * that are encountered.
     */
    public void readZipEntries(ZipEntryReader zipEntryReader)
    throws IOException
    {
        JarInputStream jarInputStream = null;

        try
        {
            jarInputStream = new JarInputStream(
                             new BufferedInputStream(
                             new FileInputStream(
                             new File(jarFileName))));

            // Get all entries from the input jar.
            while (true)
            {
                // Can we get another entry?
                ZipEntry zipEntry = jarInputStream.getNextEntry();
                if (zipEntry == null)
                {
                    break;
                }

                // Delegate the actual reading to the ZIP entry reader.
                zipEntryReader.readZipEntry(zipEntry, jarInputStream);

                // Close the entry, so we can continue with the next one.
                jarInputStream.closeEntry();
            }

            manifest = jarInputStream.getManifest();
        }
        finally
        {
            if (jarInputStream != null)
            {
                try
                {
                    jarInputStream.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
    }
}
